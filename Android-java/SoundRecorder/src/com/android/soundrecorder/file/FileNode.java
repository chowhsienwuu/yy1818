package com.android.soundrecorder.file;

public class FileNode {
	public final static String TAG = "FileNode";
	private String mFilePath = "";
	
	private int FILE_STATE_IDLE = 0x0;
	private int FILE_STATE_RECODING = 0x1;
	private int FILE_STATE_PLAYING =  0x2;
	
	private int mState = FILE_STATE_IDLE;
	
	public FileNode(){
		
	}
	public FileNode(String filepath){
		mFilePath = filepath;
	}
	public FileNode(String filString, int filesttue){
		mFilePath = filString;
		mState = filesttue;
	}
	
	public int getmState() {
		return mState;
	}
	public void setmState(int mState) {
		this.mState = mState;
	}
	public String getmFilePath() {
		return mFilePath;
	}
	public void setmFilePath(String mFilePath) {
		this.mFilePath = mFilePath;
	}
	
}
