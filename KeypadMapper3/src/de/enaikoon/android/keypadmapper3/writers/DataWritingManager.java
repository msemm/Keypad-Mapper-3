/**************************************************************************
 * TODO Copyright
 *
 * $Id: DataWritingManager.java 137 2013-01-25 14:39:08Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/writers/DataWritingManager.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Environment;
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

    synchronized public void saveAddresses(List<Address> addresses) {
        OsmWriter osmWriter =
                new OsmWriter(kpmFolder + "/" + basename + ".osm~", localizer.getString("app_name"));
        try {

            osmWriter.openOsmWriter(false);

            for (Address address : addresses) {
                osmWriter.addNode(address.getLatitude(), address.getLongitude(),
                        convertToTags(address));
            }

            osmWriter.flush();

        } catch (FileNotFoundException e) {
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } catch (IOException exception) {
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } finally {
            if (osmWriter != null) {
                try {
                    osmWriter.close();
                } catch (IOException e) {
                    showDialogFatalError(localizer.getString("errorFileOpen"));
                }
            }
        }
        File tmpFile = new File(kpmFolder + "/" + basename + ".osm~");
        tmpFile.renameTo(new File(kpmFolder + "/" + basename + ".osm"));

    }

    synchronized public void saveTrackpoints(List<Trackpoint> trackpoints) {
        GpxWriter trackWriter =
                new GpxWriter(kpmFolder + "/" + basename + ".gpx", localizer.getString("app_name"));
        try {
            trackWriter.openGpxWriter(false);

            for (Trackpoint trackpoint : trackpoints) {
                if (trackpoint.getAltitude() != Double.MIN_VALUE) {
                    trackWriter.addTrackpoint(trackpoint.getLatitude(), trackpoint.getLongitude(),
                            trackpoint.getTime(), trackpoint.getAltitude());
                } else {
                    trackWriter.addTrackpoint(trackpoint.getLatitude(), trackpoint.getLongitude(),
                            trackpoint.getTime());
                }
            }

            trackWriter.flush();

        } catch (FileNotFoundException e) {
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } catch (IOException exception) {
            showDialogFatalError(localizer.getString("errorFileOpen"));
        } finally {
            if (trackWriter != null) {
                try {
                    trackWriter.close();
                } catch (IOException e) {
                    showDialogFatalError(localizer.getString("errorFileOpen"));
                }
            }
        }
        File tmpFile = new File(kpmFolder + "/" + basename + ".gpx~");
        tmpFile.renameTo(new File(kpmFolder + "/" + basename + ".gpx"));
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    abstract public void showDialogFatalError(String message);

    protected Map<String, String> convertToTags(Address address) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", address.getNotes());
        map.put("addr:housenumber", address.getNumber());
        map.put("addr:housename", address.getHousename());
        map.put("addr:street", address.getStreet());
        map.put("addr:postcode", address.getPostcode());
        map.put("addr:city", address.getCity());
        map.put("addr:country", address.getCountryCode());
        return map;
    }
}
