package com.audioencryption;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	RecorderThread mRecordeThread =  new RecorderThread();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
//        new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				EncryManager.main(null);
//			}
//		}).start();
        
        new Thread(mRecordeThread).start();
        mRecordeThread.startRecording();
        
        
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(100000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//mRecordeThread.stopRecording();
			}
		}).start();

	}
    
    
    
}
