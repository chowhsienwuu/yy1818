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
	
	public AudioPlayWav(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
		int minSize = AudioTrack.getMinBufferSize(sampleRate,
				channelConfiguration, audioEncoding);
		
		mbuffer = new byte[40960];
		mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				AudioFormat.CHANNEL_OUT_MONO, audioEncoding,  40960,
				AudioTrack.MODE_STREAM);

		mAudioTrack.setStereoVolume(0.2f, 0.2f);
		mAudioTrack.play();
		
		initFile();
		mPlayThread = new Thread(this);
		mPlayThread.setName("playthread");
		mPlayThread.start();
	}

	private void initFile() {
		// TODO Auto-generated method stub
		mPlayFile = new File("/sdcard/WAV_RECODE/20160805_055637LENTH34s.wav");
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
		byte[] head = new byte[44];
		EncryManager em = new EncryManager("123456");
		try {
			mFis.read(head);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//skip the head.
		
		int len = 0;
		while (true){
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
				break;
			}
		}
		
	}
	
}
