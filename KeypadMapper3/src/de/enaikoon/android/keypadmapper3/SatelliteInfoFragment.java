/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;
import de.enaikoon.android.keypadmapper3.view.BarChart;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class SatelliteInfoFragment extends Fragment implements LocationListener, Listener {

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private BarChart chart;

    private TextView accuracyValue;

    private TextView accuracyMeasure;

    private TextView satInView;

    private TextView satInUse;

    private View noGpsReceptionView;

    private View satInfoView;

    private LocationProvider locationProvider = KeypadMapperApplication.getInstance()
            .getLocationProvider();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_satellite_status, container, false);
        chart = (BarChart) view.findViewById(R.id.chart);
        accuracyValue = (TextView) view.findViewById(R.id.accuracyValue);
        accuracyMeasure = (TextView) view.findViewById(R.id.accuracyValueMeasure);
        satInView = (TextView) view.findViewById(R.id.satInView);
        satInUse = (TextView) view.findViewById(R.id.satInUse);
        noGpsReceptionView = view.findViewById(R.id.noSatInfo);
        satInfoView = view.findViewById(R.id.satInfo);
        init(view);
        Log.d("Keypad", "sat info create view");
        return view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.location.GpsStatus.Listener#onGpsStatusChanged(int)
     */
    @Override
    public void onGpsStatusChanged(int event) {
        updateGpsSatellitesInfo(locationProvider.getLastKnownGpsStatus());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onLocationChanged(android.location.
     * Location)
     */
    @Override
    public void onLocationChanged(Location currentLocation) {
        String locationStatus = localizer.getString("satellite_n_a");
        String measurement = "";
        if (currentLocation != null) {
            String mv = KeypadMapperApplication.getInstance().getSettings().getMeasurement();
            
            if (mv.equalsIgnoreCase(KeypadMapperSettings.UNIT_METER)) {
                locationStatus = "" + (int) currentLocation.getAccuracy();
                measurement = localizer.getString("meters_display_unit");
            } else {
                locationStatus =
                        ""
                                + (int) UnitsConverter.convertMetersToFeets(currentLocation
                                        .getAccuracy());
                measurement = localizer.getString("feet_display_unit");
            }
        }
        
        accuracyMeasure.setText(measurement);
        accuracyValue.setText(locationStatus);
    }

    @Override
    public void onPause() {
        
        locationProvider.removeGpsStatusListener(this);
        locationProvider.removeLocationListener(this);
        super.onPause();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String arg0) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onProviderEnabled(java.lang.String)
     */
    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onResume() {
        super.onResume();
        
        Log.d("Keypad", "sat info resume");
        updateGpsSatellitesInfo(locationProvider.getLastKnownGpsStatus());
        this.onLocationChanged(locationProvider.getLastKnownLocation());
        locationProvider.addGpsStatusListener(this);
        locationProvider.addLocationListener(this);
        
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(satInView.getApplicationWindowToken(), 0);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.location.LocationListener#onStatusChanged(java.lang.String,
     * int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void init(View view) {
        ((TextView) view.findViewById(R.id.noGpsReceptionText)).setText(localizer
                .getString("satellite_no_gps"));
        ((TextView) view.findViewById(R.id.inViewText)).setText(localizer
                .getString("satellite_in_view"));
        ((TextView) view.findViewById(R.id.inUseText)).setText(localizer
                .getString("satellite_in_use"));
        ((TextView) view.findViewById(R.id.accuracyTextView)).setText(localizer
                .getString("satellite_accuracy"));
        
        updateGpsSatellitesInfo(locationProvider.getLastKnownGpsStatus());
        onLocationChanged(locationProvider.getLastKnownLocation());
        
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(satInView.getApplicationWindowToken(), 0);
    }

    private void updateGpsSatellitesInfo(GpsStatus gpsStatus) {
        int maxSats = 0;
        int usedSats = 0;
        if (gpsStatus != null) {
            chart.updateData(gpsStatus);

            Iterable<GpsSatellite> gpsSatellites = gpsStatus.getSatellites();
            for (GpsSatellite sat : gpsSatellites) {
                maxSats++;
                if (sat.usedInFix()) {
                    usedSats++;
                }
            }
        }
        satInView.setText("" + maxSats);
        satInUse.setText("" + usedSats);

        if (maxSats == 0) {
            noGpsReceptionView.setVisibility(View.VISIBLE);
            satInfoView.setVisibility(View.GONE);
        } else {
            noGpsReceptionView.setVisibility(View.GONE);
            satInfoView.setVisibility(View.VISIBLE);
        }
    }
}
