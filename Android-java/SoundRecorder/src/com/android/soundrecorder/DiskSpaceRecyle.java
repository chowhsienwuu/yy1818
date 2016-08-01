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
		
		StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
		long blocks = fs.getAvailableBlocks();
		long blockSize = fs.getBlockSize();
		mSDCardDirectory.getTotalSpace();
		
		
	}
	public static final String TAG = "zxw";
	private Thread mDiskSpaceRecyleThread = null;
	private static DiskSpaceRecyle mInstance = new DiskSpaceRecyle();
//	private boolean mIsRun = flase;
	private long  mSleepTime = 3 * 1000; //1min to check once;
	private File mSDCardDirectory = null;
	private File mWAVDirectory = null;
	private long mEmptySize = 5024678656L; // 500M 
//	private long mEmptySize = 500 * 1024 * 1024 ; // 500M 
	
	
	public static DiskSpaceRecyle getInstance(){
		return mInstance;
	}
	
	public boolean start(){
		if (!mDiskSpaceRecyleThread.isAlive()) {
			mDiskSpaceRecyleThread.start();
		}
		
		return mDiskSpaceRecyleThread.isAlive();
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

	private void doDiskCheck(){
		StatFs fs = new StatFs(mSDCardDirectory.getAbsolutePath());
		long blocks = fs.getAvailableBlocks();
		long blockSize = fs.getBlockSize();
		Log.i(TAG, "..fs.." + + blocks * blockSize / 1024. / 1024);
//		Log.i(TAG, "..save sdcard " + mSDCardDirectory.getTotalSpace());
//		Log.i(TAG, "..save sdcard wav" + mWAVDirectory.getTotalSpace());
		
		while (fs.getAvailableBlocks() * fs.getBlockSize() < mEmptySize){
			if (!deleteOldestFile(mWAVDirectory)){
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
