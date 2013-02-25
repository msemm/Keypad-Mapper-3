/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;

/**
 * 
 */
public class ConnectivityUtils {

    public static boolean isDownloadAllowed(Context context) {
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        boolean onlyWifi = settings.isWiFiOnlyEnabled();
        boolean isWifiConnected = isWifiConnected(context);
        return !onlyWifi || isWifiConnected;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            return true;
        }
        return false;
    }
}
