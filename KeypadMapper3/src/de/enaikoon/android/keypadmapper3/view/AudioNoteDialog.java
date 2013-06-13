package de.enaikoon.android.keypadmapper3.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.utils.WavUtil;
import de.enaikoon.android.library.resources.locale.Localizer;

public class AudioNoteDialog extends Dialog implements OnClickListener {
    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();
    
    private TextView textTime;
    private ImageButton btnRecord;
    private ImageButton btnPlay;
    private AudioRecord recorder;
    private MediaPlayer mplayer;

    private static final int MAX_RECORD_TIME = 30000; // 30 seconds
    private volatile boolean paused;
    private volatile boolean recording;
    private volatile boolean recordingSuccess;
    
    private FileOutputStream fos;
    
    private Context context;
    private String filename;
    private String actualFilename;
    private Location location;
    
    private String TAG = "Keypad";

    private class CounterThread extends Thread {
        public static final int MSG_RECORD_TIME = 0;
        public static final int MSG_PLAY_TIME = 1;
        
        private boolean working;
        
        @Override
        public void run() {
            working = true;
            
            while (working) {
                if (handler == null) {
                    return;
                }
                
                if (recording) {
                    handler.sendEmptyMessage(MSG_RECORD_TIME);
                    try {
                        Thread.sleep(150);
                    } catch (Exception e) {}
                } else if (paused && !recording) {
                    try {
                        Thread.sleep(200);
                    } catch (Exception e) {}
                } else if (!paused && !recording) {
                    handler.sendEmptyMessage(MSG_PLAY_TIME);
                    try {
                        Thread.sleep(150);
                    } catch (Exception e) {}
                }
            }
        }
        
        public void stopWorking() {
            working = false;
        }
    }
    
    private long startRecordingTime;
    
