package de.enaikoon.android.keypadmapper3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;

import org.osm.keypadmapper2.KeypadMapper2Activity;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.enaikoon.android.inviu.opencellidlibrary.CellIDCollectionService;
import de.enaikoon.android.inviu.opencellidlibrary.CellIDCollectionService.LocalBinder;
import de.enaikoon.android.inviu.opencellidlibrary.Configurator;
import de.enaikoon.android.inviu.opencellidlibrary.UploadService;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.WavUtil;
import de.enaikoon.android.keypadmapper3.writers.GpxWriter;
import de.enaikoon.android.keypadmapper3.writers.OsmWriter;
import de.enaikoon.android.library.resources.locale.Localizer;
import de.enaikoon.android.library.resources.locale.Localizer.LocaleProvider;

public class KeypadMapperApplication extends Application {

    public static KeypadMapperApplication getInstance() {
        return instance;
    }

    private Localizer localizer;

    private String cachedStreetFromNominatin;

    private String cachedPostCodeFromNominatin;

    private String osmInfoName = "";

    private LocationProvider locationProvider;

    private static KeypadMapperApplication instance;

    private File lastPhotoFile;

    private KeypadMapperSettings settings;

    private boolean extendedEditorEnabled = false;

    private Mapper mapper;

    private String screenToActivate = null;
    
    private CellIDCollectionService cellIdService;
    
    private String TAG = "KeypadMapper";
    
    private ServiceConnection cellidServiceConn;
    
    public FileFilter onlyFilesFilter = null;
    
    private boolean testVersion;
    
    private AudioRecord ar = null;
    private final int RECORDING_NOTIF_ID = 1;
    
    public String getCachedPostCodeFromNominatin() {
        return cachedPostCodeFromNominatin;
    }

    public String getCachedStreetFromNominatin() {
        return cachedStreetFromNominatin;
    }

    public File getKeypadMapperDirectory() {
        File extStorage = Environment.getExternalStorageDirectory();
        File kpmFolder =
                new File(extStorage.getAbsolutePath() + File.separatorChar
                        + localizer.getString("app_name"));
        return kpmFolder;
    }

    public File getLastPhotoFile() {
        return lastPhotoFile;
    }

    public Localizer getLocalizer() {
        return localizer;
    }

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    public Mapper getMapper() {
        return mapper;
    }

    public String getScreenToActivate() {
        return screenToActivate;
    }

    public KeypadMapperSettings getSettings() {
        return settings;
    }

