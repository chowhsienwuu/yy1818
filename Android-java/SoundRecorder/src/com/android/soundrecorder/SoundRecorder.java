/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.soundrecorder;

import java.io.File;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.StatFs;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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
		Recorder.OnStateChangedListener {
	static final String TAG = "SoundRecorder";

	WakeLock mWakeLock;
	Recorder mRecorder;
	String mErrorUiMessage = null; // Some error messages are displayed in the
									// UI,
									// not a dialog. This happens when a
									// recording
									// is interrupted for some reason.

	long mMaxFileSize = -1; // can be specified in the intent
	RemainingTimeCalculator mRemainingTimeCalculator;

	String mTimerFormat;
	final Handler mHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() {
			updateTimerView();
		}
	};

	ImageButton mRecordButton;
	ImageButton mPlayButton;
	ImageButton mStopButton;

	ImageView mStateLED;
	TextView mStateMessage1;
	TextView mStateMessage2;
	ProgressBar mStateProgressBar;
	TextView mTimerView;

	VUMeter mVUMeter;

	private RecorderWav mRecorderWav = null;

	@Override
	public void onCreate(Bundle icycle) {
		super.onCreate(icycle);
		mRecorderWav = new RecorderWav();

		setContentView(R.layout.main);
//		setContentView(R.)
		mRecorder = new Recorder();
		mRecorder.setOnStateChangedListener(this);
		mRemainingTimeCalculator = new RemainingTimeCalculator();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"SoundRecorder");

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
		mPlayButton = (ImageButton) findViewById(R.id.playButton);
		mStopButton = (ImageButton) findViewById(R.id.stopButton);

		mStateLED = (ImageView) findViewById(R.id.stateLED);
		mStateMessage1 = (TextView) findViewById(R.id.stateMessage1);
		mStateMessage2 = (TextView) findViewById(R.id.stateMessage2);
		mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);
		mTimerView = (TextView) findViewById(R.id.timerView);

		mVUMeter = (VUMeter) findViewById(R.id.uvMeter);

		mRecordButton.setOnClickListener(this);
		mPlayButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);

		mTimerFormat = getResources().getString(R.string.timer_format);

		mVUMeter.setRecorder(mRecorder);
	}

	/*
	 * Make sure we're not recording music playing in the background, ask the
	 * MediaPlaybackService to pause playback.
	 */
	private void stopAudioPlayback() {
		// Shamelessly copied from MediaPlaybackService.java, which
		// should be public, but isn't.
		Intent i = new Intent("com.android.music.musicservicecommand");
		i.putExtra("command", "pause");

		sendBroadcast(i);
	}

	/*
	 * Handle the buttons.
	 */
	public void onClick(View button) {

		switch (button.getId()) {
		case R.id.recordButton:
			mRecorderWav.startRecording();
			mStopButton.setClickable(true);

			break;
		case R.id.playButton:
			mRecorder.startPlayback();
			break;
		case R.id.stopButton:
			mRecorderWav.stopRecording();
			// mRecorder.stop();
			break;
		}
	}

	/*
	 * Handle the "back" hardware key.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			switch (mRecorder.state()) {
			case Recorder.IDLE_STATE:
				if (mRecorder.sampleLength() > 0)
					saveSample();
				finish();
				break;
			case Recorder.PLAYING_STATE:
				mRecorder.stop();
				saveSample();
				break;
			case Recorder.RECORDING_STATE:
				mRecorder.clear();
				break;
			}
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	public void onStop() {

		super.onStop();
	}

	@Override
	protected void onPause() {

		super.onPause();
	}

	/*
	 * If we have just recorded a smaple, this adds it to the media data base
	 * and sets the result to the sample's URI.
	 */
	private void saveSample() {
	}

	/*
	 * Called on destroy to unregister the SD card mount event receiver.
	 */
	@Override
	public void onDestroy() {

		super.onDestroy();
	}

	/**
	 * Update the big MM:SS timer. If we are in playback, also update the
	 * progress bar.
	 */
	private void updateTimerView() {

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
		Resources res = getResources();

		switch (mRecorder.state()) {
		case Recorder.IDLE_STATE:
			break;
		case Recorder.RECORDING_STATE:
			break;
		case Recorder.PLAYING_STATE:
			break;
		}

		updateTimerView();
		mVUMeter.invalidate();
	}

	/*
	 * Called when Recorder changed it's state.
	 */
	public void onStateChanged(int state) {

		updateUi();
	}

	/*
	 * Called when MediaPlayer encounters an error.
	 */
	public void onError(int error) {
	}
}
