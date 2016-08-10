package com.android.soundrecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

public class AudioPlayWav implements Runnable{
	private Context mContext = null;
	private Handler mUiHandler = null;
	private AudioTrack mAudioTrack = null;
	byte[] mbuffer = null;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; // mono
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; // pcm 16bit.
	private int sampleRate = 44100; // 4.41KHZ
	private File mPlayFile = null;
	private FileInputStream mFis = null;
	private Thread mPlayThread = null;
	
	//play status
	public static final int IDLE_STATE = 0;
	public static final int PLAY_STARTED = 1;
	public static final int PLAY_ERROR_STATE = 2;
	public static final int PLAY_PAUSE_STATE = 4;
	public static final int PLAY_END = 5;
	
	int mState = IDLE_STATE;
	
	
	public int getmState() {
		return mState;
	}


	public void setmState(int mState) {
		this.mState = mState;
	}


	public AudioPlayWav(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
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
		setmState(PLAY_STARTED);
		mPlayThread.start();
	}
	
	
	private void initFile() {
		// TODO Auto-generated method stub
		mPlayFile = new File("/sdcard/WAV_RECODE/20160810_225208LENTH5min25s.wav");
//		mPlayFile = new File("/sdcard/WAV_RECODE/20160805_055637LENTH34s.wav");
		if (!mPlayFile.exists()){
			return;
		}
		try {
			mFis = new FileInputStream(mPlayFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		EncryManager em = new EncryManager("123456");
		try {
			mFis.skip(44);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//skip the head.
			
		int len = 0;
		while (mState == PLAY_STARTED || mState == PLAY_PAUSE_STATE){
			try {
				len = mFis.read(mbuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (len > 0){
				em.decryptionbyte(mbuffer, len);
				mAudioTrack.write(mbuffer, 0, len);
			}else {
				setmState(PLAY_END);
				break;
			}
			
			if (mState == PLAY_PAUSE_STATE && mPlayThread != null){
				doPause();
			}
		}
	}
	
	public void stop(){
		mState = PLAY_END;
		mPlayThread.notify();
		try {
			mFis.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean pause() {
		if (mState == PLAY_STARTED) {
			setmState(PLAY_PAUSE_STATE);
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
				setmState(PLAY_STARTED);
				mPlayThread.notify();
				return true;
			}
		}
		return false;
	}
}
