package com.encryption.soundrecorder.wav;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.encryption.soundrecorder.SoundRecorderActivity;
import com.encryption.soundrecorder.encryption.EncryManager;
import com.encryption.soundrecorder.file.FileManager;
import com.encryption.soundrecorder.util.Fm1388Util;

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

	private AudioRecord audioRecord;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; // mono
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // pcm 16bit.
	private int sampleRate = 16000; // 4.41KHZ
	private int bufferSizeInBytes = -1;
	private byte[] mRecodBuffer = null;
	
	private int mBytePerSec = -1; 
	private long wavdatalen = 0L; //how many byte write in.
	
	private File mRecodingFile = null;
	private RandomAccessFile mRecodRaf = null;
	private Thread mRecodThread = null;
	private EncryManager mEncryptionManager = null;
	
	private static final long MAX_FILE_SIZE = 1 * 1024 * 1024 * 1024L;
	//max 3hour 
	private static final long MAX_FILE_TIME = 3 * 60 * 60L;
	//private static final long MAX_FILE_TIME = 5;
	private long mMaxFileSize = MAX_FILE_SIZE; //in Bytes.
	private long mMaxRecodTime = MAX_FILE_TIME; // in sec
	
	private Context mContext = null;
	private Handler mHandler = null;
	
	public RecorderWav(Context context, Handler hander, String passwd) {
		mContext = context;
		mHandler = hander;
		mEncryptionManager = new EncryManager(passwd);
		
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
		return ((long)wavdatalen) / mBytePerSec;
	}
	
	public long getRecodFileSize(){
		return wavdatalen + 44L;
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
	}
	
	public synchronized void startRecording() {
		Fm1388Util.changeModeVr();
		
		Message msg = new Message();
		try {
			initFile();
			setState(RECORDING_STARTED);
			mRecodThread.start();
			audioRecord.startRecording();
			msg.what = SoundRecorderActivity.STATE_RECODE_STARTED;
		} catch (Exception e) {
			e.printStackTrace();
			msg.what = SoundRecorderActivity.STATE_RECODE_ERR;
			setState(RECORDING_ERROR_STATE);
		}

		msg.obj = mRecodingFile.getName();
		mHandler.sendMessage(msg);
	}

	public synchronized void stopRecording() {
		try {
			//audioRecord.
			audioRecord.stop(); 
			audioRecord.release();
			setState(IDLE_STATE);
			mHandler.sendEmptyMessage(SoundRecorderActivity.STATE_RECODE_END);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			setState(RECORDING_ERROR_STATE);
			mHandler.sendEmptyMessage(SoundRecorderActivity.STATE_RECODE_ERR);
		}
	}
	
	
	private void initFile() throws Exception {
		String name = FileManager.getInstance().genNewRecodFileName();
		mRecodThread.setName(name);
		mRecodThread.setPriority(Thread.MAX_PRIORITY);
		mRecodingFile = new File(FileManager.getInstance().getWAVrootDir(), "/" + name);

		mRecodRaf = new RandomAccessFile(mRecodingFile, "rws");
		mRecodRaf.write(getWavHeader(1));
	}

	@Override
	public void run() {
		int getLen = 0; 
		while (mState == RECORDING_STARTED || mState == RECORDING_PAUSE_STATE) {
			getLen = audioRecord.read(mRecodBuffer, 0, bufferSizeInBytes);
			//when paused , do not block read data but do not write into data file
			if (getLen > 0 && mState != RECORDING_PAUSE_STATE){
				try {
					mEncryptionManager.encryptionbyte(mRecodBuffer, getLen);
					mRecodRaf.write(mRecodBuffer, 0, getLen);
					wavdatalen += getLen;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (wavdatalen % (1024 * 256) == 0) {
				try {
					mRecodRaf.seek(0);
					mRecodRaf.write(getWavHeader(wavdatalen));
					mRecodRaf.seek(44 + wavdatalen);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			if (getRecodTimeInSec() >= mMaxRecodTime 
					|| getRecodFileSize() >= mMaxFileSize){
				stopRecording();
				mHandler.sendEmptyMessage(SoundRecorderActivity.FILE_REACH_SIZE);
				Log.i(TAG, ".reach file size or time stop recording");
			}
		}
		
		stopRecording();

		try {
			mRecodRaf.seek(0);
			mRecodRaf.write(getWavHeader(wavdatalen));
			mRecodRaf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		FileManager.getInstance().addFileNode(mRecodingFile);
		FileManager.getInstance().addWavFileMTP(mContext, mRecodingFile);
		wavdatalen = 0;
		
		if (getState() == IDLE_STATE) {
			mHandler.sendEmptyMessage(SoundRecorderActivity.SAVE_FILE_SUCCESS);
		}
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
