
package com.android.soundrecorder;

import java.io.File;

import javax.security.auth.login.LoginException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Calculates remaining recording time based on available disk space and
 * optionally a maximum recording file size.
 * 
 * The reason why this is not trivial is that the file grows in blocks every few
 * seconds or so, while we want a smooth countdown.
 */

class RemainingTimeCalculator {
	public static final int UNKNOWN_LIMIT = 0;
	public static final int FILE_SIZE_LIMIT = 1;
	public static final int DISK_SPACE_LIMIT = 2;

	// which of the two limits we will hit (or have fit) first
	private int mCurrentLowerLimit = UNKNOWN_LIMIT;

	private File mSDCardDirectory;

	// State for tracking file size of recording.
	private File mRecordingFile;
	private long mMaxBytes;

	// Rate at which the file grows
	private int mBytesPerSecond;

	// time at which number of free blocks last changed
	private long mBlocksChangedTime;
	// number of available blocks at that time
	private long mLastBlocks;

	// time at which the size of the file has last changed
	private long mFileSizeChangedTime;
	// size of the file at that time
	private long mLastFileSize;

	public RemainingTimeCalculator() {
		mSDCardDirectory = Environment.getExternalStorageDirectory();
	}

	/**
	 * If called, the calculator will return the minimum of two estimates: how
	 * long until we run out of disk space and how long until the file reaches
	 * the specified size.
	 * 
	 * @param file
	 *            the file to watch
	 * @param maxBytes
	 *            the limit
	 */

	public void setFileSizeLimit(File file, long maxBytes) {
		mRecordingFile = file;
		mMaxBytes = maxBytes;
	}

	/**
	 * Resets the interpolation.
	 */
	public void reset() {
		mCurrentLowerLimit = UNKNOWN_LIMIT;
		mBlocksChangedTime = -1;
		mFileSizeChangedTime = -1;
	}

	/**
	 * Returns how long (in seconds) we can continue recording.
	 */
	public long timeRemaining() {
		// Calculate how long we can record based on free disk space

		StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
		long blocks = fs.getAvailableBlocks();
		long blockSize = fs.getBlockSize();
		long now = System.currentTimeMillis();

		if (mBlocksChangedTime == -1 || blocks != mLastBlocks) {
			mBlocksChangedTime = now;
			mLastBlocks = blocks;
		}

		/*
		 * The calculation below always leaves one free block, since free space
		 * in the block we're currently writing to is not added. This last block
		 * might get nibbled when we close and flush the file, but we won't run
		 * out of disk.
		 */

		// at mBlocksChangedTime we had this much time
		long result = mLastBlocks * blockSize / mBytesPerSecond;
		// so now we have this much time
		result -= (now - mBlocksChangedTime) / 1000;

		if (mRecordingFile == null) {
			mCurrentLowerLimit = DISK_SPACE_LIMIT;
			return result;
		}

		// If we have a recording file set, we calculate a second estimate
		// based on how long it will take us to reach mMaxBytes.

		mRecordingFile = new File(mRecordingFile.getAbsolutePath());
		long fileSize = mRecordingFile.length();
		if (mFileSizeChangedTime == -1 || fileSize != mLastFileSize) {
			mFileSizeChangedTime = now;
			mLastFileSize = fileSize;
		}

		long result2 = (mMaxBytes - fileSize) / mBytesPerSecond;
		result2 -= (now - mFileSizeChangedTime) / 1000;
		result2 -= 1; // just for safety

		mCurrentLowerLimit = result < result2 ? DISK_SPACE_LIMIT
				: FILE_SIZE_LIMIT;

		return Math.min(result, result2);
	}

	/**
	 * Indicates which limit we will hit (or have hit) first, by returning one
	 * of FILE_SIZE_LIMIT or DISK_SPACE_LIMIT or UNKNOWN_LIMIT. We need this to
	 * display the correct message to the user when we hit one of the limits.
	 */
	public int currentLowerLimit() {
		return mCurrentLowerLimit;
	}

	/**
	 * Is there any point of trying to start recording?
	 */
	public boolean diskSpaceAvailable() {
		StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
		// keep one free block
		return fs.getAvailableBlocks() > 1;
	}

	/**
	 * Sets the bit rate used in the interpolation.
	 * 
	 * @param bitRate
	 *            the bit rate to set in bits/sec.
	 */
	public void setBitRate(int bitRate) {
		mBytesPerSecond = bitRate / 8;
	}
}

public class SoundRecorder extends Activity implements Button.OnClickListener,
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

	ImageView mStateLED;
	TextView mStateMessage1;
	TextView mStateMessage2;
	ProgressBar mStateProgressBar;
	TextView mTimerView;
	EditText mPasswdText = null;
	
	Resources res = null;
	private RecorderWav mRecorderWav = null;

	@Override
	public void onCreate(Bundle icycle) {
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
		
		mRecordButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);
		mPauseButton.setOnClickListener(this);
		
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
			mRecorderWav = new RecorderWav(mPasswdText.getEditableText().toString());
			mRecorderWav.setOnStateChangedListener(this);
			mRecorderWav.setHandler(mUiHandler);
			//mRecorderWav.setMaxRecodTime(5);
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

	private long mRecodeTime = 0;

	private void updateTimerView() {
		if (mRecorderWav == null) {
			return;
		}
		
		int state = mRecorderWav.getState();

		boolean ongoing = state == RecorderWav.RECORDING_STARTED;

		mRecodeTime = mRecorderWav.getRecodTimeInSec();
//		Log.i("zxw", "update timervier" + mRecodeTime + "..ongoing " + true);
		int hour = (int)(mRecodeTime / 3600);
		int sec = (int)(mRecodeTime % 60);
		int min = (int)((mRecodeTime - 3600 * hour) / 60);

		String timeStr = String.format(mTimerFormat, hour, min, sec);
		mTimerView.setText(timeStr);
		
		if (mRecodeTime % 2 == 1){
			mStateLED.setVisibility(View.INVISIBLE);
		}else {
			mStateLED.setVisibility(View.VISIBLE);
		}
		
		updateTimeRemaining();

		if (ongoing)
			mLoopHandler.postDelayed(mUpdateTimer, 1000);
	}

	/*
	 * Called when we're in recording state. Find out how much longer we can go
	 * on recording. If it's under 5 minutes, we display a count-down in the UI.
	 * If we've run out of time, stop the recording.
	 */
	private void updateTimeRemaining() {

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
				Toast.makeText(SoundRecorder.this, 
						"save file success", Toast.LENGTH_LONG).show();
			case FILE_REACH_SIZE:
				Log.i(TAG, "..file_reach size callonClick");
				mRecordButton.callOnClick();
				break;
			case UI_HANDLER_TEST:
				Log.i(TAG, "..get a UI_HANDLER_TEST");
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};
	
}
