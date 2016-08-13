
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
import android.widget.TextView;
import android.widget.Toast;


public class SoundRecorderActivity extends Activity implements Button.OnClickListener,
		RecorderWav.OnStateChangedListener {
	static final String TAG = "zxw";

	WakeLock mWakeLock;

	String mErrorUiMessage = null; // Some error messages are displayed in the

	long mMaxFileSize = -1; // can be specified in the intent

	String mTimerFormat;
	final Handler mLoopHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			updateTimerView();
		}
	};

	ImageButton mRecordButton;
	ImageButton mPauseButton;
	ImageButton mStopButton;
	
	Button mPlayPause;
	Button mPlayStop;
	Button mPlayNext;
	Button mPlayPrev;
	
	ImageView mStateLED;
	TextView mStateMessage1;
	TextView mStateMessage2;
	ProgressBar mStateProgressBar;
	TextView mTimerView;
	EditText mPasswdText = null;
	
	Resources res = null;
	private RecorderWav mRecorderWav = null;
	private AudioPlayWav mAudioPlayWav = null;
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
				
		updateUi();
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
		mStopButton = (ImageButton) findViewById(R.id.stopButton);
		mPauseButton = (ImageButton) findViewById(R.id.pauseButton);
		
		mStateLED = (ImageView) findViewById(R.id.stateLED);
		mStateMessage1 = (TextView) findViewById(R.id.stateMessage1);
		mStateMessage2 = (TextView) findViewById(R.id.stateMessage2);
		mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);
		mTimerView = (TextView) findViewById(R.id.timerView);
		mPasswdText = (EditText) findViewById(R.id.passwdEdit);
		
		mPlayNext = (Button)findViewById(R.id.next);
		mPlayPrev = (Button)findViewById(R.id.prev);
		mPlayPause = (Button)findViewById(R.id.playpause);
		mPlayStop = (Button)findViewById(R.id.stop);
		
		mRecordButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);
		mPauseButton.setOnClickListener(this);
		mPlayNext.setOnClickListener(this);
		mPlayPause.setOnClickListener(this);
		mPlayStop.setOnClickListener(this);
		mPlayPrev.setOnClickListener(this);
		
		mTimerFormat = getResources().getString(R.string.timer_format);

	}

	/*
	 * Handle the buttons.
	 */
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
			mRecorderWav = new RecorderWav(this, mPasswdText.getEditableText().toString());
			mRecorderWav.setOnStateChangedListener(this);
			mRecorderWav.setHandler(mUiHandler);
