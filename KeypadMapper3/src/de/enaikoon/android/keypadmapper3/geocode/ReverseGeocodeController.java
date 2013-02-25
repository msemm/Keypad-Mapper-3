/**************************************************************************
 * Copyright
 *
 * $Id: ReverseGeocodeController.java 130 2013-01-23 17:04:57Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/geocode/ReverseGeocodeController.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.geocode;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.utils.ConnectivityUtils;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class ReverseGeocodeController {

    private class GeoInfoFetcher extends AsyncTask<Location, Void, ReverseGeocodeResult> {

        /*
         * (non-Javadoc)
         * 
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected ReverseGeocodeResult doInBackground(Location... location) {
            if (!ConnectivityUtils.isDownloadAllowed(parentActivity)) {
                return null;
            }
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(parentActivity
                            .getApplicationContext());
            String lang = preferences.getString("general_language", "en");
            ReverseGeocodeResult result = null;
            try {
                result =
                        reverser.reverseGeocode(location[0].getLatitude(),
                                location[0].getLongitude(), lang);
            } catch (GeoReversingError e) {

                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ReverseGeocodeResult result) {
            super.onPostExecute(result);
            updateAddressInfo(result);
        }

    }

    private ReverseGeocodeService reverser = new ReverseGeocodeService();

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    // private final Logger log =
    // Logger.getLogger(ReverseGeocodeController.class);

    private static final long DELAY_IN_REQUESTS = 10 * 1000L;

    public static final String TAG = "ReverseGeocodeController";

    private TextView infoLine;

    private Location lastLocation;

    private Activity parentActivity;

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public ReverseGeocodeController(Activity parentActivity, TextView infoLine) {
        super();
        this.infoLine = infoLine;
        this.parentActivity = parentActivity;
        String street = KeypadMapperApplication.getInstance().getCachedStreetFromNominatin();
        String postcode = KeypadMapperApplication.getInstance().getCachedPostCodeFromNominatin();
        updateAddressInfo(street, postcode);
    }

    public void onPause() {
        KeypadMapperApplication.getInstance().getLocationProvider()
                .removeLocationListener(locationListener);
    }

    public void onResume() {
        KeypadMapperApplication.getInstance().getLocationProvider()
                .addLocationListener(locationListener);
    }

    synchronized protected void updateLocation(Location newLocation) {
        if (newLocation != null) {
            if (lastLocation == null
                    || (newLocation.getTime() - lastLocation.getTime()) > DELAY_IN_REQUESTS) {
                lastLocation = newLocation;
                new GeoInfoFetcher().execute(newLocation);
            }
        } else {
            updateAddressInfo(null);
        }
    }

    private void updateAddressInfo(ReverseGeocodeResult geoInfo) {
        if (geoInfo != null) {
            String street = geoInfo.getStreetName();
            String postcode = geoInfo.getPostcode();
            updateAddressInfo(street, postcode);
        } else {
            updateAddressInfo(null, null);
        }
    }

    private void updateAddressInfo(String street, String postcode) {
        KeypadMapperApplication.getInstance().setCachedStreetFromNominatin(street);
        KeypadMapperApplication.getInstance().setCachedPostCodeFromNominatin(postcode);
        StringBuilder infoBuilder = new StringBuilder();
        // infoBuilder.append("Nollendorfstrasse, 10777");
        if (street != null) {
            infoBuilder.append(street);
        }
        if (postcode != null) {
            infoBuilder.append(", ");
            infoBuilder.append(postcode);
        }
        // log.info("String result: " + infoBuilder.toString());
        infoLine.setText(infoBuilder.toString());
    }
}
