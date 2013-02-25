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
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import de.enaikoon.android.inviu.opencellidlibrary.CellIDCollectionService;
import de.enaikoon.android.inviu.opencellidlibrary.Configurator;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.RateMeActivity;
import de.enaikoon.android.keypadmapper3.SatelliteInfoFragment;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.domain.NotificationListener;
import de.enaikoon.android.keypadmapper3.file.KeypadMapperFolderCleaner;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.photo.CameraHelper;
import de.enaikoon.android.keypadmapper3.services.ControllableResourceInitializerService;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.FontUtils;
import de.enaikoon.android.keypadmapper3.view.menu.KeypadMapperMenu;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener;
import de.enaikoon.android.library.resources.locale.Localizer;

public class KeypadMapper2Activity extends FragmentActivity implements AddressInterface,
        LocationListener, MenuListener, NotificationListener {

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (satteliteInfoVisible) {
                        showSettings();
                    } else {
                        if (state == State.keypad) {
                            state = State.extended;
                            showKeypad();
                        } else {
                            showSatteliteInfo();
                        }
                    }

                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    if (satteliteInfoVisible) {
                        state = State.extended;
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

    private LocationProvider locationProvider = KeypadMapperApplication.getInstance()
            .getLocationProvider();

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
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            uiOptimizationEnabled = true;
        }

        super.onCreate(savedInstanceState);

        // starting cellid library
        if (!CellIDCollectionService.isServiceStarted) {
            Configurator.setPRODUCTION_VERSION(true);
            Configurator.setSDCARD_DIRECTORY_NAME(KeypadMapperApplication.getInstance()
                    .getKeypadMapperDirectory().getAbsolutePath()
                    + "/" + "opencellid/");
            Intent startServiceIntent = new Intent(this, CellIDCollectionService.class);
            startService(startServiceIntent);
        }

        updateLocale();
        setContentView(R.layout.main);
        if (Build.VERSION.SDK_INT < 11) {
            ViewGroup rootView = (ViewGroup) this.getWindow().getDecorView();
            FontUtils.setRobotoFont(this, rootView);
        }
        menu = new KeypadMapperMenu(findViewById(R.id.menu));
        menu.setMenuListener(this);

        // check for GPS
        if (!locationProvider.isLocationServiceEnabled()) {
            showDialogGpsDisabled();
        }

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

        if (savedInstanceState == null) {

            KeypadMapperFolderCleaner.cleanFolderFromEmptyFiles(kpmFolder);

            // first start
            state = State.keypad;

        } else {
            // restart
            state = State.values()[savedInstanceState.getInt("state", State.keypad.ordinal())];
            satteliteInfoVisible = savedInstanceState.getBoolean("sat_info");

            extendedAddressActive = savedInstanceState.getBoolean("extended_address");

        }

        keypadFragment = (KeypadFragment) getSupportFragmentManager().findFragmentByTag("keypad");

        extendedAddressFragment =
                (ExtendedAddressFragment) getSupportFragmentManager().findFragmentByTag(
                        "extended_address");

        satelliteInfo =
                (SatelliteInfoFragment) getSupportFragmentManager().findFragmentByTag("satellite");

        satInfoView = findViewById(R.id.satellite_view);
        extendedAddressView = findViewById(R.id.extended_address_view);
        keypadView = findViewById(R.id.keypad_view);
        if (keypadView == null && extendedAddressView == null) {
            state = State.both;
        }

        if (satteliteInfoVisible) {
            showSatteliteInfo();
        } else {
            showKeypad();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (CellIDCollectionService.isServiceStarted) {
            Intent stopServiceIntent = new Intent(this, CellIDCollectionService.class);
            stopService(stopServiceIntent);
        }
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
    public void onLocationChanged(Location location) {
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
        } else if (type == OptionType.EDITOR_TOGGLE) {
            if (state == State.keypad) {
                state = State.extended;
            } else {
                state = State.keypad;
            }
            showKeypad();
        } else if (type == OptionType.UNDO) {
            mapper.undo();
        } else if (type == OptionType.SETTINGS) {
            showSettings();
        } else if (type == OptionType.FREEZE_GPS) {
            mapper.freezeUnfreezeLocation();
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
        ControllableResourceInitializerService.startResourceLoading(getApplicationContext(),
                "lang_support_codes", "lang_support_names", "lang_support_urls");
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mapper.addNotificationListener(this);
        mapper.onResume();
        menu.onResume();
        locationProvider.addLocationListener(this);

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
        } else {
            KeypadMapperApplication.getInstance().setExtendedEditorEnabled(true);
            menu.setKeypadMode(false);
            menu.setGpsInfoMode(false);
            extendedAddressView.setVisibility(View.VISIBLE);
            keypadView.setVisibility(View.GONE);
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
        hideKeyboard();
        satteliteInfoVisible = true;
        this.keypadView.setVisibility(View.GONE);
        this.extendedAddressView.setVisibility(View.GONE);
        this.satInfoView.setVisibility(View.VISIBLE);
        KeypadMapperApplication.getInstance().setExtendedEditorEnabled(false);
        state = State.keypad;
        menu.setGpsInfoMode(true);
    }

    public void showSettings() {
        hideKeyboard();
        satteliteInfoVisible = false;
        KeypadMapperApplication.getInstance().setExtendedEditorEnabled(false);
        state = State.keypad;
        Preferences.startActivityForResult(this, REQUEST_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handled = CameraHelper.onActivityResult(this, requestCode, resultCode, data);
        if (handled) {
            return;
        }
        switch (requestCode) {
        case REQUEST_GPS_ENABLE:
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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
        default:
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        menu.onPause();
        locationProvider.removeLocationListener(this);
        mapper.removeNotificationListener(this);
        mapper.onPause();

        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());

        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state", state.ordinal());
        outState.putBoolean("extended_address", extendedAddressActive);
        outState.putBoolean("sat_info", satteliteInfoVisible);
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
}