    public boolean isExtendedEditorEnabled() {
        return extendedEditorEnabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // TODO: set this to false on production build
        testVersion = false;

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));
        
        onlyFilesFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isDirectory()
                        && (file.getName().endsWith(".gpx") || file.getName().endsWith(".osm") 
                                || file.getName().endsWith(".jpg")
                                || file.getName().endsWith(".wav"));
            }
        };
        
        settings = new KeypadMapperSettings(getApplicationContext());

        localizer = new Localizer(getApplicationContext(), "lang_support_codes");
        localizer.setLocaleProvider(new LocaleProvider() {

            @Override
            public String getLocale() {
                return settings.getCurrentLanguageCode();
            }
        });
        
        // final LogConfigurator logConfigurator = new LogConfigurator();
        //
        // logConfigurator.setFileName(getKeypadMapperDirectory() +
        // File.separator
        // + "keypadmapper.log");
        // logConfigurator.setRootLevel(Level.DEBUG);
        // // Set log level of a specific logger
        // logConfigurator.setLevel("org.apache", Level.ERROR);
        // logConfigurator.configure();

        locationProvider = new LocationProvider(getApplicationContext());
        
        mapper = new Mapper(this);
        // check if last GPX file is terminated properly
        if (settings.getLastGpxFile() != null) {
            checkLastGpxFile();
        }
        if (settings.getLastOsmFile() != null) {
            checkLastOsmFile();
        }

        if (settings.getLaunchCount() == 0) {
            settings.setTurnOffUpdates(true);
        }
        
        // starting cellid library 
        Log.d(TAG, "Starting OpenCellID service");
        Configurator.setPRODUCTION_VERSION(true); // show log
        Configurator.setGpsTimeout(300*1000); // 300 seconds
        Configurator.setAutomaticUpload(true);
        Configurator.setMinSignalLevelDifference(2);
        Configurator.setMinTimestampDifference(5000); // 5 seconds
        Configurator.setMinDistance(5);

        Configurator.setMaxLogSize(5);
        Configurator.setMaxDatabaseSize(50);
        
        Configurator.setSDCARD_DIRECTORY_NAME(KeypadMapperApplication.getInstance()
                .getKeypadMapperDirectory().getAbsolutePath()
                + "/" + "opencellid/");
        Intent startServiceIntent = new Intent(this, CellIDCollectionService.class);
        startService(startServiceIntent);
        
        cellidServiceConn = new ServiceConnection () {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocalBinder localbinder = (LocalBinder) service;
                cellIdService = localbinder.getService();
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                cellIdService = null;
            } 
        };

        bindService(startServiceIntent, cellidServiceConn, Service.BIND_AUTO_CREATE);
        
        Intent uploadService = new Intent(this, UploadService.class);
        startService(uploadService);
        
        detectCompass();
        
        if (settings.isRecording()) {
            showNotification();
        }
    }
    
    public void setCachedPostCodeFromNominatin(String cachedPostCodeFromNominatin) {
        this.cachedPostCodeFromNominatin = cachedPostCodeFromNominatin;
    }

    public void setCachedStreetFromNominatin(String cachedStreetFromNominatin) {
        this.cachedStreetFromNominatin = cachedStreetFromNominatin;
    }

    public void setExtendedEditorEnabled(boolean extendedEditorEnabled) {
        this.extendedEditorEnabled = extendedEditorEnabled;
    }

    public void setLastPhotoFile(File lastPhotoFile) {
        this.lastPhotoFile = lastPhotoFile;
    }

    public void setScreenToActivate(String screenToActivate) {
        this.screenToActivate = screenToActivate;
    }

    public void stopGpsRecording () {
        settings.setRecording(false);
        if (settings.isTurnOffUpdates()) {
            locationProvider.stopRequestingUpdates();
            // switch to keypad if KeypadMapper2Activity is front
            switchToKeypad();
        }
        mapper.stopRecording();
        clearNotification();

    }
    
    public void startGpsRecording () {
        settings.setRecording(true);
        locationProvider.startRequestingUpdates();
        mapper.startRecording();
        showNotification();
    }
    
    public boolean isGpsRecording () {
        return settings.isRecording();
    }
    
    public CellIDCollectionService getCellIdService() {
        return cellIdService;
    }
  
    public boolean isAnyDataAvailable() {
        File kpmFolder = KeypadMapperApplication.getInstance().getKeypadMapperDirectory();
        File[] filePaths = kpmFolder.listFiles(onlyFilesFilter);
        
        if (filePaths == null || filePaths.length == 0) {
            return false;
        }
        
        int count = 0;
        for (File file : filePaths) {
            if (file.getAbsolutePath().endsWith(".gpx") && file.length() <= GpxWriter.getEmptyFileSize()) {
                // skip it
                continue;
            } else if (file.getAbsolutePath().endsWith(".osm") && file.length() <= OsmWriter.getEmptyFileSize()) {
                // skip it
                continue;
            }
            count++;
        }
        return (count > 0) ? true : false;
    }
    
    public FileFilter getFileFilter() {
        return onlyFilesFilter;
    }
    
    public void checkLastGpxFile() {
        String lastGpxFile = settings.getLastGpxFile();
        if (lastGpxFile != null) {
            File file = new File(lastGpxFile);
            if (!file.exists())
                return;
            File destFile = new File(lastGpxFile + "~");

            BufferedReader br = null;
            BufferedWriter bw = null;
            try {
                boolean gpxOpen = false;
                boolean trkOpen = false;
                boolean trkSegOpen = false;
                boolean trkPointOpen = false;
                boolean wptOpen = false;

                boolean gpxClosed = false;
                boolean trkClosed = false;
                boolean trkSegClosed = false;
                boolean trkPointClosed = false;
                boolean wptClosed = false;

                br = new BufferedReader(new FileReader(file));
                destFile.createNewFile();
                bw = new BufferedWriter(new FileWriter(destFile));

                StringBuffer temp = new StringBuffer();
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        // end of file is reached 
                        if (trkPointOpen && !trkPointClosed) {
                            // dont write string buffer, just close the file
                        }
                        
                        if (wptOpen && !wptClosed) {
                            // dont write string buffer, just close the file
                            // audio note is lost
                        }
                        
                        if (trkSegOpen && !trkSegClosed) {
                            bw.write("</trkseg>\n");
                        }

                        if (trkOpen && !trkClosed) {
                            bw.write("</trk>\n");
                        }

                        if (gpxOpen && !gpxClosed) {
                            bw.write("</gpx>");
                        }

                        break;
                    }

                    if (line.toLowerCase().contains("<gpx ")) {
                        gpxOpen = true;
                    } 

                    if (line.toLowerCase().contains("<wpt ")) {
                        wptOpen = true;
                        trkSegOpen = false;
                        trkSegClosed = false;
                        trkOpen = false;
                        trkClosed = false;
                    } else if (wptOpen && !line.toLowerCase().contains("</wpt>")) {
                        temp.append(line + "\n");
                        continue;
                    }
                    
                    if (line.toLowerCase().contains("</wpt>")) {
                        wptClosed = true;
                        temp.append(line);
                        // flush buffer to bw - it's a good trackpoint
                        bw.write(temp.toString());
                        bw.newLine();
                        temp = new StringBuffer();
                        continue;
                    }
                    
                    if (line.toLowerCase().contains("<trk>")) {
                        trkOpen = true;
                        wptOpen = false;
                        wptClosed = false;
                    }

                    if (line.toLowerCase().contains("<trkseg>")) {
                        trkSegOpen = true;
                    }

                    if (line.toLowerCase().contains("<trkpt")) {
                        trkPointOpen = true;
                        // add line to temp buffer
                        temp.append(line + "\n");
                        continue;
                    } else if (trkPointOpen && !line.toLowerCase().contains("</trkpt>")) {
                        temp.append(line + "\n");
                        continue;
                    }

                    if (line.toLowerCase().contains("</trkpt>")) {
                        trkPointClosed = true;
                        temp.append(line);
                        // flush buffer to bw - it's a good trackpoint
                        bw.write(temp.toString());
                        bw.newLine();
                        temp = new StringBuffer();
                        continue;
                    }

                    if (line.toLowerCase().contains("</trkseg>")) {
                        trkSegClosed = true;
                    }

                    if (line.toLowerCase().contains("</trk>")) {
                        trkClosed = true;
                    }

                    if (line.toLowerCase().contains("</gpx>")) {
                        gpxClosed = true;
                    }

                    bw.write(line);
                    bw.newLine();
                }
            } catch (Exception e) {
                Log.e("KeypadMapper", "problem fixing file", e);
                destFile.delete();
            } finally {
                try {
                    br.close();
                    bw.flush();
                    bw.close();
                    String tmp = file.getAbsolutePath();
                    file.delete();
                    destFile.renameTo(new File(tmp));
                } catch (Exception ignored) {
                    Log.e("KeypadMapper", "error finishing", ignored);
                }
            }
        }
    }
    
    public void checkLastOsmFile() {
        String lastOsmFile = settings.getLastOsmFile();
        if (lastOsmFile != null) {
            File file = new File(lastOsmFile);
            if (!file.exists())
                return;
            File destFile = new File(lastOsmFile + "~");

            BufferedReader br = null;
            BufferedWriter bw = null;
            try {
                boolean osmOpen = false;
                boolean nodeOpen = false;
                
                boolean osmClosed = false;
                boolean nodeClosed = false;
                
                br = new BufferedReader(new FileReader(file));
                destFile.createNewFile();
                bw = new BufferedWriter(new FileWriter(destFile));

                StringBuffer temp = new StringBuffer();
                while (true) {
                    String line = br.readLine();
                    if (line == null) {
                        // end of file is reached 
                        if (nodeOpen && !nodeClosed) {
                            // dont write string buffer, just close the file
                        }
                        
                        if (osmOpen && !osmClosed) {
                            bw.write("</osm>\n");
                        }

                        break;
                    }

                    if (line.toLowerCase().contains("<osm ")) {
                        osmOpen = true;
                    } 

                    if (line.toLowerCase().contains("<node ")) {
                        nodeOpen = true;
                        // add line to temp buffer
                        temp.append(line + "\n");
                        continue;
                    } else if (nodeOpen && !line.toLowerCase().contains("</node>")) {
                        temp.append(line + "\n");
                        continue;
                    }

                    if (line.toLowerCase().contains("</node>")) {
                        nodeClosed = true;
                        temp.append(line);
                        // flush buffer
                        bw.write(temp.toString());
                        bw.newLine();
                        temp = new StringBuffer();
                        continue;
                    }

                    if (line.toLowerCase().contains("</osm>")) {
                        osmClosed = true;
                    }

                    bw.write(line);
                    bw.newLine();
                }
            } catch (Exception e) {
                Log.e("KeypadMapper", "problem fixing file", e);
                destFile.delete();
            } finally {
                try {
                    br.close();
                    bw.flush();
                    bw.close();
                    String tmp = file.getAbsolutePath();
                    file.delete();
                    destFile.renameTo(new File(tmp));
                } catch (Exception ignored) {
                    Log.e("KeypadMapper", "error finishing", ignored);
                }
            }
        }
    }
    
    public boolean isTestVersion() {
        return testVersion;
    }
    
    public AudioRecord getAudioRecorder() {
        if (ar == null) {
            setRecorder();
        }
        return ar;
    }
    
    public void setRecorder() {
        ar = WavUtil.getRecorder();
    }
    
    public void releaseRecorder() {
        if (ar != null) {
            ar.release();
            ar = null;
        }
    }
    
    private void detectCompass() {
        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        
        if (sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null 
                || sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null) {
            Log.d("KeypadMapper", "No accelerometer or magnetic field sensors avaialble");
            settings.setCompassAvailable(false);
        } else {
            settings.setCompassAvailable(true);
        }
    }
    
    public void showNotification() {
        Intent reactivate = new Intent();
        reactivate.setClass(getApplicationContext(), KeypadMapper2Activity.class);
        reactivate.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, reactivate, PendingIntent.FLAG_UPDATE_CURRENT);
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon_keypad_notification)
                .setContentIntent(contentIntent)
                .setContentTitle(localizer.getString("app_name"))
                .setContentText(localizer.getString("notification_title"));
        
        Notification notif = mBuilder.build();
        notif.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        
        NotificationManager myNm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        myNm.notify(RECORDING_NOTIF_ID, notif);
    }
    
    public void clearNotification() {
        NotificationManager myNm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        myNm.cancel(RECORDING_NOTIF_ID);

    }
    
    /**
     * Only to be called when recording is turned off.
     */
    private void switchToKeypad() {
        if (KeypadMapper2Activity.getInstance() != null) {
            KeypadMapper2Activity.getInstance().showKeypad();
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean systemHasVibrator() {
        if (Build.VERSION.SDK_INT >= 11) {
            Vibrator vibService = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            return vibService.hasVibrator();
        }
        // assume true - it gets ignored on phones which don't have a vibrator
        return true;
    }
}
