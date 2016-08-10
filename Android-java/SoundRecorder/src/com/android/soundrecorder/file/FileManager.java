package com.android.soundrecorder.file;

public class FileManager {
	public static final String TAG = "FileManager";
	private static FileManager mInstance = new FileManager();
	
	private FileManager(){
	}
	
	public static FileManager getInstance(){
		return mInstance;
	}
	
	
	
	
}
