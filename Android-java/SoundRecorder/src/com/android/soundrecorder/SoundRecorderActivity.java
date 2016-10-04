
package com.android.soundrecorder;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.android.soundrecorder.file.FileManager;
import com.android.soundrecorder.util.MiscUtil;
import com.android.soundrecorder.wav.AudioPlayWav;
import com.android.soundrecorder.wav.RecorderWav;


public class SoundRecorderActivity extends Activity implements Button.OnClickListener
		{
	public static final String TAG = "SoundRecorderActivity";

	WakeLock mWakeLock;
	long mMaxFileSize = -1; // can be specified in the intent
	String mTimerFormat;
	
	final Handler mLoopHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			uiLoopRender(true);
		}
	};

	Button mRecord = null;
	Button mPlayPause = null;
	Button mPlayStop = null;
	Button mPlayNext = null;
	Button mPlayPrev = null;
	ImageView mStateLED = null;
	TextView mTimerView = null;
	EditText mPasswdText = null;
	
	Resources res = null;
	private RecorderWav mRecorderWav = null;
	private AudioPlayWav mAudioPlayWav = null;
	private TextView mStatusText = null;
	private SeekBar mplaySeekBar = null;
	
	@Override
	public void onCreate(Bundle icycle) {
		FileManager.getInstance();
		super.onCreate(icycle);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.main);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundRecorder");
		res = getResources();
		initResourceRefs();
		if (!misInUiloopRender) {
			mLoopHandler.postDelayed(mUpdateTimer, 500);
		}
	}

	private void initResourceRefs() {
		mRecord = (Button) findViewById(R.id.recordButton);
		
		mStateLED = (ImageView) findViewById(R.id.stateLED);
		mTimerView = (TextView) findViewById(R.id.timerView);
		mPasswdText = (EditText) findViewById(R.id.passwdEdit);
		mStatusText = (TextView)findViewById(R.id.statustext);
		mplaySeekBar = (SeekBar)findViewById(R.id.playseekbar);
		
		mPlayNext = (Button)findViewById(R.id.next);
		mPlayPrev = (Button)findViewById(R.id.prev);
		mPlayPause = (Button)findViewById(R.id.playpause);
		mPlayStop = (Button)findViewById(R.id.stop);
		
		mStatusText.setClickable(false);
		mplaySeekBar.setClickable(false);
		mRecord.setOnClickListener(this);
		mPlayNext.setOnClickListener(this);
		mPlayPause.setOnClickListener(this);
		mPlayStop.setOnClickListener(this);
		mPlayPrev.setOnClickListener(this);
		
		mTimerFormat = getResources().getString(R.string.timer_format);
		mplaySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}		
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				if (mAudioPlayWav != null && arg2){
					if (mAudioPlayWav.getState() == AudioPlayWav.PLAY_PAUSE_STATE || 
							mAudioPlayWav.getState() == AudioPlayWav.PLAY_STARTED){
						mAudioPlayWav.seekTo(arg1);
					}
				}
			}
		});
	}
	
	private void stopRecordForSafe(){
		if (mRecorderWav != null){
			mRecorderWav.stopRecording();
		}
		mRecorderWav = null;
	}
	
	private void stopPlayForSafe(){
		if(mAudioPlayWav != null){
			mAudioPlayWav.stop();
		}
		mAudioPlayWav = null;
	}
	
	public void onClick(View button) {
		switch (button.getId()) {
		case R.id.recordButton:
			if (mPasswdText.getEditableText().length() == 0){
				Toast.makeText(this, "input the passwd to encryption",
						Toast.LENGTH_LONG).show();
				return;
			}
			stopRecordForSafe();
			stopPlayForSafe();
			
			mRecorderWav = new RecorderWav(this, mUiHandler, mPasswdText.getEditableText().toString());
			mRecorderWav.startRecording();
			break;			
		case R.id.playpause:
			//in recorder status . play. continue function
			if (mRecorderWav != null){
				mRecorderWav.pauseRecording();
				break;
			}
			
			stopRecordForSafe();
			//is idle status . start a play.
			if (mAudioPlayWav == null) {
				mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
						mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getNewestFile().getFilePath()));
			}else { // in play status alread. play plau.
				if (mAudioPlayWav.getState() == AudioPlayWav.PLAY_STARTED){
					mAudioPlayWav.pause();
				}else if(mAudioPlayWav.getState() == AudioPlayWav.PLAY_PAUSE_STATE){
					mAudioPlayWav.resume();
				}
			}
			break;
		case R.id.stop: 
			stopRecordForSafe();
			stopPlayForSafe();
			break;
		case R.id.prev:
			stopRecordForSafe();
			stopPlayForSafe();
			mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
					mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getPreFile().getFilePath()));
			break;
		case R.id.next:
			stopRecordForSafe();
			stopPlayForSafe();
			mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
					mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getNextFile().getFilePath()));
			break;
		}
	}

	@Override
	public void onBackPressed() {
		stopRecordForSafe();
		stopPlayForSafe();
		mWakeLock.release();
		super.onBackPressed();
	}
	
	@Override
	public void onDestroy() {
		stopRecordForSafe();
		stopPlayForSafe();
		super.onDestroy();
	}
	
	private long mTimeViewTime = 0;
	private void updateTimerRest() {
		String timeStr = String.format(mTimerFormat, 0, 0, 0);
		mTimerView.setText(timeStr);
		mStateLED.setVisibility(View.INVISIBLE);
		mplaySeekBar.setProgress(0);
	}
	private boolean misInUiloopRender = false;
	private void uiLoopRender(boolean loop) {
		misInUiloopRender = true;
		if (mRecorderWav != null && mState == STATE_RECODE_STARTED){
			mTimeViewTime = mRecorderWav.getRecodTimeInSec();
			int[] hms = MiscUtil.sec2hms(mTimeViewTime);
			String timeStr = String
					.format(mTimerFormat, hms[0], hms[1], hms[2]);
			mTimerView.setText(timeStr);

			if (mTimeViewTime % 2 == 1) {
				mStateLED.setVisibility(View.INVISIBLE);
			} else {
				mStateLED.setVisibility(View.VISIBLE);
			}
			mStatusText.setText("STATUS: Recod: " + FileName);
		}
		if (mAudioPlayWav != null && mState == STATE_PLAY_STARTED){
			mTimeViewTime = mAudioPlayWav.getPlayTimeInSec();
			int[] hms = MiscUtil.sec2hms(mTimeViewTime);

			String timeStr = String
					.format(mTimerFormat, hms[0], hms[1], hms[2]);
			mTimerView.setText(timeStr);
			mStatusText.setText("STATUS: Play: " + FileName);
			mplaySeekBar.setMax(mPlayFileLenInSec);
			mplaySeekBar.setProgress((int)mTimeViewTime);
		}
		
		if (mState == STATE_IDLE){
			updateTimerRest();
			mStatusText.setText("STATUS: IDLE");
		}
		
		if (loop){ 
			mLoopHandler.postDelayed(mUpdateTimer, 400);
		}
	}
	
	public static final int UI_HANDLER_TEST = -1;
	public static final int STATE_IDLE = 0;
	public static final int UI_HANDLER_UPDATE_UP = 0X01;
	public static final int SAVE_FILE_SUCCESS = 0x02;
	public static final int FILE_REACH_SIZE = 0X3;
	public static final int UI_HANDLER_UPDATE_TIMERVIEW = 0X04;
	public static final int UI_HANDLER_UPDATE_TIME_RESET = 0X05;
	public static final int STATE_RECODE_STARTED = 0X06;
	public static final int STATE_RECODE_END = 0x07;
	public static final int STATE_PLAY_STARTED = 0x08;
	public static final int STATE_PLAY_END = 0X09;
	public static final int STATE_RECODE_ERR = 0x10;
	
	private int mState = STATE_IDLE;
	private String FileName = "";
	private int mPlayFileLenInSec = 0;
	private  Handler mUiHandler = new  Handler(){
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, ".uihandler ..get." + msg.what);
			switch (msg.what) {
			case UI_HANDLER_TEST:
				Log.i(TAG, "..get a UI_HANDLER_TEST");
				break;
			case SAVE_FILE_SUCCESS:
				Toast.makeText(SoundRecorderActivity.this, 
						"save file success", Toast.LENGTH_LONG).show();
			     break;
			case FILE_REACH_SIZE:
				mRecord.callOnClick();
				break;
			case STATE_RECODE_STARTED:
				mState = STATE_RECODE_STARTED;
				FileName = msg.obj.toString();
				uiLoopRender(false);
				break;
			case STATE_RECODE_END:
				mState = STATE_IDLE;
				if (mRecorderWav != null){
					if (mRecorderWav.getState() == RecorderWav.IDLE_STATE){
						mRecorderWav = null;
					}
				}
				uiLoopRender(false);
				break;
			case STATE_RECODE_ERR:
				break;
			case STATE_PLAY_STARTED:
				mState = STATE_PLAY_STARTED;
				mPlayFileLenInSec = msg.arg1;
				FileName = msg.obj.toString();
				uiLoopRender(false);
				break;
			case STATE_PLAY_END:
				mState = STATE_IDLE;
				if (mAudioPlayWav != null){
					if (mAudioPlayWav.getState() == AudioPlayWav.PLAY_END){
						mAudioPlayWav = null;
					}
				}
				//mAudioPlayWav = null;
				uiLoopRender(false);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
	
}
