/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;

/**
 * 
 */
public class KeypadMapperSettings {
    public static final int MAX_VIBRATION_TIME = 500; // 500ms
    public static final int DEFAULT_VIBRATION_TIME = 120; // 120ms
    public static final int VIBRATION_TIME_STEP = 1;
    
    public static final int KEYBOARD_MAX_VIBRATION_TIME = 500; // 500ms
    public static final int KEYBOARD_DEFAULT_VIBRATION_TIME = 50; // 50ms
    public static final int KEYBOARD_VIBRATION_TIME_STEP = 1;
    
    public static final int MAX_USE_COMPASS_AT_SPEED_KMH = 20; // 20 km/h
    public static final int DEFAULT_USE_COMPASS_AT_SPEED_KMH = 5;
    public static final int USE_COMPASS_AT_SPEED_STEP_KMH = 1;
    
    public static final int MAX_USE_COMPASS_AT_SPEED_MPH = 10; // mph
    public static final int DEFAULT_USE_COMPASS_AT_SPEED_MPH = 5;
    public static final int USE_COMPASS_AT_SPEED_STEP_MPH = 1;
    
    public static final float M_PER_SEC_HAS_KM_PER_HOUR = 3.6f;
    public static final float M_PER_SEC_HAS_MILES_PER_HOUR = 2.23694f;
    public static final float MPH_HAS_KMH = 1.60934f;
    public static final float KMH_HAS_MPH = 0.621371f;

    public static final int MAX_HOUSE_NUMBER_DISTANCE_METERS = 25; // meters
    public static final int DEFAULT_HOUSE_NUMBER_DISTANCE_METERS = 10;
    public static final int MAX_HOUSE_NUMBER_DISTANCE_FEET = 80; // feet
    
    /** This value is never displayed. It's only for internal storage and logic. */
    public static final String UNIT_METER = "m";
    /** This value is never displayed. It's only for internal storage and logic. */
    public static final String UNIT_FEET = "ft";
    
    private SharedPreferences preferences;

    public KeypadMapperSettings(Context context) {
        preferences =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (preferences.getString("general_language", null) == null) {
            String lang = System.getProperty("user.language");
            if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("de")
                    && !lang.equalsIgnoreCase("es") && !lang.equalsIgnoreCase("fr")
                    && !lang.equalsIgnoreCase("el") && !lang.equalsIgnoreCase("ru")
                    && !lang.equalsIgnoreCase("nl") && !lang.equalsIgnoreCase("it")
                    && !lang.equalsIgnoreCase("pl")) {
                lang = "en";
            }
            Editor editor = preferences.edit();
            editor.putString("general_language", lang);
            editor.commit();
        }
        if (preferences.getString("list_errorreporting", null) == null) {
            Editor editor = preferences.edit();
            editor.putString("list_errorreporting",
                    context.getString(R.string.options_bugreport_default));
            editor.commit();
        }
        
