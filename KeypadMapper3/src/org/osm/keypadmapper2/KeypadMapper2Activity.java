/*   Copyright (C) 2010 Nic Roets

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
 */
package org.osm.keypadmapper2;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.RateMeActivity;
import de.enaikoon.android.keypadmapper3.SatelliteInfoFragment;
import de.enaikoon.android.keypadmapper3.SettingsActivity;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.domain.NotificationListener;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.photo.CameraHelper;
import de.enaikoon.android.keypadmapper3.services.ControllableResourceInitializerService;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.view.menu.KeypadMapperMenu;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener;
import de.enaikoon.android.library.resources.locale.Localizer;

public class KeypadMapper2Activity extends FragmentActivity implements AddressInterface,
        LocationListener, MenuListener, NotificationListener, SensorEventListener {
    
    SensorManager sensorManager;
    Sensor gsensor, msensor;
    float[] mGravity;
    float[] mGeomagnetic;
    public static volatile float azimuth;
    
    private Button btnTestVersion;
    private Dialog testScreenDialog;

    private StringBuffer duplicates;
    private StringBuffer allData;
    
    private static ArrayList<Location> allLocations = new ArrayList<Location>();
    
    private class LocObj {
        public double gpsLat;
        public double gpsLong;
        public double compassLat;
        public double compassLon;
        
        public LocObj() {
            gpsLat = gpsLong = compassLat = compassLon = 0.0d;
        }
    }
    
    private static final int SB_ALL_LIMIT = 250000;
    
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(btnTestVersion.getApplicationWindowToken(), 0);

                    if (satteliteInfoVisible) {
                        showSettings();
                    } else {
                        if (getResources().getBoolean(R.bool.is_tablet)) {
                            showSatteliteInfo();
                        } else {
                            // not tablet
                            if (state == State.keypad) {
                                state = State.extended;
                                showKeypad();
                            } else {
                                showSatteliteInfo();
                            }
                        }
                    }

                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(btnTestVersion.getApplicationWindowToken(), 0);

                    if (satteliteInfoVisible) {
                        if (getResources().getBoolean(R.bool.is_tablet)) {
                            state = State.keypad;
                        } else {
                            state = State.extended;
                        }
                        showKeypad();
                    } else {
                        if (state == State.extended) {
                            state = State.keypad;
                            showKeypad();
                        } else {
                            showSettings();
                        }
                    }
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

    }

    private enum State {
        keypad, settings, extended, both
    }

    private GestureDetector gestureDetector;

    private boolean satteliteInfoVisible = false;

    private static final int SWIPE_MIN_DISTANCE = 120;

    private static final int SWIPE_MAX_OFF_PATH = 250;

    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private static final int REQUEST_GPS_ENABLE = 1;

    private static final int REQUEST_SETTINGS = 3;

    private LocationProvider locationProvider = KeypadMapperApplication.getInstance().getLocationProvider();

    private State state;

    private AlertDialog gpsDialog;

    private Mapper mapper = KeypadMapperApplication.getInstance().getMapper();

    public static final String TAG = "KeypadMapper2Activity";

    private KeypadFragment keypadFragment;

    private View keypadView;

    private View extendedAddressView;

    private View satInfoView;

    private ExtendedAddressFragment extendedAddressFragment;

    private String lang = null;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private boolean extendedAddressActive = false;

    private KeypadMapperMenu menu;

    private SatelliteInfoFragment satelliteInfo;

    private boolean uiOptimizationEnabled = false;
    
    public static final int REQUEST_CODE_NOTHING = 300;
    
    
    private static KeypadMapper2Activity instance;
    
    public static KeypadMapper2Activity getInstance() {
        return instance;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureDetector.onTouchEvent(ev)) {
            return true;
        } else if (keypadView.getVisibility() == View.VISIBLE
                && keypadFragment.dispatchTouchEvent(ev)) {
            return true;
        } else if (extendedAddressView.getVisibility() == View.VISIBLE
                && extendedAddressFragment.dispatchTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osm.keypadmapper2.AddressInterface#extendedAddressActive()
     */
    @Override
    public void extendedAddressActive() {
        extendedAddressActive = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osm.keypadmapper2.AddressInterface#extendedAddressInactive()
     */
    @Override
    public void extendedAddressInactive() {
        extendedAddressActive = false;
    }

    public void makePhotoWithLocation() {
        CameraHelper.startPhotoIntent(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.enaikoon.android.keypadmapper3.domain.NotificationListener#
     * notifyAboutFatalError(java.lang.String)
     */
    @Override
    public void notifyAboutFatalError(String text) {
        showDialogFatalError(text);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Window window = getWindow();
        window.setFormat(PixelFormat.RGBA_8888);
        window.setFlags(WindowManager.LayoutParams.FLAG_DITHER,
                WindowManager.LayoutParams.FLAG_DITHER);
        Log.d(TAG, "attached to window");
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "create");
        
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
           
            uiOptimizationEnabled = true;
        }
        
        super.onCreate(savedInstanceState);
        
        if (KeypadMapperApplication.getInstance().getSettings().isCompassAvailable()) {
            sensorManager =  (SensorManager)getSystemService(Context.SENSOR_SERVICE);
            gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        
        updateLocale(); 
        
        setContentView(R.layout.main);
        
        menu = new KeypadMapperMenu(findViewById(R.id.menu));
        menu.setMenuListener(this);

        gestureDetector = new GestureDetector(this, new MyGestureDetector());

        // check for external storage
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            // We can read and write the media
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            // We can only read the media
            showDialogFatalError(localizer.getString("errorStorageRO"));
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need to know is we can neither read nor write
            showDialogFatalError(localizer.getString("errorStorageUnavailable"));
        }

        File kpmFolder = KeypadMapperApplication.getInstance().getKeypadMapperDirectory();
        if (!kpmFolder.exists()) {
            if (!kpmFolder.mkdir()) {
                showDialogFatalError(localizer.getString("FolderCreationFailed"));
            }
        }

        if (savedInstanceState == null) {
            savedInstanceState = getIntent().getExtras();
        }

        duplicates = new StringBuffer();
        allData = new StringBuffer();

        if (savedInstanceState == null) {
            // first start
            state = State.keypad;
            
            // only on first run automatically start GPS recording
            if (KeypadMapperApplication.getInstance().getSettings().isFirstRun()) {
                KeypadMapperApplication.getInstance().getSettings().clearFirstRun();
                KeypadMapperApplication.getInstance().startGpsRecording(); // always start when app starts
            }
        } else {
            // restart
            state = State.values()[savedInstanceState.getInt("state", State.keypad.ordinal())];
            satteliteInfoVisible = savedInstanceState.getBoolean("sat_info");

            extendedAddressActive = savedInstanceState.getBoolean("extended_address");
            
            if (savedInstanceState.getBoolean("debug_dialog_on")) {
                duplicates.append(savedInstanceState.getString("duplicates"));
                allData.append(savedInstanceState.getString("allData"));
                showTestScreenDialog();
            }
        }

        keypadFragment = (KeypadFragment) getSupportFragmentManager().findFragmentByTag("keypad");
        
        Log.d("Keypad", "isTablet = " + getResources().getBoolean(R.bool.is_tablet));
        if (!getResources().getBoolean(R.bool.is_tablet)) {
            extendedAddressFragment = (ExtendedAddressFragment) getSupportFragmentManager().findFragmentByTag("extended_address");
        } else {
            extendedAddressFragment = (ExtendedAddressFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_extended_address_tablet);
        }
        
        Log.d("Keypad", "extended address fragment = " + extendedAddressFragment);

        satelliteInfo =
                (SatelliteInfoFragment) getSupportFragmentManager().findFragmentByTag("satellite");

        satInfoView = findViewById(R.id.satellite_view);
        extendedAddressView = findViewById(R.id.extended_address_view);
        keypadView = findViewById(R.id.keypad_view);
        if (keypadView == null && extendedAddressView == null) {
            state = State.both;
        }

        btnTestVersion = (Button) keypadView.findViewById(R.id.btnTestVersion);
        /*
        btnTestVersion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.setClass(KeypadMapper2Activity.this, SettingsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(i);
                
                // show debug screen with all registered addresses and locations
                //showTestScreenDialog();
            }
        });*/
        
        if (satteliteInfoVisible) {
            showSatteliteInfo();
        } else {
            showKeypad();
        }
        
        locationProvider.refreshReferenceToGps();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "destroy");
    }

    @Override
    public void onHousenumberChanged(String newHousenumber) {
        extendedAddressFragment.updateHouseNumber(newHousenumber);
        keypadFragment.updateHousenumber(newHousenumber);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && state == State.extended) {
            state = State.keypad;
            showKeypad();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK && satteliteInfoVisible) {
            state = State.keypad;
            showKeypad();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onLocationChanged(Location loc) {
        /* TODO: leave this in case of more testing needed for location filtering
        if (loc == null || !KeypadMapperApplication.getInstance().isTestVersion())
            return;
        
        
        // DEBUG DATA ONLY
        double DISTANCE = KeypadMapperApplication.getInstance().getSettings().getDistance();
        
        LocObj locObjLeft = getCurrentAddress(0, DISTANCE, loc);
        LocObj locObjForward = getCurrentAddress(DISTANCE, 0, loc);
        LocObj locObjRight = getCurrentAddress(0, -DISTANCE, loc);
        
        boolean passed = true;

        int statusCount = locationProvider.getLastFixCount();
        if (!GPSDataValidator.validateGPSData(locationProvider.getLastValidLocation(), loc, statusCount, NMEAHelper.HDOP, NMEAHelper.VDOP)) {
            passed = false;
        } else {
            locationProvider.setLastValidLocation(loc);
        }
        
        
        
        
        final StringBuffer tempBuffer = new StringBuffer();
        tempBuffer.append("----------------------------------\n");
        tempBuffer.append("Unmodified GPS coords - lat: " + loc.getLatitude() + " lon: " + loc.getLongitude() + "\n");
        tempBuffer.append("Speed m/s:" + loc.getSpeed() + " accuracy: " + loc.getAccuracy() + " altitude: " + loc.getAltitude() + "\n");
        //tempBuffer.append("Number of satellites in the fix (from Location object): " + statusCount + "\n");
        tempBuffer.append("Number of satellites in the fix (from last known GpsStatus): " + statusCount + "\n");
        tempBuffer.append("Time: " + loc.getTime() + "\n");
        /*tempBuffer.append("LEFT " + DISTANCE + "m\n");
        tempBuffer.append("Compass lat: " + locObjLeft.compassLat + " lon: " + locObjLeft.compassLon + "   GPS lat: " + locObjLeft.gpsLat + " lon: " + locObjLeft.gpsLong + "\n");
        tempBuffer.append("FORWARD " + DISTANCE + "m\n");
        tempBuffer.append("Compass lat: " + locObjForward.compassLat + " lon: " + locObjForward.compassLon + "   GPS lat: " + locObjForward.gpsLat + " lon: " + locObjForward.gpsLong + "\n");
        tempBuffer.append("RIGHT " + DISTANCE + "m\n");
        tempBuffer.append("Compass lat: " + locObjRight.compassLat + " lon: " + locObjRight.compassLon + "   GPS lat: " + locObjRight.gpsLat + " lon: " + locObjRight.gpsLong + "\n");
        */
       /* if (allData.length() > SB_ALL_LIMIT) {
            allData = new StringBuffer();
        }
        
        if (duplicates.length() > SB_ALL_LIMIT) {
            duplicates = new StringBuffer();
        }
        allData.append(tempBuffer.toString());

        // find duplicate and mark it
        /*for (Location tmp : allLocations) {
            if (tmp.getLatitude() == loc.getLatitude() && tmp.getLongitude() == loc.getLongitude()) {
                duplicates.append(tempBuffer.toString());
                break;
            }
        }*/
        /*if (!passed) {
            duplicates.append(tempBuffer.toString());
        }

        
        if (allLocations.size() > 25) {
            allLocations.remove(0);
        }
        allLocations.add(loc);*/
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * de.enaikoon.android.keypadmapper3.view.menu.MenuListener#onMenuOptionClicked
     * (de.enaikoon.android.keypadmapper3.view.menu.MenuListener.OptionType)
     */
    @Override
    public void onMenuOptionClicked(OptionType type) {
        if (type == OptionType.CAMERA) {
            makePhotoWithLocation();
        } else if (type == OptionType.GPS_INFO) {
            showSatteliteInfo();
        } else if (type == OptionType.ADDRESS_EDITOR) {
            state = State.extended;
            showKeypad();
        } else if (type == OptionType.KEYPAD) {
            state = State.keypad;
            showKeypad();
        } else if (type == OptionType.UNDO) {
            mapper.undo();
        } else if (type == OptionType.SETTINGS) {
            showSettings();
        } else if (type == OptionType.FREEZE_GPS) {
            mapper.freezeUnfreezeLocation(this);
        } else if (type == OptionType.KEYPAD) {
            state = State.keypad;
            showKeypad();
        } 
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            showDialogGpsDisabled();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER) && gpsDialog != null) {
            gpsDialog.dismiss();
            gpsDialog = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        instance = this;
        
        Log.d(TAG, "resume");

        ControllableResourceInitializerService.startResourceLoading(getApplicationContext(),
                "lang_support_codes", "lang_support_names", "lang_support_urls");
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mapper.addNotificationListener(this);
        
        if (KeypadMapperApplication.getInstance().getSettings().isCompassAvailable()) {
            sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_UI);
        }
        
        locationProvider.addLocationListener(this);
        mapper.onResume();
        menu.onResume();
        satelliteInfo.onResume();
               
        String languageToLoad =
                KeypadMapperApplication.getInstance().getSettings().getCurrentLanguageCode();
        if (!lang.equals(languageToLoad)
                || uiOptimizationEnabled != KeypadMapperApplication.getInstance().getSettings()
                        .isLayoutOptimizationEnabled()) {
            Intent localIntent = new Intent(this, KeypadMapper2Activity.class);
            Bundle data = new Bundle();
            data.putInt("state", state.ordinal());
            data.putBoolean("extended_address", extendedAddressActive);
            data.putBoolean("sat_info", satteliteInfoVisible);
            localIntent.putExtras(data);
            startActivity(localIntent);
            finish();
        } else {
            updateLocale();
        }

        if (!locationProvider.isLocationServiceEnabled()) {
            showDialogGpsDisabled();
        } else if (gpsDialog != null) {
            gpsDialog.dismiss();
            gpsDialog = null;
        }
        
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            // show empty toast so that there's no problem with showing
            // optimized view. On some phones with Android 2.3 and maybe others,
            // layout isn't rendered in fullscreen and showing toast fixes it.
            View etv = getLayoutInflater().inflate(R.layout.empty_toast_view, null);
            Toast t = new Toast(getApplicationContext());
            t.setView(etv);
            t.setDuration(1);
            t.show();
        }
        
        RateMeActivity.startRateMe(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // useless, doesn't get called when a GPS fix is available
    }

    public void showKeypad() {
        hideKeyboard();
        satteliteInfoVisible = false;
        this.satInfoView.setVisibility(View.GONE);
        if (state == State.keypad) {
            KeypadMapperApplication.getInstance().setExtendedEditorEnabled(false);
            menu.setKeypadMode(true);
            keypadView.setVisibility(View.VISIBLE);
            extendedAddressView.setVisibility(View.GONE);
            if (KeypadMapperApplication.getInstance().isTestVersion()) {
                btnTestVersion.setVisibility(View.VISIBLE);
            }
        } else {
            KeypadMapperApplication.getInstance().setExtendedEditorEnabled(true);
            menu.setKeypadMode(false);
            menu.setGpsInfoMode(false);
            extendedAddressView.setVisibility(View.VISIBLE);
            keypadView.setVisibility(View.GONE);
            btnTestVersion.setVisibility(View.GONE);
        }
        // check for GPS
        if (!locationProvider.isLocationServiceEnabled()) {
            showDialogGpsDisabled();
        } else if (gpsDialog != null) {
            gpsDialog.dismiss();
            gpsDialog = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osm.keypadmapper2.AddressInterface#showMessage()
     */
    @Override
    public void showMessage(String messageKey) {
        Toast.makeText(this, localizer.getString(messageKey), Toast.LENGTH_SHORT).show();
    }

    public void showSatteliteInfo() {
        if (!KeypadMapperApplication.getInstance().getSettings().isRecording() && 
                KeypadMapperApplication.getInstance().getSettings().isTurnOffUpdates()) {
            Toast.makeText(KeypadMapperApplication.getInstance(), localizer.getString("hint_turn_off_gps_updates"), Toast.LENGTH_LONG).show();
            Log.d("Keypad", "Shouldn't show fragment!!!!");
            state = State.keypad;
            showKeypad();
            return;
        }
        hideKeyboard();
        state = State.keypad;
        satteliteInfoVisible = true;
        this.keypadView.setVisibility(View.GONE);
        this.extendedAddressView.setVisibility(View.GONE);
        this.satInfoView.setVisibility(View.VISIBLE);
        KeypadMapperApplication.getInstance().setExtendedEditorEnabled(false);
        menu.setGpsInfoMode(true);
        // check for GPS
        if (!locationProvider.isLocationServiceEnabled()) {
            showDialogGpsDisabled();
        } else if (gpsDialog != null && gpsDialog.isShowing()) {
            gpsDialog.dismiss();
            gpsDialog = null;
        }
    }

    public void showSettings() {
        hideKeyboard();
        satteliteInfoVisible = false;
        KeypadMapperApplication.getInstance().setExtendedEditorEnabled(false);
        state = State.keypad;
        SettingsActivity.startActivityForResult(this, REQUEST_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onactivityresult req_code " + requestCode);

        boolean handled = CameraHelper.onActivityResult(this, requestCode, resultCode, data);
        if (handled) {
            return;
        }
        
        switch (requestCode) {
        case REQUEST_GPS_ENABLE:
            if (!locationProvider.isLocationServiceEnabled()) {
                showDialogGpsDisabled();
            }
            break;
        case REQUEST_SETTINGS:
            if (KeypadMapperApplication.getInstance().getScreenToActivate() != null
                    || (data != null && data.getStringExtra("option") != null)) {

                String optionTxt = KeypadMapperApplication.getInstance().getScreenToActivate();
                
                KeypadMapperApplication.getInstance().setScreenToActivate(null);
                if (optionTxt == null) {
                    optionTxt = data.getStringExtra("option");
                }
                if (optionTxt.equals("swipe_right")) {
                    showSatteliteInfo();
                } else if (optionTxt.equals("swipe_left")) {
                    state = State.keypad;
                    showKeypad();
                } else {
                    MenuListener.OptionType option = MenuListener.OptionType.valueOf(optionTxt);
                    menu.interceptClick(option);
                }
            } else {
                state = State.keypad;
                showKeypad();
            }
            break;
        case REQUEST_CODE_NOTHING:
            state = State.keypad;
            showKeypad();
            break;
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        instance = null;
        menu.onPause();
       
        mapper.removeNotificationListener(this);
        mapper.onPause();
        satelliteInfo.onPause();

        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());

        if (settings.isCompassAvailable()) {
            sensorManager.unregisterListener(this);
        }
        
        locationProvider.removeLocationListener(this);
        
        super.onPause();
        Log.d(TAG, "pause");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state", state.ordinal());
        outState.putBoolean("extended_address", extendedAddressActive);
        outState.putBoolean("sat_info", satteliteInfoVisible);
        
        outState.putBoolean("debug_dialog_on", testScreenDialog != null && testScreenDialog.isShowing());
        outState.putString("duplicates", duplicates.toString());
        outState.putString("allData", allData.toString());
        Log.d(TAG, "saveinstance");
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
                }
            } 
        }
    }
    
    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.keypadView.getWindowToken(), 0);
    }

    private void showDialogFatalError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setNegativeButton(localizer.getString("quit"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
        builder.create().show();
    }

    private void showDialogGpsDisabled() {
        if (gpsDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(localizer.getString("errorGpsDisabled"))
                    .setCancelable(false)
                    .setNegativeButton(localizer.getString("quit"),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .setPositiveButton(localizer.getString("system_settings"),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    startActivityForResult(
                                            new Intent(
                                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                            REQUEST_GPS_ENABLE);
                                    gpsDialog.dismiss();
                                    gpsDialog = null;
                                }
                            });
            gpsDialog = builder.create();
            gpsDialog.show();
        } 
    }
    
    private void showTestScreenDialog() {
        if (testScreenDialog == null) {
            testScreenDialog = new Dialog(this, R.style.CustomDialogTheme);
            testScreenDialog.setContentView(R.layout.test_screen_dialog);
            
            final TextView txtDuplicates = (TextView) testScreenDialog.findViewById(R.id.txtDuplicates);
            txtDuplicates.setText(duplicates.toString());
            
            final TextView txtAllData = (TextView) testScreenDialog.findViewById(R.id.txtAllData);
            txtAllData.setText(allData.toString());
            
            Button btnReset = (Button) testScreenDialog.findViewById(R.id.btnReset);
            btnReset.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    duplicates = new StringBuffer();
                    allData = new StringBuffer();
                    
                    synchronized (allLocations) {
                        allLocations.clear();
                    }
                    
                    txtAllData.setText("");
                    txtDuplicates.setText("");
                }
            });
            
            Button btnClose = (Button) testScreenDialog.findViewById(R.id.btnClose);
            btnClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    testScreenDialog.dismiss();
                    testScreenDialog = null;
                }
            });
            
            Button btnEmailIt = (Button) testScreenDialog.findViewById(R.id.btnEmail);
            btnEmailIt.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.test_email_subject));
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "***Duplicates***\n\n" + duplicates.toString() + "\n\n***All data***\n\n" + allData.toString());
                    emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, getResources().getStringArray(R.array.bugreport_persons));

                    startActivity(emailIntent);
                }
            });
            
        }
        testScreenDialog.show();
    }

    private void updateLocale() {
        String languageToLoad =
                KeypadMapperApplication.getInstance().getSettings().getCurrentLanguageCode();
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        lang = languageToLoad;
    }
    
    /** 
     * THIS IS ONLY A TEST PROCEDURE mimicking Mapper.saveCurrentAddress() for
     * spotting duplicates.
     * 
     * @param forward
     * @param left
     * @param locationToSave
     * @return
     */
    public LocObj getCurrentAddress(double forward, double left, Location locationToSave) {
        LocObj locObj = new LocObj();
       
        forward /= 111111;
        left /= 111111;

        double lat = 0.0d;
        double lon = 0.0d;

        double speed = locationToSave.getSpeed();
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        // convert meters per second to km/h or mph
        if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
            // meters
            speed = speed * KeypadMapperSettings.M_PER_SEC_HAS_KM_PER_HOUR;
        } else {
            // feet
            speed = speed * KeypadMapperSettings.M_PER_SEC_HAS_MILES_PER_HOUR;
        }

        if (settings.isCompassAvailable() 
                && (((speed > 0.0 && settings.getUseCompassAtSpeed() > 0.0
                        && speed < settings.getUseCompassAtSpeed()) || 
                     (speed == 0.0 && settings.getUseCompassAtSpeed() > 0.0)))) {

            float azimuth = (float) Math.toDegrees(KeypadMapper2Activity.azimuth); // orientation
            if (azimuth < 0.0f) {
                azimuth = (azimuth + 360);
            }
            float bearing = azimuth;

            lat =
                    (locationToSave.getLatitude()
                            + Math.sin(Math.PI / 180 * bearing) * left + Math
                            .cos(Math.PI / 180 * bearing) * forward);
            lon =
                    (locationToSave.getLongitude() + (Math.sin(Math.PI / 180
                            * bearing)
                            * forward - Math.cos(Math.PI / 180 * bearing)
                            * left)
                            / Math.cos(Math.PI / 180 * locationToSave.getLatitude()));
            locObj.compassLat = lat;
            locObj.compassLon = lon;
        } else {
            // use bearing
            lat =
                    (locationToSave.getLatitude()
                            + Math.sin(Math.PI / 180 * locationToSave.getBearing()) * left + Math
                            .cos(Math.PI / 180 * locationToSave.getBearing()) * forward);
            lon =
                    (locationToSave.getLongitude() + (Math.sin(Math.PI / 180
                            * locationToSave.getBearing())
                            * forward - Math.cos(Math.PI / 180 * locationToSave.getBearing())
                            * left)
                            / Math.cos(Math.PI / 180 * locationToSave.getLatitude()));
            locObj.gpsLat = lat;
            locObj.gpsLong = lon;
        }
        
        return locObj;
    }
    
    public KeypadMapperMenu getMenu() {
        return menu;
    }

    @Override
    public void onAddressUpdated() {
        Log.d("Keypad", "address updated sent");
        extendedAddressFragment.updatedAddress();
    }
}
