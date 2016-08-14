package com.android.soundrecorder.wav;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.android.soundrecorder.SoundRecorderActivity;
import com.android.soundrecorder.encryption.EncryManager;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AudioPlayWav implements Runnable{
	private Context mContext = null;
	private Handler mUiHandler = null;
	private AudioTrack mAudioTrack = null;
	byte[] mbuffer = null;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; // mono
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // pcm 16bit.
	private int sampleRate = 44100; // 4.41KHZ
	private File mPlayFile = null;
	private RandomAccessFile mRaf = null;
	private Thread mPlayThread = null;
	
	//play status
	public static final int IDLE_STATE = 0;
	public static final int PLAY_STARTED = 1;
	public static final int PLAY_ERROR_STATE = 2;
	public static final int PLAY_PAUSE_STATE = 4;
	public static final int PLAY_END = 5;
	private static final String TAG = "AudioPlayWav";
	
	int mState = IDLE_STATE;
	
	private String mPasswd = "";
	
	public int getState() {
		return mState;
	}

	public void setState(int mState) {
		this.mState = mState;
	}

	public AudioPlayWav(Context context, Handler handler, String passwd, File wav){
		mContext = context;
		mUiHandler = handler;
		mPasswd = passwd;
		mPlayFile = wav;
		Log.i(TAG, "play WAV file is " + wav.getAbsolutePath());
		
		int minSize = AudioTrack.getMinBufferSize(sampleRate,
				channelConfiguration, audioEncoding);
		
		mbuffer = new byte[10240];
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, audioEncoding,  10240,
				AudioTrack.MODE_STREAM);

		mAudioTrack.setStereoVolume(1.0f, 1.0f);
		mAudioTrack.play();
		
		initFile();
		mPlayThread = new Thread(this);
		mPlayThread.setName("playthread");
		setState(PLAY_STARTED);
		mPlayThread.start();
		
		Message msg = new Message();
		msg.what = SoundRecorderActivity.STATE_PLAY_STARTED;
		msg.obj = mPlayFile.getName();
		mUiHandler.sendMessage(msg);
//		mUiHandler.sendEmptyMessage(SoundRecorderActivity.STATE_PLAY_STARTED);
	}
	
	private void initFile() {
		if (!mPlayFile.exists()){
			return;
		}
		try {
			mRaf = new RandomAccessFile(mPlayFile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		EncryManager em = new EncryManager(mPasswd);
		try {
			mRaf.skipBytes(44);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int len = 0;
		while (mState == PLAY_STARTED || mState == PLAY_PAUSE_STATE){
			try {
				len = mRaf.read(mbuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (len > 0){
				em.decryptionbyte(mbuffer, len);
				mAudioTrack.write(mbuffer, 0, len);
			}else {
				setState(PLAY_END);
				break;
			}
			
			if (mState == PLAY_PAUSE_STATE && mPlayThread != null){
				doPause();
			}
		}
		
		stop();
	}
	
	public long getPlayTimeInSec(){
		try {
			return mRaf.getFilePointer() / (44100 * 2 * 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0L;
	}
	
	public void stop(){
		mState = PLAY_END;
		if (mState == PLAY_PAUSE_STATE && mPlayThread != null) {
			mPlayThread.notify();
		}
		try {
			mRaf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mAudioTrack.release();
		mUiHandler.sendEmptyMessage(SoundRecorderActivity.STATE_PLAY_END);
	}
	
	public boolean pause() {
		if (mState == PLAY_STARTED) {
			setState(PLAY_PAUSE_STATE);
			return true;
		}
		return false;
	}

	private void doPause(){
		if (mPlayThread != null && mState == PLAY_PAUSE_STATE){
			synchronized (mPlayThread) {
				try {
					mPlayThread.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public boolean resume(){
		if (mPlayThread != null && mState == PLAY_PAUSE_STATE){
			synchronized (mPlayThread) {
				setState(PLAY_STARTED);
				mPlayThread.notify();
				return true;
			}
		}
		return false;
	}
}
