package com.encryption.soundrecorder.util;

public class MiscUtil {
	public static int[] sec2hms(long secTime){
		int[] ret_hms = new int[3];
		ret_hms[0] = (int)(secTime / 3600);
		ret_hms[2] = (int)(secTime % 60);
		ret_hms[1] = (int)((secTime - 3600 * ret_hms[0]) / 60);
		return ret_hms;
	}
}
