package com.android.soundrecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class RecorderWav implements Runnable {
	static final String SAMPLE_PREFIX = "RecorderWav";

	public static final int IDLE_STATE = 0;
	public static final int RECORDING_STATE = 1;

	public static final int RECORDING_ERROR_STATE = 2;
	public static final int PLAYING_STATE = 3;
	int mState = IDLE_STATE;

	public static final int NO_ERROR = 0;
	public static final int SDCARD_ACCESS_ERROR = 1;
	public static final int INTERNAL_ERROR = 2;
	public static final int IN_CALL_RECORD_ERROR = 3;

	public interface OnStateChangedListener {
		public void onStateChanged(int state);
		public void onError(int error);
	}

	OnStateChangedListener mOnStateChangedListener = null;

	//

	private static final String TAG = "RecorderWav";
	private AudioRecord audioRecord;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; // mono
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // pcm 16bit.
	private int sampleRate = 44100; // 4.41KHZ
	private int bufferSizeInBytes = -1;
	private byte[] mRecodBuffer = null;

	File mRecodingFile = null;
	FileOutputStream mRecodOutputStream = null;
	Thread mRecodThread = null;

	public RecorderWav() {
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate,
				channelConfiguration, audioEncoding);
		bufferSizeInBytes = 4096 * 10;
		Log.i(TAG, "bufferSizeInBytes=" + bufferSizeInBytes); // 4096 byte.
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRate, channelConfiguration, audioEncoding,
				bufferSizeInBytes);
		mRecodBuffer = new byte[bufferSizeInBytes];
		mRecodThread = new Thread(this);
	}

	public AudioRecord getAudioRecord() {
		return audioRecord;
	}

	public int getState() {
		return mState;
	}

	public void startRecording() {
		try {
			initFile();
			mState = RECORDING_STATE;
			mRecodThread.start();
			audioRecord.startRecording();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			mState = RECORDING_ERROR_STATE;
		}
	}

	public void stopRecording() {
		try {
			audioRecord.stop(); // CHECK THIS FILE.
			mState = IDLE_STATE;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			mState = RECORDING_ERROR_STATE;
		}
	}

	private void initFile() {
		File dir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), "WAV_RECODE");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		//
		Date date = new Date();
		mRecodingFile = new File(dir, "/" + date.getMonth() + "."
				+ date.getDate() + "_" + date.getHours() + "."
				+ date.getMinutes() + ".wav");
		try {
			mRecodOutputStream = new FileOutputStream(mRecodingFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			mRecodOutputStream.write(getWavHeader(9696999));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // file write the head.

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int getLen = 0;
		long wavdatalen = 0L;
		while (mState == RECORDING_STATE) {
			getLen = audioRecord.read(mRecodBuffer, 0, bufferSizeInBytes);
			if (getLen > 0) {
				try {
					for (int i = 0; i < getLen; i++) {
						mRecodBuffer[i] = (byte) (mRecodBuffer[i]);
					}
					mRecodOutputStream.write(mRecodBuffer, 0, getLen);
					wavdatalen += getLen;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		try {
			mRecodOutputStream.flush();
			mRecodOutputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			RandomAccessFile raf = new RandomAccessFile(mRecodingFile, "rw");
			raf.seek(0);
			raf.write(getWavHeader(wavdatalen));
			raf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// mRecodingFile.
	}

	private byte[] getWavHeader(long totalAudioLen) {
		int mChannels = 1;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRate;
		long byteRate = sampleRate * 2 * mChannels;

		byte[] header = new byte[44];
		header[0] = 'R'; 
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f';
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; 
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; 
		header[21] = 0;
		header[22] = (byte) mChannels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * mChannels); 
		header[33] = 0;
		header[34] = 16; 
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		return header;
	}

}
