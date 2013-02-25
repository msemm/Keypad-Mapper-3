package org.osm.keypadmapper2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.HelpActivity;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.ShareFilesActivity;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.file.KeypadMapperFolderCleaner;
import de.enaikoon.android.keypadmapper3.photo.CameraHelper;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.view.menu.KeypadMapperMenu;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener;
import de.enaikoon.android.library.resources.locale.Localizer;

public class Preferences extends PreferenceActivity {

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

    private static final int SWIPE_MIN_DISTANCE = 120;

    private static final int SWIPE_MAX_OFF_PATH = 250;

    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    public static void startActivityForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, Preferences.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivityForResult(intent, requestCode);
    }

    private GestureDetector gestureDetector;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();

    private Preference aboutPref;

    private Preference sharePref;

    private Preference clearPref;

    private AlertDialog aboutDialog;

    private KeypadMapperMenu menu;

    private Mapper mapper = KeypadMapperApplication.getInstance().getMapper();

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @SuppressWarnings("deprecation")
    // using old mode because the new mode needs fragments (works only with API
    // level >10)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        KeypadMapperApplication.getInstance().setScreenToActivate(null);
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        menu = new KeypadMapperMenu(findViewById(R.id.menu));
        menu.setMenuListener(new MenuListener() {

            @Override
            public void onMenuOptionClicked(OptionType type) {
                if (type == OptionType.FREEZE_GPS) {
                    KeypadMapperApplication.getInstance().getMapper().freezeUnfreezeLocation();
                } else if (type == OptionType.UNDO) {
                    mapper.undo();
                } else if (type == OptionType.CAMERA) {
                    CameraHelper.startPhotoIntent(Preferences.this);
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
        // menu.updateLocation(KeypadMapperApplication.getInstance().getMapper().getCurrentLocation());

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

        addPreferencesFromResource(R.xml.preferences);

        gestureDetector = new GestureDetector(this, new MyGestureDetector());
        findViewById(android.R.id.list).setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        });
        // help
        Preference helpPref = findPreference("help");
        helpPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent help = new Intent(Preferences.this, HelpActivity.class);
                startActivity(help);
                return true;
            }
        });
        helpPref.setTitle(localizer.getString("prefsHelpTitle"));
        helpPref.setSummary(localizer.getString("prefsHelpSummary"));

        // about
        aboutPref = findPreference("about");
        aboutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                showAboutDialog();
                return true;
            }
        });
        aboutPref.setTitle(localizer.getString("prefsAbout"));
        aboutPref.setSummary(localizer.getString("prefsAboutDetails"));

        sharePref = findPreference("share");
        sharePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent share = new Intent(Preferences.this, ShareFilesActivity.class);
                startActivity(share);
                return true;
            }
        });
        sharePref.setTitle(localizer.getString("prefsShare"));
        sharePref.setSummary(localizer.getString("prefsShareDetails"));

        initLanguagePreference();

        ListPreference errorPref = (ListPreference) findPreference("list_errorreporting");
        errorPref.setTitle(localizer.getString("options_bugreport"));
        errorPref.setSummary(localizer.getString("options_bugreport_summary"));
        errorPref.setDialogTitle(localizer.getString("options_bugreport"));
        String[] errorEntries =
                new String[] { localizer.getString("options_bugreport_values_1"),
                        localizer.getString("options_bugreport_values_2"),
                        localizer.getString("options_bugreport_values_3") };
        errorPref.setEntries(errorEntries);
        errorPref.setEntryValues(getResources().getTextArray(R.array.options_bugreport_keys));
        errorPref.setNegativeButtonText(localizer.getString("cancel"));

        updateDistanceValues(KeypadMapperApplication.getInstance().getSettings().getMeasurement());

        // measurement unit
        ListPreference measurementPref = (ListPreference) findPreference("measurement");
        measurementPref.setTitle(localizer.getString("prefsMeasurementTitle"));
        measurementPref.setSummary(localizer.getString("prefsMeasurementSummary"));
        measurementPref.setDialogTitle(localizer.getString("prefsMeasurementTitle"));
        String[] measurementValues =
                new String[] { localizer.getString("prefsMeasurementsEntries_1"),
                        localizer.getString("prefsMeasurementsEntries_2") };
        measurementPref.setEntries(measurementValues);
        measurementPref
                .setEntryValues(getResources().getTextArray(R.array.prefsMeasurementsValues));
        measurementPref.setNegativeButtonText(localizer.getString("cancel"));
        measurementPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateDistanceValues((String) newValue);
                return true;
            }
        });

        clearPref = findPreference("clearFolder");
        clearPref.setTitle(localizer.getString("prefsClearFolderTitle"));
        clearPref.setSummary(localizer.getString("prefsClearFolderSummary"));
        clearPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder clearDialog = new AlertDialog.Builder(Preferences.this);
                clearDialog
                        .setMessage(localizer.getString("prefsClearFolderQuestion"))
                        .setPositiveButton(localizer.getString("prefsClearFolderQuestionYes"),
                                new OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        KeypadMapperFolderCleaner
                                                .cleanFolder(KeypadMapperApplication.getInstance()
                                                        .getKeypadMapperDirectory());
                                        KeypadMapperApplication.getInstance().getMapper()
                                                .setFolderCleared(true);
                                    }
                                })
                        .setNegativeButton(localizer.getString("prefsClearFolderQuestionNo"), null)
                        .create();
                clearDialog.show();
                return true;
            }
        });

        // Keep screen on
        Preference keepScreenOnPref = findPreference("keep_screen_on");
        keepScreenOnPref.setTitle(localizer.getString("prefsScreenOnTitle"));
        keepScreenOnPref.setSummary(localizer.getString("prefsScreenOnSummary"));
        keepScreenOnPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Boolean checked = (Boolean) newValue;
                settings.setKeepScreenOnEnabled(checked);
                menu.onPause();
                menu.onResume();
                return true;
            }
        });

        Preference optimizeLayoutPref = findPreference("layout_optimization_status");
        optimizeLayoutPref.setTitle(localizer.getString("prefsOptimizeLayout"));
        optimizeLayoutPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preferences.this.finish();
                Intent localIntent = new Intent(Preferences.this, Preferences.class);
                Preferences.this.startActivity(localIntent);
                return true;
            }
        });

        PreferenceScreen screen = (PreferenceScreen) findPreference("preferencescreen");
        // initialize the DisplayMetrics object
        DisplayMetrics deviceDisplayMetrics = new DisplayMetrics();

        // populate the DisplayMetrics object with the display characteristics
        getWindowManager().getDefaultDisplay().getMetrics(deviceDisplayMetrics);

        // get the width and height
        int screenWidth = deviceDisplayMetrics.widthPixels;
        int screenHeight = deviceDisplayMetrics.heightPixels;

        int max = Math.max(screenWidth, screenHeight);
        if (max > 480) {
            screen.removePreference(optimizeLayoutPref);
        }

        Preference wifiOnlyPref = findPreference("wifi_only");
        wifiOnlyPref.setTitle(localizer.getString("prefsUseWifiOnlyTitle"));
        wifiOnlyPref.setSummary(localizer.getString("prefsUseWifiOnlySummary"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        boolean handled = CameraHelper.onActivityResult(this, requestCode, resultCode, data);
        if (handled) {
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aboutDialog != null) {
            aboutDialog.dismiss();
            aboutDialog = null;
        }
    }

    @Override
    protected void onPause() {
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());

        menu.onPause();
        mapper.onPause();
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean("aboutDisplayed")) {
            showAboutDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        menu.onResume();
        mapper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("aboutDisplayed", aboutDialog != null);
    }

    /**
     * gets the applicaton version name
     * 
     * @return
     */
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

    /**
     * 
     */
    private void initLanguagePreference() {
        ListPreference langPref = (ListPreference) findPreference("general_language");
        langPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preferences.this.finish();

                Intent localIntent = new Intent(Preferences.this, Preferences.class);
                Preferences.this.startActivity(localIntent);

                return true;
            }
        });
        langPref.setTitle(localizer.getString("prefsLanguageTitle"));
        langPref.setSummary(localizer.getString("prefsLanguageSummary"));
        langPref.setDialogTitle(localizer.getString("prefsLanguageTitle"));
        langPref.setNegativeButtonText(localizer.getString("cancel"));

        String[] codes = localizer.getStringArray("lang_support_codes");
        String[] names = localizer.getStringArray("lang_support_names");

        boolean[] loaded = new boolean[codes.length];
        for (int i = 0; i < loaded.length; i++) {
            loaded[i] = localizer.isLocaleLoaded(codes[i]);
        }

        List<String> loadedCodes = new ArrayList<String>();
        List<String> loadedNames = new ArrayList<String>();

        for (int i = 0; i < codes.length && i < names.length; i++) {
            if (loaded[i]) {
                loadedCodes.add(codes[i]);
                loadedNames.add(names[i]);
            }
        }

        langPref.setEntryValues(loadedCodes.toArray(new String[] {}));
        langPref.setEntries(loadedNames.toArray(new String[] {}));

    }

    private void showAboutDialog() {
        // display About window
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout =
                inflater.inflate(R.layout.about_dialog, (ViewGroup) findViewById(R.id.layout_root));
        TextView text = (TextView) layout.findViewById(R.id.text);
        String aboutText = localizer.getString("about_message");
        text.setText(Html.fromHtml(String.format(aboutText, localizer.getString("app_name"),
                getVersionName())));
        text.setMovementMethod(LinkMovementMethod.getInstance());
        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageDrawable(localizer.getLocalizedImage("enaikoon_logo_frosted"));

        aboutDialog =
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(localizer.getString("app_name"))
                        .setView(layout)
                        .setOnCancelListener(new OnCancelListener() {

                            @Override
                            public void onCancel(DialogInterface dialog) {
                                Log.i("", "");
                            }
                        })
                        .setPositiveButton(localizer.getString("close"),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        aboutDialog = null;
                                    }
                                }).create();

        aboutDialog.show();

        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(localizer
                                .getString("about_logo_url")));
                startActivity(i);
            }
        });
    }

    private void updateDistanceValues(String measurement) {
        ListPreference distancePref = (ListPreference) findPreference("housenumberDistance");
        distancePref.setTitle(localizer.getString("prefsDataPlacementDistance"));
        distancePref.setSummary(localizer.getString("prefsDataPlacementDistanceSummary"));

        distancePref.setDialogTitle(localizer.getString("prefsDataPlacementDistance"));
        String[] distances;
        if (measurement.equalsIgnoreCase("m")) {
            distances =
                    new String[] { localizer.getString("prefsDataDistanceEntries_1"),
                            localizer.getString("prefsDataDistanceEntries_2"),
                            localizer.getString("prefsDataDistanceEntries_5"),
                            localizer.getString("prefsDataDistanceEntries_8"),
                            localizer.getString("prefsDataDistanceEntries_10"),
                            localizer.getString("prefsDataDistanceEntries_15"),
                            localizer.getString("prefsDataDistanceEntries_20"),
                            localizer.getString("prefsDataDistanceEntries_25") };
        } else {
            distances =
                    new String[] { localizer.getString("prefsDataDistanceEntriesFt_1"),
                            localizer.getString("prefsDataDistanceEntriesFt_2"),
                            localizer.getString("prefsDataDistanceEntriesFt_5"),
                            localizer.getString("prefsDataDistanceEntriesFt_8"),
                            localizer.getString("prefsDataDistanceEntriesFt_10"),
                            localizer.getString("prefsDataDistanceEntriesFt_15"),
                            localizer.getString("prefsDataDistanceEntriesFt_20"),
                            localizer.getString("prefsDataDistanceEntriesFt_25") };

        }
        distancePref.setEntries(distances);
        distancePref.setEntryValues(getResources().getTextArray(R.array.prefsDataDistanceValues));
        distancePref.setNegativeButtonText(localizer.getString("cancel"));
    }

}
