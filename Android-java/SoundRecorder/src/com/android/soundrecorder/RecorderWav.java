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
	private static final String TAG = "RecorderWav";
	
	public static final int IDLE_STATE = 0;
	public static final int RECORDING_STARTED = 1;
	public static final int RECORDING_ERROR_STATE = 2;
	public static final int PLAYING_STATE = 3;
	public static final int RECORDING_PAUSE_STATE = 4;
	public static final int SUCCESS_SAVE_FILE = 5;
	
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
	
	public void setOnStateChangedListener(OnStateChangedListener listener) {
		mOnStateChangedListener = listener;
	}
	
	private void signalStateChanged(int state) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onStateChanged(state);
	}

	private void setError(int error) {
		if (mOnStateChangedListener != null)
			mOnStateChangedListener.onError(error);
	}

	private AudioRecord audioRecord;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; // mono
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // pcm 16bit.
	private int sampleRate = 44100; // 4.41KHZ
	private int bufferSizeInBytes = -1;
	private byte[] mRecodBuffer = null;
	
	private int mBytePerSec = -1; 
	private long wavdatalen = 0L; //how many byte write in.
	
	private File mRecodingFile = null;
	private FileOutputStream mRecodOutputStream = null;
	private Thread mRecodThread = null;

	/*
	 * ;//44100 * 1(mono) * 2(pcm16) =176400 byte/sec
	 *	0xFFFFFFFF == most == 4294967295
	 * 24347S
	 * 6hour WAV HOST .
	 * 
	 * 
	 */
	
	public RecorderWav() {
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate,
				channelConfiguration, audioEncoding);
		bufferSizeInBytes = 4096 * 10; // 
		Log.i(TAG, "bufferSizeInBytes=" + bufferSizeInBytes); // 4096 byte.
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRate, channelConfiguration, audioEncoding,
				bufferSizeInBytes);
		
		mBytePerSec = sampleRate * 1 * 2 ;//44100 * 1(mono) * 2(pcm16) =176400
		mRecodBuffer = new byte[bufferSizeInBytes];
		mRecodThread = new Thread(this);
	}

	public AudioRecord getAudioRecord() {
		return audioRecord;
	}
	
	public long getRecodTimeInSec(){
		return wavdatalen / mBytePerSec;
	}
	
	
	public int getState() {
		return mState;
	}
	
	public synchronized  void pauseRecording(){
		if (mState == RECORDING_STARTED){
			setState(RECORDING_PAUSE_STATE);
		}else if (mState == RECORDING_PAUSE_STATE){
			setState(RECORDING_STARTED);
		}
	}
	
	private void setState(int statue){
		if (mState == statue){
			return;
		}
		mState = statue;
		signalStateChanged(mState);
	}
	
	public synchronized void startRecording() {
		try {
			initFile();
			setState(RECORDING_STARTED);
			mRecodThread.start();
			audioRecord.startRecording();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			setState(RECORDING_ERROR_STATE);
		}
	}

	public synchronized void stopRecording() {
		try {
			audioRecord.stop(); // CHECK THIS FILE.
			audioRecord.release();
			setState(IDLE_STATE);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			setState(RECORDING_ERROR_STATE);
		}
	}

	private void initFile() {
		File dir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), "WAV_RECODE");
		if (!dir.exists()) {
			dir.mkdirs();
		}

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
		
//		mRecodingFile.
		try {
			// just write the WAV HEAD!.
			mRecodOutputStream.write(getWavHeader(1));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		int getLen = 0;
		while (mState == RECORDING_STARTED || mState == RECORDING_PAUSE_STATE) {
			getLen = audioRecord.read(mRecodBuffer, 0, bufferSizeInBytes);
			//when paused , do not block read data but do not write into data file
			if (getLen > 0 && mState != RECORDING_PAUSE_STATE){
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
//		sta
		wavdatalen = 0;
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
