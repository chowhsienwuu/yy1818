package com.android.soundrecorder.file;

import java.io.File;

public class FileNode {
	public final static String TAG = "FileNode";
	private String mFilePath = "";
	
	public static final int FILE_STATE_IDLE = 0x0;
	public static final int FILE_STATE_RECODING = 0x1;
	public static final int FILE_STATE_PLAYING =  0x2;
	
	public int mState = FILE_STATE_IDLE;
	private long mLastModifyTime = 0L;
	
	public long getLastModifyTime() {
		return mLastModifyTime;
	}
	public void setLastModifyTime(long mLastModifyTime) {
		this.mLastModifyTime = mLastModifyTime;
	}
	public FileNode(){		
	}
	public FileNode(String filepath){
		mFilePath = filepath;
	}
	public FileNode(String filString, int filesttue){
		mFilePath = filString;
		mState = filesttue;
	}
	public boolean delete(){
		File f = new File(mFilePath);
		return f.delete();
	}
	
	public int getState() {
		return mState;
	}
	public void setState(int mState) {
		this.mState = mState;
	}
	public String getFilePath() {
		return mFilePath;
	}
	public void setFilePath(String mFilePath) {
		this.mFilePath = mFilePath;
	}
	
	@Override
	public String toString() {
		String str = "filepath: " + mFilePath + ".filestate: " + mState + ".LastModifyTime : " 
				+ mLastModifyTime + " . ";
		return str;
	}

}
