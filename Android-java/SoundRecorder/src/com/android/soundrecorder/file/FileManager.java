package com.android.soundrecorder.file;

import java.io.File;
import java.util.ArrayList;

import android.os.Environment;

public class FileManager {
	public static final String TAG = "FileManager";
	private static FileManager mInstance = new FileManager();
	
	private String mSdcardRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	private String mWAVrootDirPrefix = "WAV_RECODE";
	private String mWAVrootDir = mSdcardRootPath + mWAVrootDirPrefix;
	
	private ArrayList<FileNode> mFileList = new ArrayList<FileNode>();
			
	private FileManager(){
	}
	
	public static FileManager getInstance(){
		return mInstance;
	}
	
	private void init(){
		File dir = new File(mWAVrootDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		
		
	}
	
	
}
