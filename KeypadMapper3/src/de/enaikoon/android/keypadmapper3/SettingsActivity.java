package de.enaikoon.android.keypadmapper3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.file.KeypadMapperFolderCleaner;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.photo.CameraHelper;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;
import de.enaikoon.android.keypadmapper3.view.menu.KeypadMapperMenu;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener;
import de.enaikoon.android.library.resources.locale.Localizer;

public class SettingsActivity extends Activity implements OnClickListener, OnDismissListener {
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    
    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Intent data = new Intent();
                    data.putExtra("option", "swipe_left");
                    KeypadMapperApplication.getInstance().setScreenToActivate("swipe_left");
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    Intent data = new Intent();
                    data.putExtra("option", "swipe_right");
                    KeypadMapperApplication.getInstance().setScreenToActivate("swipe_right");
                    setResult(Activity.RESULT_OK, data);
                    finish();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }

    }

    public static void startActivityForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivityForResult(intent, requestCode);
    }
    
    private GestureDetector gestureDetector;
    
    private ScrollView svScrollView;
    
    private LinearLayout llGeneralLanguage;
    private TextView txtGeneralLanguageTitle;
    private TextView txtGeneralLanguageSummary;
    
    private LinearLayout llRate;
    private TextView txtRateTitle;
    private TextView txtRateSummary;
    
    private LinearLayout llShare;
    private TextView txtShareTitle;
    private TextView txtShareSummary;
    
    private LinearLayout llClearFolder;
    private TextView txtClearFolderTitle;
    private TextView txtClearFolderSummary;
    
    private LinearLayout llScreenOn;
    private TextView txtScreenOnTitle;
    private TextView txtScreenOnSummary;
    private CheckBox chkScreenOn;
    
    private LinearLayout llCompass;
    private TextView txtCompassTitle;
    private TextView txtCompassSummary;
    private SeekBar seekCompass;
    private TextView txtCompassValue;
    
    private LinearLayout llVibrationTime;
    private TextView txtVibrationTimeTitle;
    private TextView txtVibrationTimeSummary;
    private SeekBar seekVibrationTime;
    private TextView txtVibrationTimeValue;
    
    private LinearLayout llKeypadVibration;
    private TextView txtKeypadVibrationTitle;
    private TextView txtKeypadVibrationSummary;
    private SeekBar seekKeypadVibration;
    private TextView txtKeypadVibrationValue;
    
    private LinearLayout llMeasurement;
    private TextView txtMeasurementTitle;
    private TextView txtMeasurementSummary;
    
    private LinearLayout llHouseNumberDistance;
    private TextView txtHouseNumberDistanceTitle;
    private TextView txtHouseNumberDistanceSummary;
    private SeekBar seekHouseNumberDistance;
    private TextView txtHouseNumberDistanceValue;
    
    private LinearLayout llTurnOffUpdates;
    private TextView txtTurnOffUpdatesTitle;
    private TextView txtTurnOffUpdatesSummary;
    private CheckBox chkTurnOffUpdates;
    
    private LinearLayout llWifiOnly;
    private TextView txtWifiOnlyTitle;
    private TextView txtWifiOnlySummary;
    private CheckBox chkWifiOnly;
    
    private LinearLayout llWavPath;
    private TextView txtWavPathTitle;
    private TextView txtWavPathSummary;
    
    private LinearLayout llOptimizeLayout;
    private TextView txtOptimizeLayoutTitle;
    private TextView txtOptimizeLayoutSummary;
    private CheckBox chkOptimizeLayout;
    
    private LinearLayout llBugReport;
    private TextView txtBugReportTitle;
    private TextView txtBugReportSummary;
    
    private LinearLayout llHelp;
    private TextView txtHelpTitle;
    private TextView txtHelpSummary;
    
    private LinearLayout llAbout;
    private TextView txtAboutTitle;
    private TextView txtAboutSummary;
    
    private KeypadMapperMenu menu; 
    private Mapper mapper;
    private Localizer localizer;
    private KeypadMapperSettings settings;
    
    private Dialog dialog;
    private int activeDialog;
    
    private final static int DIALOG_LANGUAGE = 0;
    private final static int DIALOG_DELETE_FILES = 1;
    private final static int DIALOG_MEASUREMENT = 2;
    private final static int DIALOG_WAV_PATH = 3;
    private final static int DIALOG_BUG_REPORT = 4;
    private final static int DIALOG_ABOUT = 5;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        KeypadMapperApplication.getInstance().setScreenToActivate(null);
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_settings);
        
        mapper = KeypadMapperApplication.getInstance().getMapper();
        localizer = KeypadMapperApplication.getInstance().getLocalizer();
        settings = KeypadMapperApplication.getInstance().getSettings();
        
        menu = new KeypadMapperMenu(findViewById(R.id.menu));
        menu.setMenuListener(new MenuListener() {

            @Override
            public void onMenuOptionClicked(OptionType type) {
                if (type == OptionType.FREEZE_GPS) {
                    KeypadMapperApplication.getInstance().getMapper().freezeUnfreezeLocation(SettingsActivity.this);
                } else if (type == OptionType.UNDO) {
                    mapper.undo();
                } else if (type == OptionType.CAMERA) {
                    CameraHelper.startPhotoIntent(SettingsActivity.this);
                } else if (type != OptionType.SETTINGS) {
                    Intent data = new Intent();
                    data.putExtra("option", type.toString());
                    KeypadMapperApplication.getInstance().setScreenToActivate(type.toString());
                    setResult(Activity.RESULT_OK, data);
                    finish();
                } 
            }
        });
        menu.setPreferenceMode(true);
        
        // load language
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String languageToLoad = preferences.getString("general_language", "en"); // your
                                                                                 // language
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        
        svScrollView = (ScrollView) findViewById(R.id.svScrollView);
        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        svScrollView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        });

        // initialize all layouts
        llGeneralLanguage = (LinearLayout) findViewById(R.id.setting_general_language);
        llGeneralLanguage.setOnClickListener(this);
        txtGeneralLanguageTitle = (TextView) llGeneralLanguage.findViewById(R.id.txtTitle);
        txtGeneralLanguageTitle.setText(localizer.getString("prefsLanguageTitle"));
        txtGeneralLanguageSummary = (TextView) llGeneralLanguage.findViewById(R.id.txtSummary);
        txtGeneralLanguageSummary.setText(localizer.getString("prefsLanguageSummary"));
        
        llRate = (LinearLayout) findViewById(R.id.setting_rate);
        llRate.setOnClickListener(this);
        txtRateTitle = (TextView) llRate.findViewById(R.id.txtTitle);
        txtRateTitle.setText(localizer.getString("settings_rate_title"));
        txtRateSummary = (TextView) llRate.findViewById(R.id.txtSummary);
        txtRateSummary.setText(localizer.getString("settings_rate_summary"));
        
        llShare = (LinearLayout) findViewById(R.id.setting_share);
        llShare.setOnClickListener(this);
        txtShareTitle = (TextView) llShare.findViewById(R.id.txtTitle);
        txtShareTitle.setText(localizer.getString("prefsShare"));
        txtShareSummary = (TextView) llShare.findViewById(R.id.txtSummary);
        txtShareSummary.setText(localizer.getString("prefsShareDetails"));
        
        llClearFolder = (LinearLayout) findViewById(R.id.setting_clear_folder);
        llClearFolder.setOnClickListener(this);
        txtClearFolderTitle = (TextView) llClearFolder.findViewById(R.id.txtTitle);
        txtClearFolderTitle.setText(localizer.getString("prefsClearFolderTitle"));
        txtClearFolderSummary = (TextView) llClearFolder.findViewById(R.id.txtSummary);
        txtClearFolderSummary.setText(localizer.getString("prefsClearFolderSummary"));
        
        llScreenOn = (LinearLayout) findViewById(R.id.setting_screen_on);
        llScreenOn.setOnClickListener(this);
        txtScreenOnTitle = (TextView) llScreenOn.findViewById(R.id.txtTitle);
        txtScreenOnTitle.setText(localizer.getString("prefsScreenOnTitle"));
        txtScreenOnSummary = (TextView) llScreenOn.findViewById(R.id.txtSummary);
        txtScreenOnSummary.setText(localizer.getString("prefsScreenOnSummary"));
        chkScreenOn = (CheckBox) llScreenOn.findViewById(R.id.btn_checkbox);
        
        llCompass = (LinearLayout) findViewById(R.id.setting_compass);
        llCompass.setOnClickListener(this);
        txtCompassTitle = (TextView) llCompass.findViewById(R.id.txtTitle);
        txtCompassTitle.setText(localizer.getString("prefsUseCompassTitle"));
        txtCompassSummary = (TextView) llCompass.findViewById(R.id.txtSummary);
        txtCompassSummary.setText(localizer.getString("prefsUseCompassSummary"));
        txtCompassValue = (TextView) llCompass.findViewById(R.id.seekBarPrefValue);
        seekCompass = (SeekBar) llCompass.findViewById(R.id.seekbar);
        
        llVibrationTime = (LinearLayout) findViewById(R.id.setting_vibration_time);
        llVibrationTime.setOnClickListener(this);
        txtVibrationTimeTitle = (TextView) llVibrationTime.findViewById(R.id.txtTitle);
        txtVibrationTimeTitle.setText(localizer.getString("prefsVibrationOnSave"));
        txtVibrationTimeSummary = (TextView) llVibrationTime.findViewById(R.id.txtSummary);
        txtVibrationTimeSummary.setText(localizer.getString("prefsVibrationOnSaveSummary"));
        seekVibrationTime = (SeekBar) llVibrationTime.findViewById(R.id.seekbar);
        txtVibrationTimeValue = (TextView) llVibrationTime.findViewById(R.id.seekBarPrefValue);
        
        llKeypadVibration = (LinearLayout) findViewById(R.id.setting_keyboard_vibration);
        llKeypadVibration.setOnClickListener(this);
        txtKeypadVibrationTitle = (TextView) llKeypadVibration.findViewById(R.id.txtTitle);
        txtKeypadVibrationTitle.setText(localizer.getString("prefsKeyboardVibrationTitle"));
        txtKeypadVibrationSummary = (TextView) llKeypadVibration.findViewById(R.id.txtSummary);
        txtKeypadVibrationSummary.setText(localizer.getString("prefsKeyboardVibrationSummary"));
        seekKeypadVibration = (SeekBar) llKeypadVibration.findViewById(R.id.seekbar);
        txtKeypadVibrationValue = (TextView) llKeypadVibration.findViewById(R.id.seekBarPrefValue);
        
        llMeasurement = (LinearLayout) findViewById(R.id.setting_measurement);
        llMeasurement.setOnClickListener(this);
        txtMeasurementTitle = (TextView) llMeasurement.findViewById(R.id.txtTitle);
        txtMeasurementTitle.setText(localizer.getString("prefsMeasurementTitle"));
        txtMeasurementSummary = (TextView) llMeasurement.findViewById(R.id.txtSummary);
        txtMeasurementSummary.setText(localizer.getString("prefsMeasurementSummary"));
        
        llHouseNumberDistance = (LinearLayout) findViewById(R.id.setting_house_number_distance);
        llHouseNumberDistance.setOnClickListener(this);
        txtHouseNumberDistanceTitle = (TextView) llHouseNumberDistance.findViewById(R.id.txtTitle);
        txtHouseNumberDistanceTitle.setText(localizer.getString("prefsDataPlacementDistance"));
        txtHouseNumberDistanceSummary = (TextView) llHouseNumberDistance.findViewById(R.id.txtSummary);
        txtHouseNumberDistanceSummary.setText(localizer.getString("prefsDataPlacementDistanceSummary"));
        seekHouseNumberDistance = (SeekBar) llHouseNumberDistance.findViewById(R.id.seekbar);
        txtHouseNumberDistanceValue = (TextView) llHouseNumberDistance.findViewById(R.id.seekBarPrefValue);
        
        llTurnOffUpdates = (LinearLayout) findViewById(R.id.setting_turnoff_updates);
        llTurnOffUpdates.setOnClickListener(this);
        txtTurnOffUpdatesTitle = (TextView) llTurnOffUpdates.findViewById(R.id.txtTitle);
        txtTurnOffUpdatesTitle.setText(localizer.getString("prefsTurnOffGpsUpdatesTitle"));
        txtTurnOffUpdatesSummary = (TextView) llTurnOffUpdates.findViewById(R.id.txtSummary);
        txtTurnOffUpdatesSummary.setText(localizer.getString("prefsTurnOffGpsUpdatesSummary"));
        chkTurnOffUpdates = (CheckBox) llTurnOffUpdates.findViewById(R.id.btn_checkbox);
        
        llWifiOnly = (LinearLayout) findViewById(R.id.setting_wifi_only);
        llWifiOnly.setOnClickListener(this);
        txtWifiOnlyTitle = (TextView) llWifiOnly.findViewById(R.id.txtTitle);
        txtWifiOnlyTitle.setText(localizer.getString("prefsUseWifiOnlyTitle"));
        txtWifiOnlySummary = (TextView) llWifiOnly.findViewById(R.id.txtSummary);
        txtWifiOnlySummary.setText(localizer.getString("prefsUseWifiOnlySummary"));
        chkWifiOnly = (CheckBox) llWifiOnly.findViewById(R.id.btn_checkbox);
        
        llWavPath = (LinearLayout) findViewById(R.id.setting_wav_path);
        llWavPath.setOnClickListener(this);
        txtWavPathTitle = (TextView) llWavPath.findViewById(R.id.txtTitle);
        txtWavPathTitle.setText(localizer.getString("prefsComputerWaveFilePathTitle"));
        txtWavPathSummary = (TextView) llWavPath.findViewById(R.id.txtSummary);
        txtWavPathSummary.setText(localizer.getString("prefsComputerWaveFilePathSummary"));
        
        llOptimizeLayout = (LinearLayout) findViewById(R.id.setting_optimize_layout);
        llOptimizeLayout.setOnClickListener(this);
        txtOptimizeLayoutTitle = (TextView) llOptimizeLayout.findViewById(R.id.txtTitle);
        txtOptimizeLayoutTitle.setText(localizer.getString("prefsOptimizeLayout"));
        txtOptimizeLayoutSummary = (TextView) llOptimizeLayout.findViewById(R.id.txtSummary);
        txtOptimizeLayoutSummary.setText("");
        chkOptimizeLayout = (CheckBox) llOptimizeLayout.findViewById(R.id.btn_checkbox);
        
        llBugReport  = (LinearLayout) findViewById(R.id.setting_bug_report);
        llBugReport.setOnClickListener(this);
        txtBugReportTitle = (TextView) llBugReport.findViewById(R.id.txtTitle);
        txtBugReportTitle.setText(localizer.getString("options_bugreport"));
        txtBugReportSummary = (TextView) llBugReport.findViewById(R.id.txtSummary);
        txtBugReportSummary.setText(localizer.getString("options_bugreport_summary"));
        
        llHelp = (LinearLayout) findViewById(R.id.setting_help);
        llHelp.setOnClickListener(this);
        txtHelpTitle = (TextView) llHelp.findViewById(R.id.txtTitle);
        txtHelpTitle.setText(localizer.getString("prefsHelpTitle"));
        txtHelpSummary = (TextView) llHelp.findViewById(R.id.txtSummary);
        txtHelpSummary.setText(localizer.getString("prefsHelpSummary"));
        
        llAbout  = (LinearLayout) findViewById(R.id.setting_about);
        llAbout.setOnClickListener(this);
        txtAboutTitle = (TextView) llAbout.findViewById(R.id.txtTitle);
        txtAboutTitle.setText(localizer.getString("prefsAbout"));
        txtAboutSummary = (TextView) llAbout.findViewById(R.id.txtSummary);
        txtAboutSummary.setText(localizer.getString("prefsAboutDetails"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        menu.onResume();
        mapper.onResume();
        
        // init values...
        init();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());

        menu.onPause();
        mapper.onPause();
    }
   
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray("scrollPos", new int[]{ svScrollView.getScrollX(), svScrollView.getScrollY()});
        
        if (dialog != null && dialog.isShowing()) {
            outState.putInt("dialog_displayed", activeDialog);
        }
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("dialog_displayed")) {
            switch (savedInstanceState.getInt("dialog_displayed")) {
            case DIALOG_LANGUAGE:
                showSelectLanguageDialog();
                break;
            case DIALOG_DELETE_FILES:
                showDeleteFilesDialog();
                break;
            case DIALOG_MEASUREMENT:
                showMesurementUnitsDialog();
                break;
            case DIALOG_WAV_PATH:
                showWavPathDialog();
                break;
            case DIALOG_BUG_REPORT:
                showBugReportDialog();
                break;
            case DIALOG_ABOUT:
                showAboutDialog();
                break;
                
                default:
                    break;
            }
        }
        // restore scroll position
        if (savedInstanceState != null && savedInstanceState.containsKey("scrollPos")) {
            final int[] position = savedInstanceState.getIntArray("scrollPos");
            svScrollView.post(new Runnable() {
                @Override
                public void run() {
                    svScrollView.scrollTo(position[0], position[1]);
                }
            });
        }
    }
    
    @Override
    public void onClick(View v) {
        if (v == llGeneralLanguage) {
            showSelectLanguageDialog();
        } else if (v == llRate) {
            rateApp();
        } else if (v == llShare) {
            shareFiles();
        } else if (v == llClearFolder) {
            showDeleteFilesDialog();
        } else if (v == llScreenOn) {
            handleScreenOnOption();
        } else if (v ==  llCompass) {
            // nothing - seekbar should handle it
        } else if (v == llVibrationTime) {
            // nothing - seekbar should handle it            
        } else if (v == llKeypadVibration) {
            // nothing - seekbar should handle it
        } else if (v == llMeasurement) {
            showMesurementUnitsDialog();
        } else if (v == llHouseNumberDistance) {
            // nothing - seekbar should handle it
        } else if (v == llTurnOffUpdates) {
            handleTurnOffUpdatesOption();
        } else if (v == llWifiOnly) {
            handleWifiOnly();
        } else if (v == llWavPath) {
            showWavPathDialog();
        } else if (v == llOptimizeLayout) {
            handleOptimizeLayout();
        } else if (v == llBugReport) {
            showBugReportDialog();
        } else if (v == llHelp) {
            startHelpActivity();
        } else if (v == llAbout) {
            showAboutDialog();
        }
    }
    
    private void showSelectLanguageDialog() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(localizer.getString("prefsLanguageTitle"));
        
        String[] codes = localizer.getStringArray("lang_support_codes");
        String[] names = localizer.getStringArray("lang_support_names");

        boolean[] loaded = new boolean[codes.length];
        for (int i = 0; i < loaded.length; i++) {
            loaded[i] = localizer.isLocaleLoaded(codes[i]);
        }

        List<String> loadedCodes = new ArrayList<String>();
        List<String> loadedNames = new ArrayList<String>();
        int tempSelected = 0;
        for (int i = 0; i < codes.length && i < names.length; i++) {
            if (loaded[i]) {
                loadedCodes.add(codes[i]);
                loadedNames.add(names[i]);
                if (codes[i].equalsIgnoreCase(settings.getCurrentLanguageCode())) {
                    Log.d("Keypad", "Selected language code: " + settings.getCurrentLanguageCode());
                    tempSelected = i;
                }
            } 
        }
        
        final CharSequence [] entries = loadedNames.toArray(new CharSequence[]{});
        final List<String> refCodes = loadedCodes;
        final int selected = tempSelected;
        
        adb.setSingleChoiceItems(entries, tempSelected, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface localdialog, int which) {
                if (which == selected) {
                    return;
                }
                settings.setCurrentLanguageCode(refCodes.get(which).toString());
                SettingsActivity.this.finish();
                
                Intent localIntent = new Intent(SettingsActivity.this, SettingsActivity.class);
                SettingsActivity.this.startActivity(localIntent);
            }
        });
        
        adb.setNegativeButton(localizer.getString("cancel"), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface localdialog, int which) {
                dialog.dismiss();
            }
        });

        activeDialog = DIALOG_LANGUAGE;
        
        dialog = adb.create();
        dialog.setOnDismissListener(this);
        dialog.show();
    }

    private void rateApp() {
        settings.setLaunchCount(100);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=de.enaikoon.android.keypadmapper3"));
            startActivity(intent);
        } catch (Exception e) {
            Log.e("KeypadMapper", "No activity to handle rate app on market intent.");
        }
    }
    
    private void shareFiles() {
        if (settings.isRecording()) {
            KeypadMapperApplication.getInstance().stopGpsRecording();
        }
        Intent share = new Intent(SettingsActivity.this, ShareFilesActivity.class);
        startActivity(share);
    }
    
    private void showDeleteFilesDialog() {
        AlertDialog.Builder clearDialog = new AlertDialog.Builder(SettingsActivity.this);
        clearDialog
                .setMessage(localizer.getString("prefsClearFolderQuestion"))
                .setPositiveButton(localizer.getString("prefsClearFolderQuestionYes"),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (settings.isRecording()) {
                                    KeypadMapperApplication.getInstance().stopGpsRecording();
                                }
                                KeypadMapperFolderCleaner
                                        .cleanFolder(KeypadMapperApplication.getInstance()
                                                .getKeypadMapperDirectory());
                                KeypadMapperApplication.getInstance().getMapper()
                                        .setFolderCleared(true);
                                menu.updateShareIcon();
                            }
                        })
                .setNegativeButton(localizer.getString("prefsClearFolderQuestionNo"), null);
                
        dialog = clearDialog.create();
        activeDialog = DIALOG_DELETE_FILES;
        dialog.setOnDismissListener(this);
        dialog.show();
    }
    
    private void handleScreenOnOption() {
        settings.setKeepScreenOnEnabled(!settings.isKeepScreenOnEnabled());
        chkScreenOn.setChecked(settings.isKeepScreenOnEnabled());
    }

    /**
     * handles display to reflect stored values
     */
    private void init() {
        // share should be disabled if it can't be shared
        if (!KeypadMapperApplication.getInstance().isAnyDataAvailable()) {
            llShare.setClickable(false);
            txtShareTitle.setTextColor(getResources().getColor(R.color.disabled_title));
            txtShareSummary.setTextColor(getResources().getColor(R.color.disabled_summary));
        } else {
            llShare.setClickable(true);
            txtShareTitle.setTextColor(getResources().getColor(R.color.white));
            txtShareSummary.setTextColor(getResources().getColor(R.color.ENAiKOON_light_gray));
        }
        
        chkScreenOn.setChecked(settings.isKeepScreenOnEnabled());
        
        if (!settings.isCompassAvailable()) {
            llCompass.setVisibility(View.GONE);
        } else {
            updateCompassText();
            seekCompass.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    settings.setUseCompassAtSpeed(seekBar.getProgress());
                    updateCompassText();
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        settings.setUseCompassAtSpeed(seekBar.getProgress());
                        updateCompassText();
                    }
                }
            });
        }
        
        // vibration on save slider
        if (!KeypadMapperApplication.getInstance().systemHasVibrator()) {
            llVibrationTime.setVisibility(View.GONE);
        } else {
            txtVibrationTimeValue.setText("" + settings.getVibrationTime());
            seekVibrationTime.setMax(KeypadMapperSettings.MAX_VIBRATION_TIME);
            seekVibrationTime.setProgress(settings.getVibrationTime());
            seekVibrationTime.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    settings.setVibrationTime(seekBar.getProgress());
                    txtVibrationTimeValue.setText("" + seekBar.getProgress());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    settings.setVibrationTime(progress);
                    txtVibrationTimeValue.setText("" + progress);
                }
            });
        }
        
        // keypad vibration time slider
        if (!KeypadMapperApplication.getInstance().systemHasVibrator()) {
            llKeypadVibration.setVisibility(View.GONE);
        } else {
            txtKeypadVibrationValue.setText("" + settings.getKeyboardVibrationTime());
            seekKeypadVibration.setMax(KeypadMapperSettings.KEYBOARD_MAX_VIBRATION_TIME);
            seekKeypadVibration.setProgress(settings.getKeyboardVibrationTime());
            seekKeypadVibration.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    settings.setKeyboardVibrationTime(seekBar.getProgress());
                    txtKeypadVibrationValue.setText("" + seekBar.getProgress());
                }
                
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }
                
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    settings.setKeyboardVibrationTime(progress);
                    txtKeypadVibrationValue.setText("" + progress);
                }
            });
        }
        
        //distances
        updateDistanceText();
        seekHouseNumberDistance.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.setHouseNumberDistance(seekBar.getProgress());
                updateDistanceText();
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    settings.setHouseNumberDistance(progress);
                    updateDistanceText();
                }
            }
        });
        
        chkTurnOffUpdates.setChecked(settings.isTurnOffUpdates());
        chkWifiOnly.setChecked(settings.isWiFiOnlyEnabled());
        
        // initialize the DisplayMetrics object
        DisplayMetrics deviceDisplayMetrics = new DisplayMetrics();

        // populate the DisplayMetrics object with the display characteristics
        getWindowManager().getDefaultDisplay().getMetrics(deviceDisplayMetrics);

        // get the width and height
        int screenWidth = deviceDisplayMetrics.widthPixels;
        int screenHeight = deviceDisplayMetrics.heightPixels;

        int max = Math.max(screenWidth, screenHeight);
        chkOptimizeLayout.setChecked(settings.isLayoutOptimizationEnabled());
        if (max > 480) {
            llOptimizeLayout.setClickable(false);
            chkOptimizeLayout.setEnabled(false);
            txtOptimizeLayoutTitle.setTextColor(getResources().getColor(R.color.disabled_title));
        } else {
            txtOptimizeLayoutTitle.setTextColor(getResources().getColor(R.color.white));
            llOptimizeLayout.setClickable(true);
            chkOptimizeLayout.setEnabled(true);
        }
    }

    private void updateCompassText() {
        String text = "" + settings.getUseCompassAtSpeed() + " ";
        
        if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
            text += KeypadMapperApplication.getInstance().getLocalizer().getString("km_per_hour");
            seekCompass.setMax(KeypadMapperSettings.MAX_USE_COMPASS_AT_SPEED_KMH);

        } else {
            text += KeypadMapperApplication.getInstance().getLocalizer().getString("miles_per_hour");
            seekCompass.setMax(KeypadMapperSettings.MAX_USE_COMPASS_AT_SPEED_MPH);
        }

        seekCompass.setProgress(settings.getUseCompassAtSpeed());
        if (settings.getUseCompassAtSpeed() == 0) {
            menu.setUseCompass(false);
        } else {
            menu.setUseCompass(true);
        }
        menu.setGpsInfoMode(menu.isGpsInfoMode());
        
        txtCompassValue.setText(text);
    }
   
    private void showMesurementUnitsDialog() {
        CharSequence [] entries = new CharSequence[] {
                localizer.getString("prefsMeasurementEntryMeters"),
                localizer.getString("prefsMeasurementEntryFeet")
        }; 
        
        final CharSequence [] entryValues = new CharSequence[] {
                KeypadMapperSettings.UNIT_METER,
                KeypadMapperSettings.UNIT_FEET
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(localizer.getString("cancel"), null);
        
        int selected = 0;
        if (!settings.getMeasurement().equalsIgnoreCase(KeypadMapperSettings.UNIT_METER)) {
            selected = 1;
        } 
        
        builder.setTitle(localizer.getString("prefsMeasurementTitle"));
        builder.setSingleChoiceItems(entries, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String oldValue = settings.getMeasurement();
                settings.setMeasurement(entryValues[which].toString());

                if (!oldValue.equals(entryValues[which].toString())) {
                    // convert the current setting
                    int newVal = 0;
                    int newDistance = 0;
                    if (oldValue.equals(KeypadMapperSettings.UNIT_METER)) {
                        // convert km/h to mph
                        newVal = (int) Math.rint(settings.getUseCompassAtSpeed() * KeypadMapperSettings.KMH_HAS_MPH);
                        newDistance = (int) Math.rint(UnitsConverter.convertMetersToFeets(settings.getHouseNumberDistance()));
                    } else {
                        // convert miles per hour to kilometers
                        newVal = (int) Math.rint(settings.getUseCompassAtSpeed() * KeypadMapperSettings.MPH_HAS_KMH);
                        newDistance = (int) Math.rint(UnitsConverter.convertFeetsToMeters(settings.getHouseNumberDistance()));
                    }
                    
                    settings.setUseCompassAtSpeed(newVal);
                    settings.setHouseNumberDistance(newDistance);

                }
                dialog.dismiss();
            }
        });
        
        activeDialog = DIALOG_MEASUREMENT;
        dialog = builder.create();
        dialog.setOnDismissListener(this);
        dialog.show();
    }
    
    private void updateDistanceText() {
        String text = "" + settings.getHouseNumberDistance() + " ";
        
        if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
            text += KeypadMapperApplication.getInstance().getLocalizer().getString("meters_display_unit");
            seekHouseNumberDistance.setMax(KeypadMapperSettings.MAX_HOUSE_NUMBER_DISTANCE_METERS);
        } else {
            text += KeypadMapperApplication.getInstance().getLocalizer().getString("feet_display_unit");
            seekHouseNumberDistance.setMax(KeypadMapperSettings.MAX_HOUSE_NUMBER_DISTANCE_FEET);
        }

        seekHouseNumberDistance.setProgress(settings.getHouseNumberDistance());
        txtHouseNumberDistanceValue.setText(text);
    }
    
    private void handleTurnOffUpdatesOption() {
        settings.setTurnOffUpdates(!settings.isTurnOffUpdates());
        chkTurnOffUpdates.setChecked(settings.isTurnOffUpdates());
        
        LocationProvider provider = KeypadMapperApplication.getInstance().getLocationProvider();
        if (settings.isTurnOffUpdates() == true && !settings.isRecording()) {
            provider.stopRequestingUpdates();
        } else if (settings.isTurnOffUpdates() == false) {
            provider.startRequestingUpdates();
            provider.addLocationListener(menu);
            provider.addLocationListener(mapper);
        }
    }
    
    private void handleWifiOnly() {
        settings.setWifiOnly(!settings.isWiFiOnlyEnabled());
        chkWifiOnly.setChecked(settings.isWiFiOnlyEnabled());
    }
    
    private void showWavPathDialog() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.wav_dialog, (ViewGroup) findViewById(R.id.layout_root));
        TextView text = (TextView) layout.findViewById(R.id.text);
        String wavText = localizer.getString("dialog_wav_explanation_html");
        text.setText(Html.fromHtml(wavText));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        final EditText edtPath = (EditText) layout.findViewById(R.id.edtWavPath);
        edtPath.setText(settings.getWavDir());
        
        dialog = new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle(localizer.getString("app_name"))
                            .setView(layout)
                            .setPositiveButton(localizer.getString("close"), new DialogInterface.OnClickListener() {
                                
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    settings.setWavDir(edtPath.getText().toString());
                                    
                                    dialog.dismiss();
                                    dialog = null;
                                }
                            }).create();
        activeDialog = DIALOG_WAV_PATH;
        dialog.show();
    }
    
    private void handleOptimizeLayout() {
        settings.setLayoutOptimizationEnabled(!settings.isLayoutOptimizationEnabled());
        chkOptimizeLayout.setChecked(settings.isLayoutOptimizationEnabled());
        
        finish();
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);
        
    }

    private void showBugReportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(localizer.getString("options_bugreport"));
        String[] displayStrings = new String[] { localizer.getString("options_bugreport_values_1"),
                                               localizer.getString("options_bugreport_values_2"),
                                               localizer.getString("options_bugreport_values_3") };
        final String [] constantStrings = getResources().getStringArray(R.array.options_bugreport_keys);
        int checkedItem = -1;
        for (checkedItem = 0; checkedItem < constantStrings.length; checkedItem++) {
            if (settings.getErrorReporting().equals(constantStrings[checkedItem])) {
                break;
            }
        }
        
        builder.setNegativeButton(localizer.getString("cancel"), null);
        builder.setSingleChoiceItems(displayStrings, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                settings.setErrorReporting(constantStrings[which]);
                dialog.dismiss();
            }
        });
        
        activeDialog = DIALOG_BUG_REPORT;
        dialog = builder.create();
        dialog.show();
    }
    
    private void showAboutDialog() {
        // display About window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout =
                inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.layout_root));
        TextView text = (TextView) layout.findViewById(R.id.text);
        String aboutText = localizer.getString("about_message");
        text.setText(Html.fromHtml(String.format(aboutText, localizer.getString("app_name"), getVersionName())));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageDrawable(localizer.getDrawable("enaikoon_logo_frosted"));

        dialog = new AlertDialog.Builder(this)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setTitle(localizer.getString("app_name"))
                                .setView(layout)
                                .setOnCancelListener(null)
                                .setPositiveButton(localizer.getString("close"), new DialogInterface.OnClickListener() {
                                    
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).create(); 

        activeDialog = DIALOG_ABOUT;
        dialog.show();

        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(localizer.getString("about_logo_url")));
                startActivity(i);
            }
        });
    }
    
    private String getVersionName() {
        try {
            // get the app version number
            PackageInfo pInfo =
                    getPackageManager().getPackageInfo(getPackageName(),
                            PackageManager.GET_META_DATA);

            return pInfo.versionName;
        } catch (Exception e) {
            return "";
        }
    }
    
    private void startHelpActivity() {
        Intent help = new Intent(SettingsActivity.this, HelpActivity.class);
        startActivity(help);
    }
    
    @Override
    public void onDismiss(DialogInterface d) {
        activeDialog = -1;
        dialog = null;
        init();
    }
    
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handled = CameraHelper.onActivityResult(this, requestCode, resultCode, data);
        if (handled) {
            return;
        }
    }
}
