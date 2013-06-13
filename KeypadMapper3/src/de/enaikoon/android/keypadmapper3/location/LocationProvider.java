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

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import de.enaikoon.android.inviu.opencellidlibrary.CellIDCollectionService;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.SatelliteInfoFragment;
import de.enaikoon.android.keypadmapper3.utils.GPSDataValidator;
import de.enaikoon.android.keypadmapper3.utils.NMEAHelper;

/**
 * Central class which will pass location objects and
 * gps status objects to all registered listeners.
 */
public class LocationProvider implements LocationListener, Listener, NmeaListener {
    private List<LocationListener> locationListeners;

    private List<Listener> gpsStatusListeners;

    private List<GpsStatus> gpsStatuses;

    private LocationManager locationManager;

    private Location lastKnownLocation;

    private GpsStatus lastKnownGpsStatus;
    
    private LocationListener satInfoListener;
    
    private int lastFixCount;
    private Location lastValidLocation;
    
    public LocationProvider(Context context) {
        locationListeners = new ArrayList<LocationListener>();
        gpsStatusListeners = new ArrayList<Listener>();
        gpsStatuses = new LinkedList<GpsStatus>();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        lastFixCount = 0;
        lastValidLocation = null;
    }

    public void addGpsStatusListener(Listener listener) {
        gpsStatusListeners.add(listener);
        //Log.d("Keypad", "add()... gps status listner size: " + gpsStatusListeners.size());
    }

    synchronized public void addLocationListener(LocationListener listener) {
        // special case
        if (listener instanceof SatelliteInfoFragment) {
            satInfoListener = listener;
            return;
        }
        locationListeners.add(listener);
        
        //Log.d("Keypad", "add()... location listner size: " + locationListeners.size());
    }

    public void stopRequestingUpdates() {
        Log.d("Keypad", "stoprequesting updates");
        CellIDCollectionService service = KeypadMapperApplication.getInstance().getCellIdService();
        if (service != null) {
            service.stopRequestingUpdates();
        }

        locationManager.removeUpdates(this);
        locationManager.removeGpsStatusListener(this);
        locationManager.removeNmeaListener(this);
        lastKnownLocation = null;
        lastKnownGpsStatus = null;
        lastValidLocation = null;
        this.onLocationChanged(null);
        this.onGpsStatusChanged(GpsStatus.GPS_EVENT_STOPPED);
    }
    
    public void startRequestingUpdates() {
        Log.d("Keypad", "startrequesting updates");
        CellIDCollectionService service = KeypadMapperApplication.getInstance().getCellIdService();
        if (service != null) {
            service.startRequestingUpdates();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.addGpsStatusListener(this);
        locationManager.addNmeaListener(this);
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

    @Override
    public void onGpsStatusChanged(int status) {
        switch (status) {
        case GpsStatus.GPS_EVENT_STARTED:
            lastKnownGpsStatus = locationManager.getGpsStatus(null);
            break;
        case GpsStatus.GPS_EVENT_STOPPED:
            lastKnownGpsStatus = null;
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
            
            lastFixCount = usedSats;
            
            if (usedSats == 0) {
                onLocationChanged(null);
            }
            break;
        }
        for (Listener listener : gpsStatusListeners) {
            listener.onGpsStatusChanged(status);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // satellite info has to show everything - other listeners only filtered
        if (satInfoListener != null) {
            satInfoListener.onLocationChanged(location);
        }
        
        if (location != null) {
            if (!GPSDataValidator.validateGPSData(lastValidLocation, location, lastFixCount, NMEAHelper.HDOP, NMEAHelper.VDOP)) {
                // should notify all with null if the location is wrong
                for (LocationListener listener : locationListeners) {
                    listener.onLocationChanged(null);
                }
                return;
            } else {
                lastValidLocation = location;
                GPSDataValidator.setLastRecodedLocation(lastValidLocation);
            }
            
        }
        // if it's null, let it pass
        lastKnownLocation = location;
        for (LocationListener listener : locationListeners) {
            listener.onLocationChanged(location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        for (LocationListener listener : locationListeners) {
            listener.onProviderDisabled(provider);
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        for (LocationListener listener : locationListeners) {
            listener.onProviderEnabled(provider);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        for (LocationListener listener : locationListeners) {
            listener.onStatusChanged(provider, status, extras);
        }
    }

    public void removeGpsStatusListener(Listener listener) {
        gpsStatusListeners.remove(listener);
        //Log.d("Keypad", "remove()... gps status listner size: " + gpsStatusListeners.size());
    }

    synchronized public void removeLocationListener(LocationListener listener) {
        if (listener instanceof SatelliteInfoFragment) {
            satInfoListener = null;
            return;
        }
        
        locationListeners.remove(listener);
        //Log.d("Keypad", "remove()... location listner size: " + locationListeners.size());
    }

    private int getSattelitesInView(GpsStatus status) {
        Iterable<GpsSatellite> gpsSatellites = status.getSatellites();
        int inView = 0;
        for (GpsSatellite sat : gpsSatellites) {
            inView++;
        }
        return inView;
    }

    @Override
    public void onNmeaReceived(long timestamp, String nmea) {
        NMEAHelper.parse(nmea);
    }
    
    public int getLastFixCount() {
        return lastFixCount;
    }
    
    public void setLastValidLocation(Location loc) {
        lastValidLocation = loc;
    }
    
    public Location getLastValidLocation() {
        return lastValidLocation;
    }
    /** Sometimes reference to Location service can be lost. 
     * This is called to renew it when activity is run from
     * notification. This will be done only if the recording is active.
     */
    public void refreshReferenceToGps() {
        if (KeypadMapperApplication.getInstance().getSettings().isRecording()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.addGpsStatusListener(this);
            locationManager.addNmeaListener(this);
        }
    }
}
