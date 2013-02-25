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
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;

/**
 * 
 */
public class KeypadMapperSettings {

    private SharedPreferences preferences;

    public KeypadMapperSettings(Context context) {
        preferences =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if (preferences.getString("general_language", null) == null) {
            String lang = System.getProperty("user.language");
            if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("de")
                    && !lang.equalsIgnoreCase("es") && !lang.equalsIgnoreCase("fr")
                    && !lang.equalsIgnoreCase("el") && !lang.equalsIgnoreCase("ru")) {
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
        if (preferences.getString("housenumberDistance", null) == null) {
            Editor editor = preferences.edit();
            editor.putString("housenumberDistance", "10");
            editor.commit();
        }
    }

    public String getCurrentLanguageCode() {
        return preferences.getString("general_language", "en");
    }

    public double getDistance() {
        if (getMeasurement().equalsIgnoreCase("m")) {
            return Double.valueOf(preferences.getString("housenumberDistance", "10"));
        } else {
            String distTxt = preferences.getString("housenumberDistance", "10");
            double dist = 1;
            if (distTxt.equalsIgnoreCase("1")) {
                dist = UnitsConverter.convertFeetsToMeters(3);
            } else if (distTxt.equalsIgnoreCase("2")) {
                dist = UnitsConverter.convertFeetsToMeters(5);
            } else if (distTxt.equalsIgnoreCase("5")) {
                dist = UnitsConverter.convertFeetsToMeters(15);
            } else if (distTxt.equalsIgnoreCase("8")) {
                dist = UnitsConverter.convertFeetsToMeters(25);
            } else if (distTxt.equalsIgnoreCase("10")) {
                dist = UnitsConverter.convertFeetsToMeters(35);
            } else if (distTxt.equalsIgnoreCase("15")) {
                dist = UnitsConverter.convertFeetsToMeters(50);
            } else if (distTxt.equalsIgnoreCase("20")) {
                dist = UnitsConverter.convertFeetsToMeters(65);
            } else if (distTxt.equalsIgnoreCase("25")) {
                dist = UnitsConverter.convertFeetsToMeters(80);
            }
            return dist;
        }

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
        return preferences.getString("measurement", "m");
    }

    public boolean isKeepScreenOnEnabled() {
        return preferences.getBoolean("keep_screen_on", false);
    }

    public boolean isLayoutOptimizationEnabled() {
        return preferences.getBoolean("layout_optimization_status", false);
    }

    public boolean isWiFiOnlyEnabled() {
        return preferences.getBoolean("wifi_only", false);
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

    public void setMeasurement(String measurement) {
        Editor editor = preferences.edit();
        editor.putString("measurement", measurement);
        editor.commit();
    }
}
