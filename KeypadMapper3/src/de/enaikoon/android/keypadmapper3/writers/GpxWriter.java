package de.enaikoon.android.keypadmapper3.writers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.TimeZone;

import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;

public class GpxWriter {
    private RandomAccessFile gpxFile;
    
    private static final String GPX_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                             "<gpx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                             "\txmlns=\"http://www.topografix.com/GPX/1/0\"\n" +
                                             "\txsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"\n" +
                                             "\tversion=\"1.0\"\n" +
                                             "\tcreator=\"" + KeypadMapperApplication.getInstance().getLocalizer().getString("app_name") + "\">\n" +
                                             "\n" +
                                             "\t<trk>\n" +
                                             "\t\t<trkseg>\n";
    
    private static final String GPX_FOOTER = "\t\t</trkseg>\n" +
                                             "\t</trk>\n" +
                                             "</gpx>\n";
    
    private KeypadMapperSettings settings;

    public GpxWriter() {
        settings = KeypadMapperApplication.getInstance().getSettings();
    }

    private String getNewFilename() {
        String path = KeypadMapperApplication.getInstance().getKeypadMapperDirectory().getAbsolutePath();

        Calendar cal = Calendar.getInstance();
        return path + "/" + String.format("%tF_%tH-%tM-%tS", cal, cal, cal, cal) + ".gpx";
    }
    
    public void addTrackpoint(double lat, double lon, long time, String filename, double speed) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString = String.format("%tFT%tTZ", cal, cal);
        String wavPath = settings.getWavDir();
        
        if (filename == null) {
            gpxFile.writeBytes("\t\t\t<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
            gpxFile.writeBytes("\t\t\t\t<time>" + timeString + "</time>\n");
            gpxFile.writeBytes("\t\t\t</trkpt>\n");
        } else {
            // according to http://josm.openstreetmap.de/wiki/Help/AudioMapping/SeparateClips
            // close current track segment and track
            gpxFile.writeBytes("\t\t</trkseg>\n");
            gpxFile.writeBytes("\t</trk>\n");
            // add way point
            gpxFile.writeBytes("\t<wpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
            gpxFile.writeBytes("\t\t<time>" + timeString + "</time>\n");
            String extracted = filename.substring(filename.lastIndexOf('/') + 1);
            gpxFile.writeBytes("\t\t<link href=\"file:///" + wavPath + extracted + "\"><text>" + extracted + "</text></link>\n");
            gpxFile.writeBytes("\t</wpt>\n");
            // open another track segment 
            gpxFile.writeBytes("\t<trk>\n");
            gpxFile.writeBytes("\t\t<trkseg>\n");
        }
    }

   
    public void addTrackpoint(double lat, double lon, long time, double ele, String filename, double speed) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString = String.format("%tFT%tTZ", cal, cal);
        String wavPath = settings.getWavDir();
        
        if (filename == null) {
            gpxFile.writeBytes("\t\t\t<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
            gpxFile.writeBytes("\t\t\t\t<time>" + timeString + "</time>\n");
            gpxFile.writeBytes("\t\t\t\t<ele>" + ele + "</ele>\n");
            gpxFile.writeBytes("\t\t\t</trkpt>\n");
        } else {
            // according to http://josm.openstreetmap.de/wiki/Help/AudioMapping/SeparateClips
            // close current track segment and track
            gpxFile.writeBytes("\t\t</trkseg>\n");
            gpxFile.writeBytes("\t</trk>\n");
            // add way point
            gpxFile.writeBytes("\t<wpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
            gpxFile.writeBytes("\t\t<time>" + timeString + "</time>\n");
            String extracted = filename.substring(filename.lastIndexOf('/') + 1);
            gpxFile.writeBytes("\t\t<link href=\"file:///" + wavPath + extracted + "\"><text>" + extracted + "</text></link>\n");
            gpxFile.writeBytes("\t\t\t<ele>" + ele + "</ele>\n");
            gpxFile.writeBytes("\t</wpt>\n");
            // open another track segment 
            gpxFile.writeBytes("\t<trk>\n");
            gpxFile.writeBytes("\t\t<trkseg>\n");
        }
    }

    /**
     * Closes this GPX file. GPX file already HAS footer written.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void close() throws IOException { 
        gpxFile.writeBytes(GPX_FOOTER);
        gpxFile.close();
    }

    /**
     * 
     * @param append if true, last file name is used to store trackpoints
     * @throws IOException
     */
    public void openGpxWriter(boolean append) throws Exception {
        if (!append) {
            // create/overwrite file
            String filename = getNewFilename();
            gpxFile = new RandomAccessFile(filename, "rw");
            gpxFile.writeBytes(GPX_HEADER);
        
            settings.setLastGpxFile(filename);
        } else {
            // append to existing (and initialised) file
            gpxFile = new RandomAccessFile(settings.getLastGpxFile(), "rw");
            // seek to end - footer.length
            gpxFile.seek(gpxFile.length() - GPX_FOOTER.getBytes().length);
        }
    }
    
    public static int getEmptyFileSize() {
        return GPX_HEADER.getBytes().length + GPX_FOOTER.getBytes().length;
    }
}
