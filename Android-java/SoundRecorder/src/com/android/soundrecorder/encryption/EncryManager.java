package com.android.soundrecorder.encryption;

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

        File file_in = new File("d:\\203.mp4");
        File file_out = new File("d:\\203.encry.mp4");
        File file_de  = new File("d:\\203.de.mp4");
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
        em.encryptionFile(file_in, file_out, 44);
        d("time002:" + new Date());
        em.decryptionFile(file_out, file_de, 44);
        d("time003:" + new Date());
    }

    /*
     * i don encryption some byte len of head.
     */
    public boolean encryptionFile(File file_in, File file_out, int noencrypheadlean) {
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

        byte[] buffer_in = new byte[4096 * 10];
        byte[] buffer_head = new byte[noencrypheadlean];
        
        int len = 0;
        try {
        	//the head.
        	len = fis.read(buffer_head);
        	fos.write(buffer_head, 0, len);
        	
            while ((len = fis.read(buffer_in)) > 0) {
            	encryptionbyte(buffer_in, len);
                fos.write(buffer_in, 0, len);
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
            resetPos(); //reset the pos, cause encryp the next file.
        }

        return true;
    }

    private boolean decryptionFile(File file_in, File file_out, int noencrypheadlean) {
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
//        byte[] test = new byte[0];
        byte[] buffer_in = new byte[1024];
        byte[] buffer_head = new byte[noencrypheadlean];
        int len = 0;
        try {
        	//the head.
        	len = fis.read(buffer_head);
        	fos.write(buffer_head, 0, len);
        	
            while ((len = fis.read(buffer_in)) > 0) {
            	decryptionbyte(buffer_in, len);
                fos.write(buffer_in, 0, len);
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
	private int mPos = 0;
	private int mEnCryLen = 40960;
	
	public EncryManager(String passwd) {
		if (passwd == null){
			passwd = "";
		}
		mEnCryptionData = passwd2sha512(passwd);
		for (int i = 0; i < mEnCryLen; i++){
//			mEnCryptionBigBuffer[i] = 101;
			mEnCryptionBigBuffer[i] = mEnCryptionData[i % 64];
		}
	}
    
	public void resetPos(){
		mPos = 0;
	}
	
	public byte[] encryptionbyte(byte[] inout, int len){
		len = len > mEnCryLen ? mEnCryLen : len;
		int i = 0;
		for (i = 0; i < len; i++){
//			d("..mPos is " + mPos + ".i." + i + ". len :" + len);
//			d("this (i + mPos) % mEnCryLen is " + ((i + mPos) % mEnCryLen));
			if (i % 2 == 0){ 
				inout[i] = (byte) (inout[i] + mEnCryptionBigBuffer[(i + mPos) % mEnCryLen]);
			}else {
				inout[i] = (byte) (inout[i] - mEnCryptionBigBuffer[(i + mPos) % mEnCryLen]);
			}
		}
		
		//shift the window pos
		mPos += len;
		mPos %= mEnCryLen;
		
		return inout;
	}
	
	public byte[] decryptionbyte(byte[] inout, int len){
		len = len > mEnCryLen ? mEnCryLen : len;
		int i = 0;
		for (i = 0; i < len; i++){
//			d("..mPos is " + mPos + ".i." + i + ". len :" + len);
//			d("this (i + mPos) % mEnCryLen is " + ((i + mPos) % mEnCryLen));
			if (i % 2 == 0){ 
				inout[i] = (byte) (inout[i] - mEnCryptionBigBuffer[i]);
			}else {
				inout[i] = (byte) (inout[i] + mEnCryptionBigBuffer[i]);
			}
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

//         for (int i = 0; i < thedigest.length; i++) {
//        	 d(" " + i + ":" + thedigest[i]);
//         }
        BigInteger bigInt = new BigInteger(1, thedigest);
        String hashtext = bigInt.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        // d("..sha: " + hashtext);

        return thedigest;
    }
}
