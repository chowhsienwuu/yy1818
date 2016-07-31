package com.android.soundrecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ObjectInputStream.GetField;
import java.util.Date;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
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
	
	public static final int ERROR_REACH_SIZE = 0X100;

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
	private String mPasswdword = null;
	private EncryManager mEncryptionManager = null;
	
	//max 3g = 3 * 1024(M) * 1024(k) * 1024(B)
	private static final long MAX_FILE_SIZE = 3 * 1024 * 1024 * 1024;
	//max 3hour 
	private static final long MAX_FILE_TIME = 3 * 60 * 60;
	
	private long mMaxFileSize = MAX_FILE_SIZE; //in Bytes.
	private long mMaxRecodTime = MAX_FILE_TIME; // in sec
	
	private Handler mHandler = null;
	public void setHandler(Handler handler){
		mHandler = handler;
	}
	
	private void sendEmpMsg(int msg){
		if (mHandler == null){
			return;
		}
		switch (msg) {
		case SoundRecorder.FILE_REACH_SIZE:
			
			break;
		case SoundRecorder.SAVE_FILE_SUCCESS:
			Log.i(TAG, "..in WAV.. save file success");
			mHandler.sendEmptyMessage(SoundRecorder.SAVE_FILE_SUCCESS);
			break;
		default:
			break;
		}
	}
	
	/*
	 * ;//44100 * 1(mono) * 2(pcm16) =176400 byte/sec
	 *	0xFFFFFFFF == most == 4294967295
	 * 24347S
	 * 6hour WAV HOST .
	 * 
	 * 
	 */
	
	public RecorderWav(String passwd) {
		Log.i(TAG, "before new encrymanager");
		mEncryptionManager = new EncryManager(passwd);
		Log.i(TAG, "after new encrymanager");
		
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
		
		Log.i(TAG, "..the max File size is " + mMaxFileSize);
	}

	public AudioRecord getAudioRecord() {
		return audioRecord;
	}
	
	public long getRecodTimeInSec(){
		return wavdatalen / mBytePerSec;
	}
	
	public long getRecodFileSize(){
		return wavdatalen + 44;
	}
	
	//in Byte
	public void setMaxFileSize(long size){
		mMaxFileSize = size < MAX_FILE_SIZE ? size : MAX_FILE_SIZE;
	}
	//in sec.
	public void setMaxRecodTime(long time){
		mMaxRecodTime = time < MAX_FILE_TIME ? time : MAX_FILE_TIME;
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
					mEncryptionManager.encryptionbyte(mRecodBuffer, getLen);
					mRecodOutputStream.write(mRecodBuffer, 0, getLen);
					wavdatalen += getLen;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			//CHECK IF THE TIME IS TO BIG SO TIME TO LONG ?.
//			Log.i(TAG, "." + getRecodTimeInSec() + "." + mMaxRecodTime);
//			Log.i(TAG, "." + getRecodFileSize() + "." + mMaxFileSize);
			if (getRecodTimeInSec() >= mMaxRecodTime ){
				stopRecording();
				setError(ERROR_REACH_SIZE);
				Log.e(TAG, ".reach file size or time stop recording");
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
		
		wavdatalen = 0;
		sendEmpMsg(SoundRecorder.SAVE_FILE_SUCCESS);
//		File dir = new File(Environment.getExternalStorageDirectory()
//				.getAbsolutePath(), "WAV_RECODE");
//		if (!dir.exists()) {
//			dir.mkdirs();
//		}
//
//		Date date = new Date();
//		File testFile = new File(dir, "/" + date.getMonth() + "."
//				+ date.getDate() + "_" + date.getHours() + "."
//				+ dat.getMinutes() + "test001 .wav");
//		mEncryptionManager.encryptionFile(mRecodingFile, testFile,
//				mEncryptionManager.passwd2sha512("123456"));
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
		
		//mEncryptionManager.encryptionbyte(header, 44);
		
		return header;
	}

}