    private CounterThread tmCounter;
    private final static int MSG_FAILED_RECORD = 2;
    
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == CounterThread.MSG_PLAY_TIME) {
                try {
                    textTime.setText(getTimeString(mplayer.getCurrentPosition()));
                } catch (Exception ignored) {
                    // do nothing
                }
            } else if (msg.what == CounterThread.MSG_RECORD_TIME) {
                textTime.setText(getTimeString(System.currentTimeMillis() - startRecordingTime));
                if (System.currentTimeMillis() - startRecordingTime > MAX_RECORD_TIME) {
                    // stop recording
                    onClick(btnRecord);
                }
            } else if (msg.what == MSG_FAILED_RECORD) {
                btnRecord.setImageDrawable(localizer.getDrawable("audio_record"));
                btnPlay.setImageDrawable(localizer.getDrawable("audio_play_disabled"));
            }
        };     
    };
    
    public AudioNoteDialog(Context c, AudioRecord r, MediaPlayer m, String tempF, String actual, Location loc) {
        super(c, R.style.CustomDialogTheme);
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.audio_dialog);
        
        context = c;
        recorder = r;
        mplayer = m;
        filename = tempF;
        actualFilename = actual;
        location = loc;
        
        textTime = (TextView) findViewById(R.id.textTime);
        textTime.setText(getTimeString(0));
        btnRecord = (ImageButton) findViewById(R.id.buttonRecord);
        btnRecord.setImageDrawable(localizer.getDrawable("audio_stop_recording"));
        btnRecord.setOnClickListener(this);
        
        btnPlay = (ImageButton) findViewById(R.id.buttonPlay);
        btnPlay.setImageDrawable(localizer.getDrawable("audio_play_disabled"));
        btnPlay.setOnClickListener(this);
        
        recordNote();
    }
    
    @Override
    public void onBackPressed() {
        cleanup();
        dismiss();
    }

    @Override
    public void onClick(View v) {
        if (v == btnRecord) {
            if (recording) {
                btnRecord.setImageDrawable(localizer.getDrawable("audio_record"));
                btnPlay.setImageDrawable(localizer.getDrawable("audio_play"));
                stopRecording();
            } else {
                btnRecord.setImageDrawable(localizer.getDrawable("audio_stop_recording"));
                btnPlay.setImageDrawable(localizer.getDrawable("audio_play_disabled"));
                recordNote();
            }
        } else if (v == btnPlay) {
            if (recording || recordingSuccess == false) {
                return;
            }
            if (!paused) {
                if (mplayer.isPlaying()) {
                    paused = true;
                    mplayer.pause();
                    btnPlay.setImageDrawable(localizer.getDrawable("audio_play"));
                    return;
                }
                
                btnPlay.setImageDrawable(localizer.getDrawable("audio_pause"));
                playNote();
            } else {
                paused = false;
                mplayer.start();
                btnPlay.setImageDrawable(localizer.getDrawable("audio_pause"));
            }
        } else {
            cleanup();
            dismiss();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        cleanup();
    }
    
    private String getTimeString(long ms) {
        long hundrets = (int) ms % 1000;
        int seconds = (int) ms / 1000;

        seconds = seconds % 60;

        return String.format("%02d:%03d", seconds, hundrets);
    }
    
    private void cleanup() {
        handler = null;
        if (tmCounter != null) {
            tmCounter.stopWorking();
            tmCounter = null;
        }
        
        if (recording) {
            stopRecording();
        } else {
            try {
                if (mplayer.isPlaying()) {
                    mplayer.stop();
                }
            } catch (Exception ignored) {}
        }
    }
    
    @Override
    public void onDetachedFromWindow() {
        cleanup();
        super.onDetachedFromWindow();
    }

    private void recordNote() {
        final File recordFile = new File(filename);
        try {
            if (recordFile.exists()) {
                recordFile.delete();
            }
            
            recordingSuccess = false;
            fos = new FileOutputStream(recordFile);
            
            recorder.startRecording();
            
            recording = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeRawData();
                }
            }).start();
            
            startRecordingTime = System.currentTimeMillis();
            tmCounter = new CounterThread();
            tmCounter.start();
            
        } catch(Exception e) {
            recording = false;
            recordingSuccess = false;
            try {
                recorder.stop();
            } catch (Exception ignored) {}
            recordFile.delete();
            if (tmCounter != null) {
                tmCounter.stopWorking();
                tmCounter = null;
            }
            
            paused = false;
            btnRecord.setImageDrawable(localizer.getDrawable("audio_record"));
            btnPlay.setImageDrawable(localizer.getDrawable("audio_play_disabled"));
            Log.e(TAG, "failed to record: ", e);
        }
    }
    
    private void stopRecording() {
        if (recording) {
            if (tmCounter != null) {
                tmCounter.stopWorking();
                tmCounter = null;
            }
            recording = false;
            paused = false;
            try {
                recorder.stop();
            } catch(Exception ignored) {}
        }
    }
    
    private void playNote() {
        if (recording) {
            Toast.makeText(context, "Still recording", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            // set media volume to max
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
                                         audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            FileInputStream fis = null;
            fis = new FileInputStream(new File(actualFilename));
            
            mplayer.reset();
            mplayer.setDataSource(fis.getFD());
            mplayer.prepare();
            mplayer.start();
            mplayer.setOnCompletionListener(new OnCompletionListener() {
                
                @Override
                public void onCompletion(MediaPlayer mp) {
                    paused = false;
                    btnPlay.setImageDrawable(localizer.getDrawable("audio_play"));
                    if (tmCounter != null) {
                        tmCounter.stopWorking();
                        tmCounter = null;
                    }
                }
            });
            tmCounter = new CounterThread();
            tmCounter.start();

            paused = false;
        } catch(Exception e) {
            Log.e(TAG, "failed to play: ", e);
            // show error
            Toast.makeText(context, "Failed to play", Toast.LENGTH_LONG).show();
            if (tmCounter != null) {
                tmCounter.stopWorking();
                tmCounter = null;
            }
        }
    }

    private void writeRawData() {
        File recordFile = new File(filename);
        int read = 0;
        byte [] buffer = new byte [WavUtil.getBufferSize()];
        try {
            while (recording) {
                read = recorder.read(buffer, 0, WavUtil.getBufferSize());
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    fos.write(buffer);
                }
            }
            fos.flush();
            fos.close();
            Log.d(TAG, "Wrote raw data file!");
            
            generateWavFile();
        } catch (Exception e) {
            Log.e(TAG, "Error writing raw data", e);
            try {
                fos.close();
            } catch (Exception ignored) {}
            
            recording = false;
            recordingSuccess = false;
            try {
                if (recorder != null)
                    recorder.stop();
            } catch (Exception ignored) {}
            
            recordFile.delete();
            if (tmCounter != null) {
                tmCounter.stopWorking();
                tmCounter = null;
            }
            
            paused = false;
            // update gui
            if (handler != null) {
                handler.sendEmptyMessage(MSG_FAILED_RECORD);
            }
        }
    }
    
    private void generateWavFile() {
        File infile = null;
        File outfile = null;
        try {
            infile = new File(filename);
            outfile = new File(actualFilename);
            
            FileInputStream fis = new FileInputStream(infile);
            FileOutputStream out = new FileOutputStream(outfile);
            long totalAudioLen = fis.getChannel().size();
            long totalDataLen = totalAudioLen + 36;
            out.write(WavUtil.getWavHeader(totalAudioLen, totalDataLen));
            
            byte [] buffer = new byte[ WavUtil.getBufferSize() ];
            while (fis.read(buffer) != -1) {
                out.write(buffer);
            }
            out.flush();
            out.close();
            fis.close();
            Log.d(TAG, "generated wave file!");
            infile.delete();
            recordingSuccess = true;
            // add trackpoint
            String fileUri = "file:///" + actualFilename.substring(actualFilename.lastIndexOf('/') + 1);
            KeypadMapperApplication.getInstance().getMapper().addWavTrackpoint(location, fileUri);
        } catch (Exception e) {
            recordingSuccess = false;
            if (handler != null) {
                handler.sendEmptyMessage(MSG_FAILED_RECORD);
            }
            infile.delete();
            outfile.delete();
            Log.e(TAG, "", e);
        }
    }
}
