/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.location;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;

/**
 * 
 */
public class LocationProvider implements LocationListener, Listener {

    public static final int FIX_VALID_TIME = 20 * 1000;

    public static final int TURN_OFF_DELAY = 10 * 1000;

    private List<LocationListener> locationListeners;

    private List<Listener> gpsStatusListeners;

    private List<GpsStatus> gpsStatuses;

    private LocationManager locationManager;

    private Location lastKnownLocation;

    private Timer fixValidationTimer;

    private long lastUpateTime;

    private GpsStatus lastKnownGpsStatus;

    private Timer turnOffTimer;

    private Handler handler;

    // private final Logger log = Logger.getLogger(LocationProvider.class);

    public LocationProvider(Context context) {
        locationListeners = new ArrayList<LocationListener>();
        gpsStatusListeners = new ArrayList<Listener>();
        gpsStatuses = new LinkedList<GpsStatus>();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler();
    }

    public void addGpsStatusListener(Listener listener) {
        gpsStatusListeners.remove(listener);
        gpsStatusListeners.add(listener);
    }

    synchronized public void addLocationListener(LocationListener listener) {
        locationListeners.remove(listener);
        locationListeners.add(listener);
        if (locationListeners.size() == 1) {
            if (turnOffTimer != null) {
                turnOffTimer.cancel();
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.addGpsStatusListener(this);
        }
    }

    public GpsStatus getLastKnownGpsStatus() {
        return lastKnownGpsStatus;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public boolean isLocationServiceEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.location.GpsStatus.Listener#onGpsStatusChanged(int)
     */
    @Override
    public void onGpsStatusChanged(int status) {
        switch (status) {
        case GpsStatus.GPS_EVENT_STARTED:
            break;
        case GpsStatus.GPS_EVENT_STOPPED:
            break;
        case GpsStatus.GPS_EVENT_FIRST_FIX:
            break;
        case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            lastKnownGpsStatus = locationManager.getGpsStatus(null);
            gpsStatuses.add(lastKnownGpsStatus);

            if (gpsStatuses.size() > 3) {
                gpsStatuses.remove(0);
            }

            for (GpsStatus tryStatus : gpsStatuses) {
                if (getSattelitesInView(lastKnownGpsStatus) < getSattelitesInView(tryStatus)) {
                    lastKnownGpsStatus = tryStatus;
                }
            }

            int usedSats = 0;
            if (lastKnownGpsStatus != null) {
                Iterable<GpsSatellite> gpsSatellites = lastKnownGpsStatus.getSatellites();
                for (GpsSatellite sat : gpsSatellites) {
                    if (sat.usedInFix()) {
                        usedSats++;
                    }
                }
            }
            if (usedSats == 0) {
                onLocationChanged(null);
            }
            break;
        }
        for (Listener listener : gpsStatusListeners) {
            listener.onGpsStatusChanged(status);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onLocationChanged(android.location.
     * Location)
     */
    @Override
    public void onLocationChanged(Location location) {
        // log.info(location);
        lastKnownLocation = location;
        lastUpateTime = System.currentTimeMillis();
        if (lastKnownLocation != null) {
            if (fixValidationTimer != null) {
                fixValidationTimer.cancel();
            }
            fixValidationTimer = new Timer();
            fixValidationTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            onLocationChanged(null);
                        }
                    });
                }
            }, FIX_VALID_TIME);
        }
        for (LocationListener listener : locationListeners) {
            listener.onLocationChanged(location);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String provider) {
        for (LocationListener listener : locationListeners) {
            listener.onProviderDisabled(provider);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onProviderEnabled(java.lang.String)
     */
    @Override
    public void onProviderEnabled(String provider) {
        for (LocationListener listener : locationListeners) {
            listener.onProviderEnabled(provider);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.location.LocationListener#onStatusChanged(java.lang.String,
     * int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        for (LocationListener listener : locationListeners) {
            listener.onStatusChanged(provider, status, extras);
        }
    }

    public void removeGpsStatusListener(Listener listener) {
        gpsStatusListeners.remove(listener);
    }

    synchronized public void removeLocationListener(LocationListener listener) {
        locationListeners.remove(listener);
        if (locationListeners.size() == 0) {
            turnOffTimer = new Timer();
            turnOffTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    removeUpdates();
                }
            }, TURN_OFF_DELAY);
        }
    }

    private int getSattelitesInView(GpsStatus status) {
        Iterable<GpsSatellite> gpsSatellites = status.getSatellites();
        int inView = 0;
        for (GpsSatellite sat : gpsSatellites) {
            inView++;
        }
        return inView;
    }

    synchronized private void removeUpdates() {
        if (locationListeners.size() == 0) {
            locationManager.removeUpdates(this);
        }
    }
}