        try {
            // backward compatibility
            getHouseNumberDistance();
        } catch (Exception e) {
            int defaultValue = DEFAULT_HOUSE_NUMBER_DISTANCE_METERS;
            
            if (getMeasurement().equals(UNIT_FEET)) {
                defaultValue = (int) Math.rint(UnitsConverter.convertMetersToFeets(DEFAULT_HOUSE_NUMBER_DISTANCE_METERS));
            }
            setHouseNumberDistance(defaultValue);
        }
    }

    public String getCurrentLanguageCode() {
        return preferences.getString("general_language", "en");
    }
    
    public void setCurrentLanguageCode(String code) {
        Editor editor = preferences.edit();
        editor.putString("general_language", code);
        editor.commit();
    }

    public void setHouseNumberDistance(int val) {
        if (getMeasurement().equals(UNIT_METER)) {
            // km/h
            if (val < 0 || val > MAX_HOUSE_NUMBER_DISTANCE_METERS) {
                val = MAX_HOUSE_NUMBER_DISTANCE_METERS;
            }
        } else {
            // mph
            if (val < 0 || val > MAX_HOUSE_NUMBER_DISTANCE_FEET) {
                val = MAX_HOUSE_NUMBER_DISTANCE_FEET;
            }
        }
        
        Editor editor = preferences.edit();
        editor.putInt("housenumberDistance", val); //
        editor.commit();
    }
    
    public int getHouseNumberDistance() {
        int defaultValue = DEFAULT_HOUSE_NUMBER_DISTANCE_METERS;
        
        if (getMeasurement().equals(UNIT_FEET)) {
            defaultValue = (int) Math.rint(UnitsConverter.convertMetersToFeets(DEFAULT_HOUSE_NUMBER_DISTANCE_METERS));
        }
        return preferences.getInt("housenumberDistance", defaultValue);
    }

    public String getLastSharedEmail() {
        return preferences.getString("last_shared_email", "");
    }

    public long getLastTimeLaunch() {
        return preferences.getLong("launch_time", 0);
    }
    
    public int getLaunchCount() {
        return preferences.getInt("launch_count", 0);
    }

    public String getMeasurement() {
        return preferences.getString("measurement", UNIT_METER);
    }

    public boolean isKeepScreenOnEnabled() {
        return preferences.getBoolean("keep_screen_on", false);
    }

    public boolean isLayoutOptimizationEnabled() {
        return preferences.getBoolean("layout_optimization_status", false);
    }
    
    public void setLayoutOptimizationEnabled(boolean enabled) {
        Editor editor = preferences.edit();
        editor.putBoolean("layout_optimization_status", enabled);
        editor.commit();
    }

    public boolean isWiFiOnlyEnabled() {
        return preferences.getBoolean("wifi_only", false);
    }
    
    public void setWifiOnly(boolean enabled) {
        Editor editor = preferences.edit();
        editor.putBoolean("wifi_only", enabled);
        editor.commit();
    }

    public void setKeepScreenOnEnabled(boolean enabled) {
        Editor editor = preferences.edit();
        editor.putBoolean("keep_screen_on", enabled);
        editor.commit();
    }

    public void setLastSharedEmail(String email) {
        Editor editor = preferences.edit();
        editor.putString("last_shared_email", email);
        editor.commit();
    }

    public void setLastTimeLaunch(long time) {
        Editor editor = preferences.edit();
        editor.putLong("launch_time", time);
        editor.commit();
    }

    public void setLaunchCount(int count) {
        Editor editor = preferences.edit();
        editor.putInt("launch_count", count);
        editor.commit();
    }
    
    /** 
     * 
     * @param measurement Either UNIT_METER or UNIT_FEET. If the value is invalid, it's UNIT_METER
     */
    public void setMeasurement(String measurement) {
        Editor editor = preferences.edit();
        if (measurement != null && (measurement.equals(UNIT_METER) || measurement.equals(UNIT_FEET))) {
            editor.putString("measurement", measurement);
        } else {
            editor.putString("measurement", UNIT_METER);
        }

        editor.commit();
    }
    
    public void setVibrationTime(int msec) {
        if (msec < 0 || msec >= MAX_VIBRATION_TIME) {
            msec = MAX_VIBRATION_TIME;
        }
        
        Editor editor = preferences.edit();
        editor.putInt("vibration_time", msec);
        editor.commit();
    }
    
    public int getVibrationTime() {
        return preferences.getInt("vibration_time", DEFAULT_VIBRATION_TIME);
    }
    
    public boolean isTurnOffUpdates() {
        return preferences.getBoolean("turnoff_updates", true);
    }
    
    public void setTurnOffUpdates(boolean updates) {
        Editor editor = preferences.edit();
        editor.putBoolean("turnoff_updates", updates);
        editor.commit();
    }
    
    public boolean isRecording() {
        return preferences.getBoolean("recording", true);
    }
    
    public void setRecording(boolean yesno) {
        Editor editor = preferences.edit();
        editor.putBoolean("recording", yesno);
        editor.commit();
    }
    
    public void setUseCompassAtSpeed(int speed) {
        if (getMeasurement().equals(UNIT_METER)) {
            // km/h
            if (speed < 0 || speed > MAX_USE_COMPASS_AT_SPEED_KMH) {
                speed = MAX_USE_COMPASS_AT_SPEED_KMH;
            }
        } else {
            // mph
            if (speed < 0 || speed > MAX_USE_COMPASS_AT_SPEED_MPH) {
                speed = MAX_USE_COMPASS_AT_SPEED_MPH;
            }
        }
        
        Editor editor = preferences.edit();
        editor.putInt("compass_at_speed", speed);
        editor.commit();
    }
    
    public int getUseCompassAtSpeed() {
        // detection here is for returning the right default value
        if (getMeasurement().equals(UNIT_METER)) {
            return preferences.getInt("compass_at_speed", DEFAULT_USE_COMPASS_AT_SPEED_KMH);
        } else {
            return preferences.getInt("compass_at_speed", DEFAULT_USE_COMPASS_AT_SPEED_MPH);
        }
    }
    
    public void setKeyboardVibrationTime(int msec) {
        if (msec < 0 || msec > KEYBOARD_MAX_VIBRATION_TIME) {
            msec = KEYBOARD_MAX_VIBRATION_TIME;
        }
        
        Editor editor = preferences.edit();
        editor.putInt("keyboard_vibration_time", msec);
        editor.commit();
    }
    
    public int getKeyboardVibrationTime() {
        return preferences.getInt("keyboard_vibration_time", KEYBOARD_DEFAULT_VIBRATION_TIME);
    }
    
    public void setWavDir(String path) {
        if (path == null || path.equals("/")) {
            path = "";
        } 
        
        path = path.replace('\\', '/');
        if (!path.endsWith("/") && path.length() > 1) {
            path = path + "/";
        } else if (path.startsWith("/") && path.length() > 1) {
            // remove starting / since we don't want to have file://// as entry in the GPX
            path = path.substring(1);
        } 
        
        Editor editor = preferences.edit();
        editor.putString("wav_dir", path);
        editor.commit();
    }
    
    public String getWavDir() {
        return preferences.getString("wav_dir", "");
    }
    
    public void setLastGpxFile(String file) {
        Editor editor = preferences.edit();
        editor.putString("lastGpxFile", file);
        editor.commit();
    }
    
    public String getLastGpxFile() {
        return preferences.getString("lastGpxFile", null);
    }
    
    public void setLastOsmFile(String file) {
        Editor editor = preferences.edit();
        editor.putString("lastOsmFile", file);
        editor.commit();
    }
    
    public String getLastOsmFile() {
        return preferences.getString("lastOsmFile", null);
    }
    
    public void setCompassAvailable(boolean avail) {
        Editor editor = preferences.edit();
        editor.putBoolean("compass_available", avail);
        editor.commit();
    }
    
    public boolean isCompassAvailable() {
        return preferences.getBoolean("compass_available", false);
    }
    
    public boolean isFirstRun() {
        return preferences.getBoolean("firstRun", true);
    }
    
    public void clearFirstRun() {
        Editor editor = preferences.edit();
        editor.putBoolean("firstRun", false);
        editor.commit();
    }
    
    public String getErrorReporting() {
        return preferences.getString("list_errorreporting", KeypadMapperApplication.getInstance().getString(R.string.options_bugreport_default));
    }
    
    public void setErrorReporting(String s) {
        Editor editor = preferences.edit();
        editor.putString("list_errorreporting", s);
        editor.commit();
    }
}
