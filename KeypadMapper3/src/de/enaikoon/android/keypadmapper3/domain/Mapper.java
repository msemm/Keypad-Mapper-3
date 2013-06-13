/**************************************************************************
 * Copyright
 *
 * $Id: Mapper.java 178 2013-02-18 12:41:07Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/domain/Mapper.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import org.osm.keypadmapper2.KeypadMapper2Activity;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.LocationNotAvailableException;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.SerialExecutor;
import de.enaikoon.android.keypadmapper3.writers.DataWritingManager;
import de.enaikoon.android.keypadmapper3.writers.OsmWriter;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class Mapper implements LocationListener {

    private boolean folderCleared;

    private Context context;

    private Location freezedLocation;

    private Location currentLocation;

    private List<FreezedLocationListener> freezedLocationListeners;

    private List<UndoAvailabilityListener> undoListeners;

    private List<Address> addresses;

    private boolean undoAvailable = false;

    private final Localizer localizer;

    private List<Trackpoint> trackpoints;

    private Address currentAddress;
    
    private int houseNumberCount;
    
    private static volatile boolean appending;
    
    private DataWritingManager writingManager = new DataWritingManager(KeypadMapperApplication
            .getInstance().getKeypadMapperDirectory()) {

        @Override
        public void showDialogFatalError(final String message) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    for (NotificationListener listener : notificationListeners) {
                        listener.notifyAboutFatalError(message);
                    }
                }
            });
        }
    };

    private Set<NotificationListener> notificationListeners = new HashSet<NotificationListener>();

    private Handler handler;

    private SerialExecutor gpxExecutor;
    private SerialExecutor osmExecutor;
    
    private Location photoLocation;

    public Mapper(Context context) {
        this.context = context;
        handler = new Handler();
        gpxExecutor = new SerialExecutor(Executors.newFixedThreadPool(1));
        osmExecutor = new SerialExecutor(Executors.newFixedThreadPool(1));
        freezedLocationListeners = new ArrayList<FreezedLocationListener>();
        undoListeners = new ArrayList<UndoAvailabilityListener>();
        localizer = KeypadMapperApplication.getInstance().getLocalizer();
        trackpoints = new CopyOnWriteArrayList<Trackpoint>();
        addresses = new CopyOnWriteArrayList<Address>();
        currentAddress = new Address();
        writingManager.initBasename();
    }

    public void addFreezedLocationListener(FreezedLocationListener freezedLocationListener) {
        freezedLocationListeners.remove(freezedLocationListener);
        freezedLocationListeners.add(freezedLocationListener);
    }

    public void addNotificationListener(NotificationListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener could not be null");
        }
        notificationListeners.add(listener);
    }

    public void addUndoListener(UndoAvailabilityListener undoListener) {
        undoListeners.remove(undoListener);
        undoListeners.add(undoListener);
    }

    public void clearAllCurrentData() {
        addresses.clear();
        houseNumberCount = 0;
        trackpoints.clear();
        writingManager.initBasename();
        setUndoAvailable(false);
    }

    public void clearFreezedLocation() {
        setFreezedLocation(null);
    }

    public void freezeUnfreezeLocation(Activity activity) {
        if (freezedLocation == null) {
            if (!KeypadMapperApplication.getInstance().getSettings().isRecording()) {
                Toast.makeText(activity, localizer.getString("error_not_recording"), Toast.LENGTH_LONG).show();
                return;
            }
            setFreezedLocation(getCurrentLocation());
            if (freezedLocation == null) {
                String message = localizer.getString("no_location");
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        } else {
            setFreezedLocation(null);
        }
    }

    public String getBasename() {
        return writingManager.getBasename();
    }

    public Address getCurrentAddress() {
        return new Address(currentAddress);
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Location getFreezedLocation() {
        return freezedLocation;
    }

    public int getHouseNumberCount() {
        return houseNumberCount;
    }

    public String[] getLast3HouseNumbers() {
        List<String> last3Numbers = new ArrayList<String>();
        int i = addresses.size();
        while (last3Numbers.size() < 3 && i > 0) {
            i--;
            String houseNumber = addresses.get(i).getNumber();
            if (!TextUtils.isEmpty(houseNumber)) {
                last3Numbers.add(houseNumber);
            }
        }
        return last3Numbers.toArray(new String[] {});
    }

    public Location getPhotoLocation() {
        return photoLocation;
    }

    public boolean isFolderCleared() {
        return folderCleared;
    }

    public boolean isUndoAvailable() {
        return undoAvailable;
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
        // it's okay to be null
        setCurrentLocation(location);
        
        if (location != null && location.getLatitude() != 0.0 && location.getLongitude() != 0.0  && location.getAccuracy() < 500 /* m */) {
            Trackpoint point =
                    new Trackpoint(location.getLatitude(), location.getLongitude(),
                            location.getTime(), null, location.getSpeed());
            if (location.hasAltitude()) {
                point.setAltitude(location.getAltitude());
            }
            trackpoints.add(point);
        }
    }
    
    public void addWavTrackpoint(Location location, String filename) {
        if (location != null && filename != null
                && location.getLatitude() != 0.0 && location.getLongitude() != 0.0
                && location.getAccuracy() < 500 /* m */) {
            Trackpoint point = new Trackpoint(location.getLatitude(), 
                                              location.getLongitude(),
                                              location.getTime(), 
                                              filename, 0);
            if (location.hasAltitude()) {
                point.setAltitude(location.getAltitude());
            }
            trackpoints.add(point);
        }
    }

    public void onPause() {
        KeypadMapperApplication.getInstance().getLocationProvider().removeLocationListener(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String provider) {
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

    public void onResume() {
        KeypadMapperApplication.getInstance().getLocationProvider().addLocationListener(this);
        writingManager.checkExternalStorageStatus();
        writingManager.initKeypadMapperFolder();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.location.LocationListener#onStatusChanged(java.lang.String,
     * int, android.os.Bundle)
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {
    }

    public void removeFreezedLocationListener(FreezedLocationListener freezedLocationListener) {
        freezedLocationListeners.remove(freezedLocationListener);
    }

    public void removeNotificationListener(NotificationListener listener) {
        notificationListeners.remove(listener);
    }

    public void removeUndoListener(UndoAvailabilityListener undoListener) {
        undoListeners.remove(undoListener);
    }
    
    public void stopRecording() {
        stopAppendingTrackpoints();
        clearAllCurrentData();
        clearFreezedLocation();
        setCurrentLocation(null);
        KeypadMapperApplication.getInstance().checkLastGpxFile();
        KeypadMapperApplication.getInstance().getSettings().setLastGpxFile(null);
        KeypadMapperApplication.getInstance().checkLastOsmFile();
        // reset last file names
        KeypadMapperApplication.getInstance().getSettings().setLastOsmFile(null);
        KeypadMapperApplication.getInstance().getLocationProvider().removeLocationListener(this);
    }
    
    public void startRecording() {
        clearAllCurrentData();
        KeypadMapperApplication.getInstance().getLocationProvider().addLocationListener(this);
        writingManager.initGpx();
        writingManager.initOsm();
        startAppendingTrackpoints();
    }

    public void reset() {
        writingManager.initBasename();
        addresses.clear();
        houseNumberCount = 0;
        setUndoAvailable(false);
    }

    public void saveCurrentAddress(double forward, double left) {
        if (!TextUtils.isEmpty(currentAddress.getNumber())
                || !TextUtils.isEmpty(currentAddress.getNotes())) {
            forward /= 111111;
            left /= 111111;

            Location locationToSave = getCurrentLocation();
            
            if (getFreezedLocation() != null) {
                locationToSave = getFreezedLocation();
                clearFreezedLocation();
            }

            if (locationToSave == null) {
                throw new LocationNotAvailableException("Location is not available");
            }
            double lat = 0.0d;
            double lon = 0.0d;
            
            double speed = locationToSave.getSpeed();
            KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
            // convert meters per second to km/h or mph
            if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
                // meters
                speed = speed * KeypadMapperSettings.M_PER_SEC_HAS_KM_PER_HOUR;
            } else {
                // feet
                speed = speed * KeypadMapperSettings.M_PER_SEC_HAS_MILES_PER_HOUR;
            }
            
            Log.i("KeypadMapper", "Speed at trackpoint: " + speed + " km/h");
            if (settings.isCompassAvailable() &&
                    (((speed > 0.0 && settings.getUseCompassAtSpeed() > 0.0
                       && speed < settings.getUseCompassAtSpeed()) || 
                       (speed == 0.0 && settings.getUseCompassAtSpeed() > 0.0)))) {

                float azimuth = (float) Math.toDegrees(KeypadMapper2Activity.azimuth); // orientation
                if (azimuth < 0.0f) {
                    azimuth = (azimuth + 360);
                }
                float bearing = azimuth;
               
                lat =
                        (locationToSave.getLatitude()
                                + Math.sin(Math.PI / 180 * bearing) * left + Math
                                .cos(Math.PI / 180 * bearing) * forward);
                lon =
                        (locationToSave.getLongitude() + (Math.sin(Math.PI / 180
                                * bearing)
                                * forward - Math.cos(Math.PI / 180 * bearing)
                                * left)
                                / Math.cos(Math.PI / 180 * locationToSave.getLatitude()));
                Log.i("KeypadMapper", "Used compass calculation: lat=" + lat + " lon=" + lon);
            } else {
                // use bearing
                lat =
                        (locationToSave.getLatitude()
                                + Math.sin(Math.PI / 180 * locationToSave.getBearing()) * left + Math
                                .cos(Math.PI / 180 * locationToSave.getBearing()) * forward);
                lon =
                        (locationToSave.getLongitude() + (Math.sin(Math.PI / 180
                                * locationToSave.getBearing())
                                * forward - Math.cos(Math.PI / 180 * locationToSave.getBearing())
                                * left)
                                / Math.cos(Math.PI / 180 * locationToSave.getLatitude()));
                Log.i("KeypadMapper", "Used normal calculation: lat=" + lat + " lon=" + lon);
            }
            
            Address addressToSave = new Address(currentAddress);
            addressToSave.setLocation(lat, lon);
            
            // make sure addresses are 4 or less
            if (addresses.size() > 4) {
                addresses.remove(0);
            }
            
            houseNumberCount++;
            addresses.add(addressToSave);
            setUndoAvailable(true);
            
            saveAddress(addressToSave);

            currentAddress.setNumber("");
            currentAddress.setNotes("");
            currentAddress.setHousename("");
        }
    }
    
    public void setCurrentAddress(Address currentAddress) {
        if (currentAddress == null) {
            throw new IllegalArgumentException("You could not set null instead of address");
        }
        this.currentAddress = currentAddress;
    }
    
   
    public void setFolderCleared(boolean folderCleared) {
        this.folderCleared = folderCleared;
    }

    public void setPhotoLocation(Location photoLocation) {
        this.photoLocation = photoLocation;
    }

    public void undo() {
        if (!addresses.isEmpty() && isUndoAvailable()) {
            Address lastNode = addresses.remove(addresses.size() - 1);
            houseNumberCount --;
            deleteLastNode();
            setUndoAvailable(false);
        }
    }

    protected void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

    protected void setUndoAvailable(boolean undoAvailable) {
        this.undoAvailable = undoAvailable;
        for (UndoAvailabilityListener listener : undoListeners) {
            listener.undoStateChanged(undoAvailable);
        }
    }
    
    private void deleteLastNode() {
        osmExecutor.execute(new Runnable() {
            @Override
            public void run() {
                OsmWriter osm = new OsmWriter();
                osm.deleteLastNode();
            } 
        });
    }

    private void saveAddress(final Address a) {
        osmExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (writingManager != null) {
                    writingManager.saveAddress(a);
                }
            }
        });
    }
    
    private void startAppendingTrackpoints() {
        appending = true;
        gpxExecutor.execute(new Runnable() {
            
            @Override
            public void run() {
                while (appending) {
                    if (trackpoints != null && writingManager != null) {
                        List<Trackpoint> tpList = new ArrayList<Trackpoint>();
                        tpList.addAll(trackpoints);
                        trackpoints.clear();
                        writingManager.saveTrackpoints(tpList, true);
                    }
                    
                    try {
                        Thread.sleep(5000L);
                    } catch (Exception e) {}
                }
            }
        });
    }
    
    private void stopAppendingTrackpoints() {
        appending = false;
    }

    private void setFreezedLocation(Location freezedLocation) {
        this.freezedLocation = freezedLocation;
        for (FreezedLocationListener freezedLocationListener : freezedLocationListeners) {
            freezedLocationListener.onFreezedLocationChanged(freezedLocation);
        }
    }
}
