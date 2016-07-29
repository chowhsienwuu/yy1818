package com.audioencryption;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

public class RecorderThread extends Thread {

	private static final String TAG = "RecorderThread";

	private AudioRecord audioRecord;
	private boolean isRecording;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int sampleRate = 44100;
	private int bufferSizeInBytes;

	public RecorderThread() {

		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate,
				channelConfiguration, audioEncoding); // need to be larger than
														// size of a frame

		Log.i(TAG, "bufferSizeInBytes=" + bufferSizeInBytes);

		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				sampleRate, channelConfiguration, audioEncoding,
				bufferSizeInBytes); // Âó¿Ë·ç
		// audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_CALL,
		// sampleRate, channelConfiguration, audioEncoding,
		// bufferSizeInBytes);
	}

	public AudioRecord getAudioRecord() {
		return audioRecord;
	}

	public boolean isRecording() {
		return this.isAlive() && isRecording;
	}

	public void startRecording() {
		try {
			audioRecord.startRecording();
			isRecording = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopRecording() {
		try {
			audioRecord.stop();
			isRecording = false;

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {

		File dir = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath(), "ivr_record");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		File recordingFile = new File(dir, "ivr_" + System.currentTimeMillis()
				+ ".wav");

		Log.i(TAG, "start recording,file=" + recordingFile.getAbsolutePath());

		OutputStream out = null;
		ByteArrayOutputStream baos = null;

		try {
			baos = new ByteArrayOutputStream();

			startRecording();

			byte[] buffer = new byte[bufferSizeInBytes];

			int bufferReadResult = 0;

			while (isRecording) {

				bufferReadResult = audioRecord.read(buffer, 0,
						bufferSizeInBytes);

				if (bufferReadResult > 0) {
					baos.write(buffer, 0, bufferReadResult);
				}
			}

			Log.i(TAG, "stop recording,file=" + recordingFile.getAbsolutePath());

			buffer = baos.toByteArray();

			Log.i(TAG, "audio byte len=" + buffer.length);

			out = new FileOutputStream(recordingFile);
			out.write(getWavHeader(buffer.length));
			out.write(buffer);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public byte[] getWavHeader(long totalAudioLen) {
		int mChannels = 1;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRate;
		long byteRate = sampleRate * 2 * mChannels;

		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) mChannels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * mChannels); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		return header;
	}
}
