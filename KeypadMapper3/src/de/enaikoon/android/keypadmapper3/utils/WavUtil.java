package de.enaikoon.android.keypadmapper3.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class WavUtil {
    private static int ANDROID_SAMPLE_RATES [] = {22050, 16000, 11025, 44100, 8000, 48000};
    
    private static int RECORDER_SAMPLERATE = 22050;
    private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BITS_PER_SAMPLE = 16;
    private static int bufferSize = 0;
    
    public static AudioRecord getRecorder() {
        AudioRecord ar = null;
        
        // first try stereo
        RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
        for (int i = 0; i < ANDROID_SAMPLE_RATES.length; i++) {
            try {
                RECORDER_SAMPLERATE = ANDROID_SAMPLE_RATES[i];
                bufferSize = getBufferSize();
                
                //Log.d("Keypad", "Trying sample rate: " + ANDROID_SAMPLE_RATES[i] +  " Got buffer size: " + bufferSize);

                ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, RECORDER_SAMPLERATE, 
                                     RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, 
                                     bufferSize);
                
                if (ar.getState() != AudioRecord.STATE_INITIALIZED) {
                  //  Log.d("Keypad", "Audio Recorder not init! State = " + ar.getState());
                } else {
                   // Log.d("KeypadMapper", "Using sample rate: " + RECORDER_SAMPLERATE);
                    return ar;
                }
            } catch (Exception e) {
               // Log.e("KeypadMapper", "Error initializing audio", e);
            }
        }
        
        ar = null;
        Log.d("KeypadMapper", "Trying mono!");
        // set it to mono
        RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        for (int i = 0; i < ANDROID_SAMPLE_RATES.length; i++) {
            try {
                RECORDER_SAMPLERATE = ANDROID_SAMPLE_RATES[i];
                bufferSize = getBufferSize();
                
               // Log.d("Keypad", "Trying sample rate: " + ANDROID_SAMPLE_RATES[i] +  " Got buffer size: " + bufferSize + " Channels: mono");

                ar = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, RECORDER_SAMPLERATE, 
                                     RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, 
                                     bufferSize);
                
                if (ar.getState() != AudioRecord.STATE_INITIALIZED) {
                  //  Log.d("Keypad", "Audio Recorder not init! State = " + ar.getState());
                } else {
                 //   Log.d("KeypadMapper", "Using sample rate: " + RECORDER_SAMPLERATE);
                    return ar;
                }
            } catch (Exception e) {
               // Log.e("KeypadMapper", "Error initializing audio", e);
            }
        }
        
        return null;
    }
    
    public static int getBufferSize() {
        return AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }
    
    
    /**
     * @see http://www.edumobile.org/android/android-development/audio-recording-in-wav-format-in-android-programming/
     * 
     * @param totalAudioLen
     * @param totalDataLen totalAudioLen + 36
     * @return
     */
    public static byte [] getWavHeader(long totalAudioLen, long totalDataLen) {
        byte[] header = new byte[44];
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = (RECORDER_CHANNELS == AudioFormat.CHANNEL_IN_MONO) ? 1 : 2; 
        long byteRate = (RECORDER_BITS_PER_SAMPLE * RECORDER_SAMPLERATE * channels) / 8;
        
        
        header[0] = 'R';  // RIFF/WAVE header
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
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BITS_PER_SAMPLE;  // bits per sample
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
