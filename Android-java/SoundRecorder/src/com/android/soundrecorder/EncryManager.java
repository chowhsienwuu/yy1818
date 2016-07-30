package com.android.soundrecorder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.util.Log;


public class EncryManager {
    public static void main(String[] args){
        EncryManager em = new EncryManager("123456");

        File file_in = new File("/storage/sdcard0/203.jpg");
        File file_out = new File("/storage/sdcard0/203.encry.jpg");
        File file_de  = new File("/storage/sdcard0/203.de.jpg");
        d("file_in" + file_in.exists());
        d("md5sum : " + em.passwd2sha512("123456"));

        if (file_de.exists()){
            file_de.delete();
            try {
                file_de.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if (file_out.exists()){
            file_out.delete();
            try {
                file_out.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        d("time001:" + new Date());
        em.encryptionFile(file_in, file_out, em.passwd2sha512("123456"));
        d("time002:" + new Date());
        em.decryptionFile(file_out, file_de, em.passwd2sha512("123456"));
        d("time003:" + new Date());
    }


    private boolean encryptionFile(File file_in, File file_out, byte[] md5byte) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file_in);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file_out);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] buffer_in = new byte[1024];
        byte[] buffer_out = new byte[1024];
        byte[] code_buffer = new byte[1024];
        for (int j = 0; j < 1024; j++) {
            code_buffer[j] = md5byte[j % 64];
        }

        int len = 0;
        int i = 0;
        try {
            while ((len = fis.read(buffer_in)) > 0) {
                for (i = 0; i < len; i++) {
                    buffer_out[i] = (byte) (buffer_in[i] + code_buffer[i]);
                }
                fos.write(buffer_out, 0, len);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private boolean decryptionFile(File file_in, File file_out, byte[] md5byte) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file_in);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file_out);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte[] buffer_in = new byte[1024];
        byte[] buffer_out = new byte[1024];
        byte[] code_buffer = new byte[1024];
        for (int j = 0; j < 1024; j++){
            code_buffer[j] = md5byte[j % 64];
        }

        int len = 0;
        int i = 0;
        try {
            while ((len = fis.read(buffer_in)) > 0) {
                for (i = 0; i < len; i++) {
                    buffer_out[i] = (byte) (buffer_in[i] - code_buffer[i]);
                }

                fos.write(buffer_out, 0, len);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally {
            try {
                fis.close();
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }




    private static void d(String a){
        System.out.println("zxw" + a);
        Log.i("zxw", "" + a);
    }
    
    
    
    private byte[] mEnCryptionData = new byte[64];
	private byte[] mEnCryptionBigBuffer = new byte[4096 * 10]; 
	private int mPos = -1;
	private int mEnCryLen = 40960;
	
	public EncryManager(String passwd) {
		passwd = "123456";
		mEnCryptionData = passwd2sha512(passwd);
		for (int i = 0; i < mEnCryLen; i++){
			mEnCryptionBigBuffer[i] = 111;
//			mEnCryptionBigBuffer[i] = mEnCryptionData[i % 64];
		}
	}
    
	public byte[] encryptionbyte(byte[] inout){
		//for now data.
		int len = inout.length < mEnCryLen ? inout.length : mEnCryLen;
		for (mPos = 0; mPos < len; mPos++){
			inout[mPos] = (byte) (inout[mPos] + mEnCryptionBigBuffer[mPos]);
		}
		
		return inout;
	}
	
    private byte[] passwd2sha512(String passwd) {

        byte[] bytesOfMessage = null;
        try {
            bytesOfMessage = passwd.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] thedigest = md.digest(bytesOfMessage);
        // d("..the md5 is " + thedigest.length);

         for (int i = 0; i < thedigest.length; i++) {
        	 d(" " + i + ":" + thedigest[i]);
         }
        BigInteger bigInt = new BigInteger(1, thedigest);
        String hashtext = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        // d("..sha: " + hashtext);

        return thedigest;
    }
}
