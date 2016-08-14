package com.android.soundrecorder.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.os.Environment;
import android.util.Log;

public class FileManager implements Runnable{
	public static final String TAG = "FileManager";
	private static FileManager mInstance = new FileManager();
	
	private String mSdcardRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
	private String mWAVrootDirPrefix = "WAV_RECODE";
	private String mWAVrootDir = mSdcardRootPath + mWAVrootDirPrefix;
	
	private ArrayList<FileNode> mFileList = new ArrayList<FileNode>();
//	private boolean mHasInit = false;
	private Thread mFileManagerThread = null;
//	private long  mSleepTime = 1 * 1000; //3min to check once;
	private long  mSleepTime = 180 * 1000; //3min to check once;
	private DiskSpaceRecyle mDiskSpaceRecyle = new DiskSpaceRecyle();
	
	public String getWAVrootDir(){
		return mWAVrootDir;
	}
	
	private FileManager(){
		init();		
	}
	public long getWAVFreeSpace(){
		return new File(mWAVrootDir).getFreeSpace();
	}
	
	public static FileManager getInstance(){
		return mInstance;
	}
	
	private void init(){
		
		File dir = new File(mWAVrootDir);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		initFileList(dir);
		
		mFileManagerThread = new Thread(this);
		mFileManagerThread.setName(TAG);
		mFileManagerThread.start();
	}
	
	private void initFileList(File dir){
		File[] files = dir.listFiles();
		
		for (File f: files){
			FileNode filenode = new FileNode(f.getAbsolutePath(), FileNode.FILE_STATE_IDLE);
			filenode.setLastModifyTime(f.lastModified());
			mFileList.add(filenode);
		}
		Collections.sort(mFileList, new SortByTime());
		
		for (int i = 0; i < mFileList.size(); i++){
			Log.i(TAG, ".i " + mFileList.get(i));
		}
	}
	
	/*
	 * sort by time.
	 */
	private class SortByTime implements Comparator<FileNode>{
		@Override
		public int compare(FileNode arg0, FileNode arg1) {
			return ((arg0.getLastModifyTime() - arg1.getLastModifyTime()) > 0L) ? 1 : -1 ;
		}
	}
	
	public boolean addFileNode(File newfile){
		if (!newfile.exists()){
			return false;
		}
		FileNode filenode = new FileNode(newfile.getAbsolutePath(), FileNode.FILE_STATE_IDLE);
		filenode.setLastModifyTime(newfile.lastModified());
		mFileList.add(filenode); 
		Collections.sort(mFileList, new SortByTime());
		return true;
	}
	
	private int mFilePos = 0;
	public FileNode getOldestFile(){
		if (mFileList.size() > 0){
			mFilePos = 0;
			return mFileList.get(0);
		}
		return null;
	}
	
	public FileNode getNewestFile(){
		if (mFileList.size() > 0){
			mFilePos = mFileList.size() - 1;
			return mFileList.get(mFilePos);
		}
		return null;
	}
	
	public FileNode getNextFile(){
		if (mFilePos + 1 < mFileList.size()){
			return mFileList.get(++mFilePos);
		}
		return getOldestFile();
	}
	
	public FileNode getPreFile(){
		if (mFilePos - 1 > 0){
			return mFileList.get(--mFilePos);
		}
		return getNewestFile();
	}
	
	@Override
	public void run() {
		while (true){
			if (mDiskSpaceRecyle.isDiskLowSpace()){
				mDiskSpaceRecyle.deleteOldestFile(mFileList);
			}
			
			try {
				Thread.sleep(mSleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
		
	
}
