
package com.android.soundrecorder;

import java.io.File;

import com.android.soundrecorder.file.DiskSpaceRecyle;
import com.android.soundrecorder.file.FileManager;
import com.android.soundrecorder.util.MiscUtil;
import com.android.soundrecorder.wav.AudioPlayWav;
import com.android.soundrecorder.wav.RecorderWav;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class SoundRecorderActivity extends Activity implements Button.OnClickListener
		{
	static final String TAG = "SoundRecorderActivity";

	WakeLock mWakeLock;

	String mErrorUiMessage = null; // Some error messages are displayed in the

	long mMaxFileSize = -1; // can be specified in the intent

	String mTimerFormat;
	
	final Handler mLoopHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			uiLoopRender(true);
		}
	};

	ImageButton mRecordButton;
	ImageButton mPauseButton;
	
	Button mPlayPause;
	Button mPlayStop;
	Button mPlayNext;
	Button mPlayPrev;
	
	ImageView mStateLED;
	TextView mTimerView;
	EditText mPasswdText = null;
	
	Resources res = null;
	private RecorderWav mRecorderWav = null;
	private AudioPlayWav mAudioPlayWav = null;
	private TextView mStatusText = null;
	private SeekBar mplaySeekBar = null;
	
	@Override
	public void onCreate(Bundle icycle) {
		//DiskSpaceRecyle.getInstance().start();
		FileManager.getInstance();
		super.onCreate(icycle);
		Log.i(TAG, "onCreate");
		setContentView(R.layout.main);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"SoundRecorder");
		res = getResources();
		initResourceRefs();
		if (!misInUiloopRender) {
			mLoopHandler.postDelayed(mUpdateTimer, 500);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

	}
	
	/*
	 * Whenever the UI is re-created (due f.ex. to orientation change) we have
	 * to reinitialize references to the views.
	 */
	private void initResourceRefs() {
		mRecordButton = (ImageButton) findViewById(R.id.recordButton);
		mPauseButton = (ImageButton) findViewById(R.id.pauseButton);
		
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
		mRecordButton.setOnClickListener(this);
		mPauseButton.setOnClickListener(this);
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
				//Log.i(TAG, "onprogressChanged. " + arg1 + ". " + arg2);
				// arg1 is the process value, arg2== is we seecked man
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
		Log.i(TAG, "onClick");
		//mUiHandler.sendEmptyMessage(UI_HANDLER_TEST);
		switch (button.getId()) {
		case R.id.recordButton:
			Log.i(TAG, "recordbutton..click");
			if (mPasswdText.getEditableText().length() == 0){
				Toast.makeText(this, "input the passwd to encryption",
						Toast.LENGTH_LONG).show();
				return;
			}
			stopRecordForSafe();
			stopPlayForSafe();
			
			mRecorderWav = new RecorderWav(this, mUiHandler, mPasswdText.getEditableText().toString());
			mRecorderWav.startRecording();
            mStateLED.setImageResource(R.drawable.recording_led);
			break;
		case R.id.pauseButton:
			Log.i(TAG, "pauseRecording..click");
			if (mRecorderWav != null) {
				mRecorderWav.pauseRecording();
			}
			break;
			
		case R.id.playpause:
			stopRecordForSafe();
			if (mAudioPlayWav == null) {
				mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
						mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getNewestFile().getFilePath()));
			}else {
				if (mAudioPlayWav.getState() == AudioPlayWav.PLAY_STARTED){
					mAudioPlayWav.pause();
				}else if(mAudioPlayWav.getState() == AudioPlayWav.PLAY_PAUSE_STATE){
					mAudioPlayWav.resume();
				}
			}
			break;
		case R.id.stop: // stop record and play
			stopRecordForSafe();
			stopPlayForSafe();
			break;
		case R.id.prev:
			stopRecordForSafe();
			/* till play */
			if (mAudioPlayWav != null){
				mAudioPlayWav.stop();
			}
			mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
					mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getPreFile().getFilePath()));
			break;
		case R.id.next:
			stopRecordForSafe();
			/* till play */
			if (mAudioPlayWav != null){
				mAudioPlayWav.stop();
			}
			mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
					mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getNextFile().getFilePath()));
			break;
		}
	}
	
	@Override
	protected void onRestart() {
		Log.d(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}
	
	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		if (mRecorderWav != null){
			mRecorderWav.stopRecording();
		}
		super.onBackPressed();
	}

	@Override
	public void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}

	/*
	 * Called on destroy to unregister the SD card mount event receiver.
	 */
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		if (mRecorderWav != null){
			mRecorderWav.stopRecording();
		}
		super.onDestroy();
	}
	
	private long mTimeViewTime = 0;

	private void updateTimerRest() {
		//Log.i(TAG, "updateTimerRest");
		String timeStr = String.format(mTimerFormat, 0, 0, 0);
		mTimerView.setText(timeStr);
		mStateLED.setVisibility(View.INVISIBLE);
	}
	private boolean misInUiloopRender = false;
	private void uiLoopRender(boolean loop) {
		misInUiloopRender = true;
//		Log.i(TAG, "uiLoopRender" + "mRecorderwav : " + mRecorderWav + 
//				" mAudioPlayWav : " + mAudioPlayWav + ".mstate " + mState);
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
		//	Log.i(TAG, ".play time." + mAudioPlayWav.getPlayTimeInSec());
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
		
		if (loop){ // loop this the method, in 1sec.
			mLoopHandler.postDelayed(mUpdateTimer, 1000);
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
	
	private int mState = STATE_IDLE;
	private String FileName = "";
	private int mPlayFileLenInSec = 0;
	private Handler mUiHandler = new  Handler(){
		@Override
		public void handleMessage(Message msg) {
			Log.i(TAG, ".uihandler ..get." + msg.what);
			switch (msg.what) {
			case UI_HANDLER_TEST:
				Log.i(TAG, "..get a UI_HANDLER_TEST");
				break;
			case SAVE_FILE_SUCCESS:
				Log.i(TAG, ".uihandler ..in save file success");
				Toast.makeText(SoundRecorderActivity.this, 
						"save file success", Toast.LENGTH_LONG).show();
			     break;
			case FILE_REACH_SIZE:
				Log.i(TAG, "..file_reach size callonClick");
				mRecordButton.callOnClick();
				break;
			case STATE_RECODE_STARTED:
				mState = STATE_RECODE_STARTED;
				FileName = msg.obj.toString();
				//Log.i(TAG, "the FileName is " + FileName);
				uiLoopRender(false);
				break;
			case STATE_RECODE_END:
				mState = STATE_IDLE;
				if (mRecorderWav != null){
					if (mRecorderWav.getState() == RecorderWav.IDLE_STATE){
						mRecorderWav = null;
					}
				}
				//mRecorderWav = null;
				uiLoopRender(false);
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
