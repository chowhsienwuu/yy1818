package com.android.soundrecorder.file;

public class FileNode {
	public final static String TAG = "FileNode";
	private String mFilePath = "";
	
	private int FILE_STATE_IDLE = 0x0;
	private int FILE_STATE_RECODING = 0x1;
	private int FILE_STATE_PLAYING =  0x2;
	
	private int mState = FILE_STATE_IDLE;
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
	
}
