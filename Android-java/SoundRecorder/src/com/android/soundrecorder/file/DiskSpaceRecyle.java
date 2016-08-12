package com.android.soundrecorder.file;

import java.util.ArrayList;

import android.util.Log;

public class DiskSpaceRecyle {

	public DiskSpaceRecyle() {
	}

	public static final String TAG = "zxw";

	// private long mEmptySize = 6024678656L; // 500M
	private long mReserveSpace = 500 * 1024 * 1024L; // 500M

	public long getReserveSpace() {
		return mReserveSpace;
	}

	public boolean setReserveSpace(long size) {
		if (size > FileManager.getInstance().getWAVFreeSpace() || size < 0L) {
			return false;
		}
		mReserveSpace = size;
		return true;
	}

	public boolean isDiskLowSpace() {
//		 Log.i(TAG, ".avail " + FileManager.getInstance().getWAVFreeSpace()
//		 + "..mReserveSpace " + mReserveSpace);
		return (FileManager.getInstance().getWAVFreeSpace() < mReserveSpace);
	}

	public boolean deleteOldestFile(ArrayList<FileNode> list) {
		if (list.size() == 0) {
			return false;
		}
		FileNode node = list.get(0);
		if (node.getState() == FileNode.FILE_STATE_IDLE){
			Log.i(TAG, "delteOlddestFile" + node);
			node.delete();
			list.remove(0); // just dele the first one.
		}else {
			Log.i(TAG, "delteOlddestFile false as file is using :" + node);
		}
	
		return true;
	}
}
