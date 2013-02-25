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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.LocationNotAvailableException;
import de.enaikoon.android.keypadmapper3.file.KeypadMapperFolderCleaner;
import de.enaikoon.android.keypadmapper3.utils.SerialExecutor;
import de.enaikoon.android.keypadmapper3.writers.DataWritingManager;
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

    private SerialExecutor executor;

    private Location photoLocation;

    public Mapper(Context context) {
        this.context = context;
        handler = new Handler();
        executor = new SerialExecutor(Executors.newFixedThreadPool(1));
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
        trackpoints.clear();
        writingManager.initBasename();
        setUndoAvailable(false);
    }

    public void clearFreezedLocation() {
        setFreezedLocation(null);
    }

    public void freezeUnfreezeLocation() {
        if (freezedLocation == null) {
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
        // Location tmp = new Location("gps");
        // tmp.setAccuracy(33);
        // tmp.setLatitude(10);
        // tmp.setLongitude(20);
        // currentLocation = tmp;
        return currentLocation;
    }

    public Location getFreezedLocation() {
        return freezedLocation;
    }

    public int getHouseNumberCount() {
        int count = 0;
        for (Address address : addresses) {
            if (TextUtils.isEmpty(address.getNumber())) {
                count++;
            }
        }
        return addresses.size() - count;
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
        setCurrentLocation(location);
        if (location != null && location.getLatitude() != 0.0 && location.getLongitude() != 0.0
                && location.getAccuracy() < 500 /* m */) {
            Trackpoint point =
                    new Trackpoint(location.getLatitude(), location.getLongitude(),
                            location.getTime());
            if (location.hasAltitude()) {
                point.setAltitude(location.getAltitude());
            }
            trackpoints.add(point);
        }
    }

    public void onPause() {
        saveAll();
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

    public void reset() {
        writingManager.initBasename();
        trackpoints.clear();
        addresses.clear();
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
            double lat =
                    (locationToSave.getLatitude()
                            + Math.sin(Math.PI / 180 * locationToSave.getBearing()) * left + Math
                            .cos(Math.PI / 180 * locationToSave.getBearing()) * forward);
            double lon =
                    (locationToSave.getLongitude() + (Math.sin(Math.PI / 180
                            * locationToSave.getBearing())
                            * forward - Math.cos(Math.PI / 180 * locationToSave.getBearing())
                            * left)
                            / Math.cos(Math.PI / 180 * locationToSave.getLatitude()));

            Address addressToSave = new Address(currentAddress);
            addressToSave.setLocation(lat, lon);
            addresses.add(addressToSave);
            setUndoAvailable(true);

            saveAll();

            currentAddress.setNumber("");
            currentAddress.setNotes("");
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
            addresses.remove(addresses.size() - 1);
            saveAll();
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

    private void saveAll() {
        executor.execute(new Runnable() {

            @Override
            public void run() {
                writingManager.saveAddresses(addresses);
                writingManager.saveTrackpoints(trackpoints);
                KeypadMapperFolderCleaner.cleanFolderFromEmptyFiles(KeypadMapperApplication
                        .getInstance().getKeypadMapperDirectory());
            }
        });
    }

    private void setFreezedLocation(Location freezedLocation) {
        this.freezedLocation = freezedLocation;
        for (FreezedLocationListener freezedLocationListener : freezedLocationListeners) {
            freezedLocationListener.onFreezedLocationChanged(freezedLocation);
        }
    }
}
