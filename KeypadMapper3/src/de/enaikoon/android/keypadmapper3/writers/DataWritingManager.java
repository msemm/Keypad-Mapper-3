/**************************************************************************
 * TODO Copyright
 *
 * $Id: DataWritingManager.java 137 2013-01-25 14:39:08Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/writers/DataWritingManager.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.writers;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.os.Environment;
import android.util.Log;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.domain.Address;
import de.enaikoon.android.keypadmapper3.domain.Trackpoint;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
abstract public class DataWritingManager {

    private String basename;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private File kpmFolder;

    public DataWritingManager(File kpmFolder) {
        this.kpmFolder = kpmFolder;
    }

    public void checkExternalStorageStatus() {
        // check for external storage
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            // We can read and write the media
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            // We can only read the media
            showDialogFatalError(localizer.getString("errorStorageRO"));
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need to know is we can neither read nor write
            showDialogFatalError(localizer.getString("errorStorageUnavailable"));
        }
    }

    public String getBasename() {
        if (basename == null) {
            initBasename();
        }
        return basename;
    }

    public void initBasename() {
        Calendar cal = Calendar.getInstance();
        basename = String.format("%tF_%tH-%tM-%tS", cal, cal, cal, cal);
    }

    public void initKeypadMapperFolder() {
        if (!kpmFolder.exists()) {
            if (!kpmFolder.mkdir()) {
                showDialogFatalError(localizer.getString("FolderCreationFailed"));
            }
        }
    }

    synchronized public void saveAddress(final Address address) {
        OsmWriter osmWriter = new OsmWriter();
        try {
            osmWriter.openOsmWriter(true);
            osmWriter.addNode(address.getLatitude(), address.getLongitude(), convertToTags(address));
            osmWriter.close();
        } catch (Exception e) {
            Log.e("KeypadMapper", "", e);
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } 
    }
   
    synchronized public void saveTrackpoints(List<Trackpoint> trackpoints, boolean append) {
        GpxWriter trackWriter = new GpxWriter();
        try {
            trackWriter.openGpxWriter(append);

            for (Trackpoint trackpoint : trackpoints) {
                if (trackpoint.getAltitude() != Double.MIN_VALUE) {
                    trackWriter.addTrackpoint(trackpoint.getLatitude(), trackpoint.getLongitude(),
                                              trackpoint.getTime(), trackpoint.getAltitude(), 
                                              trackpoint.getFilename(), trackpoint.getSpeed());
                } else {
                    trackWriter.addTrackpoint(trackpoint.getLatitude(), trackpoint.getLongitude(), trackpoint.getTime(), 
                                              trackpoint.getFilename(), trackpoint.getSpeed());
                }
            }
            
        } catch (Exception e) {
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } finally {
            if (trackWriter != null) {
                try {
                    trackWriter.close();
                } catch (Exception e) {
                    showDialogFatalError(localizer.getString("errorFileOpen"));
                }
            }
        }
    }
    
    /**
     * Only creates a new GPX file.
     */
    public void initGpx() {
        GpxWriter tw = new GpxWriter();
        try {
            boolean append = KeypadMapperApplication.getInstance().getSettings().isRecording() 
                             && KeypadMapperApplication.getInstance().getSettings().getLastGpxFile() != null;
            
            tw.openGpxWriter(append);
            tw.close();
        } catch (Exception e) {
            Log.e("Keypad", "", e);
            showDialogFatalError(localizer.getString("errorFileOpen"));
        }
    }
    
    public void initOsm() {
        OsmWriter osm = new OsmWriter();
        try {
            boolean append = KeypadMapperApplication.getInstance().getSettings().isRecording() 
                    && KeypadMapperApplication.getInstance().getSettings().getLastOsmFile() != null;
            osm.openOsmWriter(append);
            osm.close();
        } catch (Exception e) {
            Log.e("Keypad", "", e);
            showDialogFatalError(localizer.getString("errorFileOpen"));
        }
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    abstract public void showDialogFatalError(String message);

    protected Map<String, String> convertToTags(Address address) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", address.getNotes());
        // remove "L/R/F: " from start of the string
        String left = localizer.getString("buttonLeft") + ": ";
        String front = localizer.getString("buttonFront") + ": ";
        String right = localizer.getString("buttonRight") + ": ";
        String houseNumber = address.getNumber();
        if (houseNumber.startsWith(left)) {
            houseNumber = houseNumber.replaceFirst(left, "");
        } else if (houseNumber.startsWith(front)) {
            houseNumber = houseNumber.replaceFirst(front, "");
        } else if (houseNumber.startsWith(right)) {
            houseNumber = houseNumber.replaceFirst(right, "");
        }
        map.put("addr:housenumber", houseNumber);
        map.put("addr:housename", address.getHousename());
        map.put("addr:street", address.getStreet());
        map.put("addr:postcode", address.getPostcode());
        map.put("addr:city", address.getCity());
        map.put("addr:country", address.getCountryCode());
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        map.put("survey:date", new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
        
        return map;
    }
}
