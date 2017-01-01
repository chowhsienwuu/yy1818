package com.encryption.soundrecorder.file;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class FileManager implements Runnable{
	public static final String TAG = "FileManager";
	private static FileManager mInstance = new FileManager();
	
	//private String mSdcardRootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
	private String mSdcardRootPath = "/sdcard" + "/";
	private String mWAVrootDirPrefix = "/";
	private String mWAVrootDir = mSdcardRootPath + mWAVrootDirPrefix;
	
	private ArrayList<FileNode> mFileList = new ArrayList<FileNode>();
//	private boolean mHasInit = false;
	private Thread mFileManagerThread = null;
//	private long  mSleepTime = 1 * 1000; //3min to check once;
	private long  mSleepTime = 180 * 1000; //3min to check once;
	private DiskSpaceRecyle mDiskSpaceRecyle = new DiskSpaceRecyle();

	private FileManager(){
		init();		
	}
	public long getWAVFreeSpace(){
		return new File(mWAVrootDir).getFreeSpace();
	}
	
	public static FileManager getInstance(){
		return mInstance;
	}
	public String getWAVrootDirPath(){
		return mWAVrootDir;
	}
	public File getWAVrootDir(){
		return new File(mWAVrootDir);
	}
	public String genNewRecodFileName(){
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss"); 
		String fileName = formatter.format(date) + ".wav";
		return fileName;
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
		
		for (File f : files) {
			if (!f.isDirectory() && f.isFile()) {
				FileNode filenode = new FileNode(f.getAbsolutePath(),
						FileNode.FILE_STATE_IDLE);
				filenode.setLastModifyTime(f.lastModified());
				mFileList.add(filenode);
			}
		}
		Collections.sort(mFileList, new SortByTime());
		
		for (int i = 0; i < mFileList.size(); i++){
			Log.i(TAG, ".i " + mFileList.get(i));
		}
	}
	
	public void addWavRootMTP(Context context){
		if (context == null){
			return;
		}
		Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(mWAVrootDir);
		scanIntent.setData(Uri.fromFile(f));
		context.sendBroadcast(scanIntent);
	}
	public void addWavFileMTP(Context context, File f){
		if (context == null){
			return;
		}
		Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		scanIntent.setData(Uri.fromFile(f));
		context.sendBroadcast(scanIntent);
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
	
	public boolean delFile(String filePath){
		for (int i = 0; i < mFileList.size(); i++){
			if (mFileList.get(i).getFilePath().equals(filePath)){
				//check file status ?. and del it 
				mFileList.get(i).delete();
				mFileList.remove(i);
				return true;
			}
		}
		
		return false;
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
				e.printStackTrace();
			}
		}
	}
		
	
}
