package com.encryption.soundrecorder.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.util.Log;

public class Fm1388Util {
	private static final boolean DEBUG = false;
	private static final String TAG = "Fm1388Util";
	
	public static final String FM1388_MODE_PATH = "/sys/class/fm1388debug/mode";
	
    public static String  readSysfs(String path) {
		
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return null; 
        }

        String str = null;
        StringBuilder value = new StringBuilder();
        
        if(DEBUG)
        	Log.i(TAG, "readSysfs path:" + path);
        
        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null)
                        value.append(str);
                };
				fr.close();
				br.close();
                if(value != null)
                    return value.toString();
                else 
                    return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean writeSysfs(String path, String value) {
        if(DEBUG)
        	Log.i(TAG, "writeSysfs path:" + path + " value:" + value);
        
        if (!new File(path).exists()) {
        	Log.e(TAG, "File not found: " + path);
            return false; 
        }
        
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path), 64);
            try {
                writer.write(value);
            }catch(IOException e){
            	
            }
            finally {
                writer.close();
            }           
            return true;
                
        } catch (IOException e) { 
        	Log.e(TAG, "IO Exception when write: " + path, e);
            return false;
        }                 
    }
    
    
	public static void changeModeVr() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String tCrrentMode = readSysfs(FM1388_MODE_PATH);
				if (tCrrentMode == null || tCrrentMode.equals("0")) {
					return;
				}
				writeSysfs(FM1388_MODE_PATH, "0");
			}
		}).start();
	}

	public static void changeModeMP() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String tCrrentMode = readSysfs(FM1388_MODE_PATH);
				if (tCrrentMode == null || tCrrentMode.equals("1")) {
					return;
				}
				writeSysfs(FM1388_MODE_PATH, "1");
			}
		}).start();
	}
}
