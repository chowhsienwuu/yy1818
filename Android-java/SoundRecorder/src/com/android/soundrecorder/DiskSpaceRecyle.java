package com.android.soundrecorder;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class DiskSpaceRecyle implements Runnable {
	
	private DiskSpaceRecyle(){
		mDiskSpaceRecyleThread = new Thread(this);
		mDiskSpaceRecyleThread.setName(TAG);
		
		mSDCardDirectory = Environment.getExternalStorageDirectory();
		mWAVDirectory = new File(mSDCardDirectory.getAbsolutePath() + "/WAV_RECODE/");
		if (!mWAVDirectory.exists()){
			mWAVDirectory.mkdir();
		}
	}
	
	public static final String TAG = "zxw";
	private Thread mDiskSpaceRecyleThread = null;
	private static DiskSpaceRecyle mInstance = new DiskSpaceRecyle();
	private long  mSleepTime = 180 * 1000; //3min to check once;
	private File mSDCardDirectory = null;
	private File mWAVDirectory = null;
//	private long mEmptySize = 6024678656L; // 500M 
	private long mEmptySize = 500 * 1024 * 1024L ; // 500M 
	
	
	public static DiskSpaceRecyle getInstance(){
		return mInstance;
	}
	
	public boolean start(){
		if (!mDiskSpaceRecyleThread.isAlive()) {
			mDiskSpaceRecyleThread.start();
		}
		
		return mDiskSpaceRecyleThread.isAlive();
	}
	
	public boolean setEmpSize(long size){
		if (size > mSDCardDirectory.getTotalSpace() || size < 0L){
			return false;
		}
		mEmptySize = size;
		return true;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true){
			
			doDiskCheck();
			try {
				Thread.sleep(mSleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void doDiskCheck() {
//		Log.i(TAG, ".lanlan.avail " + mWAVDirectory.getFreeSpace()
//				+ "..emtpsize " + mEmptySize);

		while (mWAVDirectory.getFreeSpace() < mEmptySize) {
			if (!deleteOldestFile(mWAVDirectory)) {
				break;
			}
		}
	}
	
	private boolean deleteOldestFile(File dir) {
		String[] fileList = dir.list();
		if (fileList == null) {
			return false;
		}

		File oldestFile = null;
		File tempFile = null;
		for (int i = 0; i < fileList.length; i++) {
			if (i == 0) {
				oldestFile = new File(dir, fileList[i]);
			}
			tempFile = new File(dir, fileList[i]);
			if (oldestFile.lastModified() > tempFile.lastModified()) {
				oldestFile = tempFile;
			}
		}
		if (oldestFile != null) {
			Log.i(TAG, "delte file : " + oldestFile.getAbsolutePath());
			return oldestFile.delete();
		}
		return false;
	}
}