//			mRecorderWav.setMaxRecodTime(5);
//			mRecorderWav.setMaxFileSize(300 * 1024L);
			mRecorderWav.startRecording();
            mStateLED.setImageResource(R.drawable.recording_led);
			break;
		case R.id.pauseButton:
			Log.i(TAG, "pauseRecording..click");
			mRecorderWav.pauseRecording();
			break;
		case R.id.stopButton:
			Log.i(TAG, "stopButton..click");
			mRecorderWav.stopRecording();
			// mRecorder.stop();
			break;
		case R.id.playpause:
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
		case R.id.stop: //play wav.stop.
			if (mAudioPlayWav != null){
				mAudioPlayWav.stop();
			}
			mAudioPlayWav = null;
			break;
		case R.id.prev:
			/* till play */
			if (mAudioPlayWav != null){
				mAudioPlayWav.stop();
			}
			mAudioPlayWav = new AudioPlayWav(this, mUiHandler, 
					mPasswdText.getEditableText().toString(), new File(FileManager.getInstance().getPreFile().getFilePath()));
			break;
		case R.id.next:
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
		// TODO Auto-generated method stub
		Log.d(TAG, "onRestart");
		super.onRestart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onResume");
		super.onResume();
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onStart");
		super.onStart();
	}

	@Override
	public void onBackPressed() {
		// TDO Auto-generated method stub
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
	
	private void updateTimerView() {
		Log.e("zxw", "updateTimerview");
		if (mRecorderWav != null) {
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

			if (mRecorderWav.getState() == RecorderWav.RECORDING_STARTED) {
				mLoopHandler.postDelayed(mUpdateTimer, 1000);
			}
		}
		
		if (mAudioPlayWav != null){
			Log.e("zxw", ".play time." + mAudioPlayWav.getPlayTimeInSec());
			mTimeViewTime = mAudioPlayWav.getPlayTimeInSec();
			int[] hms = MiscUtil.sec2hms(mTimeViewTime);

			String timeStr = String
					.format(mTimerFormat, hms[0], hms[1], hms[2]);
			mTimerView.setText(timeStr);
			
			if (mAudioPlayWav.getState() == AudioPlayWav.PLAY_STARTED ||
					mAudioPlayWav.getState() == AudioPlayWav.PLAY_PAUSE_STATE) {
				mLoopHandler.postDelayed(mUpdateTimer, 1000);
			}
		}
	}

	/**
	 * Shows/hides the appropriate child views for the new state.
	 */
	private void updateUi() {
		if (mRecorderWav == null) {
			mRecordButton.setClickable(true);
			mPasswdText.setEnabled(true);
			mPauseButton.setClickable(false);
			mStopButton.setClickable(false);
			mStateLED.setVisibility(View.INVISIBLE);
			return;
		}
		switch (mRecorderWav.getState()) {
		case RecorderWav.IDLE_STATE:
			mStateLED.setVisibility(View.INVISIBLE);
			mRecordButton.setClickable(true);
			mPasswdText.setEnabled(true);
			mPauseButton.setClickable(false);
			mStopButton.setClickable(false);
			break;
		case RecorderWav.RECORDING_STARTED:
			mStateLED.setVisibility(View.VISIBLE);
			mRecordButton.setClickable(false);
			mPauseButton.setClickable(true);
			mStopButton.setClickable(true);
			mPasswdText.setEnabled(false);
			break;
		case RecorderWav.RECORDING_PAUSE_STATE:
			mStateLED.setVisibility(View.INVISIBLE);
			mRecordButton.setClickable(false);
			mPauseButton.setClickable(true);
			mStopButton.setClickable(true);
			break;
		}

		updateTimerView();
	}

	/*
	 * Called when Recorder changed it's state.
	 */
	public void onStateChanged(int state) {

//		updateUi();
		mUiHandler.sendEmptyMessage(UI_HANDLER_UPDATE_UP);
	}
	/*
	 * Called when MediaPlayer encounters an error.
	 */
	public void onError(int error) {
//		mUiHandler.sendEmptyMessage(UI_HANDLER_TEST);
//		mLoopHandler.sendEmptyMessage(FILE_REACH_SIZE);
		Log.i(TAG, "..ui OnError" + error);
		switch (error) {
		case RecorderWav.ERROR_REACH_SIZE:
			//now we start a another recod.
			Log.i(TAG, "..ui..oneror..sendemptymsg file_reach_size");
			mUiHandler.sendEmptyMessage(FILE_REACH_SIZE);
			break;

		default:
			break;
		}
	}
	/*
	 * 
	 */
	public static final int UI_HANDLER_UPDATE_UP = 0X01;
	public static final int SAVE_FILE_SUCCESS = 0x02;
	public static final int FILE_REACH_SIZE = 0X3;
	public static final int UI_HANDLER_UPDATE_TIMERVIEW = 0X04;
	public static final int UI_HANDLER_TEST = 0X10;
	private Handler mUiHandler = new  Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Log.i(TAG, ".uihandler ..get." + msg.what);
			switch (msg.what) {
			case UI_HANDLER_UPDATE_UP:
				updateUi();
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
			case UI_HANDLER_TEST:
				Log.i(TAG, "..get a UI_HANDLER_TEST");
				break;
			case UI_HANDLER_UPDATE_TIMERVIEW:
				updateTimerView();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
	
}
