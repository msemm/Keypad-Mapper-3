package de.enaikoon.android.keypadmapper3.writers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.TimeZone;

public class GpxWriter {
    private BufferedWriter gpxFile;

    private String path;

    private String creatorName = "Keypadmapper";

    public GpxWriter(String path) {
        this.path = path;
    }

    public GpxWriter(String path, String creatorName) {
        this.path = path;
        this.creatorName = creatorName;
    }

    /**
     * Adds a new trackpoint to the track.
     * 
     * @param lat
     *            current WGS84 latitude
     * @param lon
     *            current WGS84 longitude
     * @param time
     *            UTC time of the fix in ms since 1970-01-01
     * @throws IOException
     *             if an I/O error occurs
     */
    public void addTrackpoint(double lat, double lon, long time) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString = String.format("%tFT%tTZ", cal, cal);

        gpxFile.write("\t\t\t<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
        gpxFile.write("\t\t\t\t<time>" + timeString + "</time>\n");
        gpxFile.write("\t\t\t</trkpt>\n");
    }

    /**
     * Adds a new trackpoint to the track.
     * 
     * @param lat
     *            current WGS84 latitude
     * @param lon
     *            current WGS84 longitude
     * @param time
     *            UTC time of the fix in ms since 1970-01-01
     * @param ele
     *            current height
     * @throws IOException
     *             if an I/O error occurs
     */
    public void addTrackpoint(double lat, double lon, long time, double ele) throws IOException {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timeString = String.format("%tFT%tTZ", cal, cal);

        gpxFile.write("\t\t\t<trkpt lat=\"" + lat + "\" lon=\"" + lon + "\">\n");
        gpxFile.write("\t\t\t\t<time>" + timeString + "</time>\n");
        gpxFile.write("\t\t\t\t<ele>" + ele + "</ele>\n");
        gpxFile.write("\t\t\t</trkpt>\n");
    }

    /**
     * Closes this GPX file. The current segment and track elements will be
     * closed automatically. The file will also be closed.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void close() throws IOException {
        gpxFile.write("\t\t</trkseg>\n");
        gpxFile.write("\t</trk>\n");
        gpxFile.write("</gpx>\n");
        gpxFile.close();
    }

    public void flush() throws IOException {
        gpxFile.flush();
    }

    /**
     * Creates or opens a GPX file. There is no check whether the file exists,
     * this has to be done by the caller. If append is set to false and the file
     * exists, it will be overwritten. If append is set to true the new data
     * will be added to the file. Please note that appending is only possible
     * for files created by this class, appending to third-party files may break
     * them.
     * 
     * @param path
     *            the file to create or open
     * @param append
     *            set to true to append to existing file
     * @throws FileNotFoundException
     *             if the file cannot be opened or created
     * @throws IOException
     *             if any other I/O error occurs
     * @throws FileFormatException
     *             if the end of the GPX file is not as expected
     */
    public void openGpxWriter(boolean append) throws IOException {
        if (!append) {
            // create/overwrite file
            gpxFile =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));

            gpxFile.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            gpxFile.write("<gpx xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            gpxFile.write("\txmlns=\"http://www.topografix.com/GPX/1/0\"\n");
            gpxFile.write("\txsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\"\n");
            gpxFile.write("\tversion=\"1.0\"\n");
            gpxFile.write("\tcreator=\"" + creatorName + "\">\n");
            gpxFile.write("\n");
            gpxFile.write("\t<trk>\n");
            gpxFile.write("\t\t<trkseg>\n");

        } else {
            // append to existing (and initialised) file
            File oldGpxFile = new File(path);
            File tempGpxFile = new File(path + "~");
            BufferedReader oldGpxReader =
                    new BufferedReader(new InputStreamReader(new FileInputStream(oldGpxFile),
                            "UTF-8"));
            gpxFile =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempGpxFile),
                            "UTF-8"));
            this.path = path;

            // replace file, reopening the last track segment (remove everything
            // added by close())
            String line;
            while (true) {
                line = oldGpxReader.readLine();
                if (line == null) {
                    // found end of file without </gpx> - file is damaged,
                    // delete temporary file
                    gpxFile.close();
                    tempGpxFile.delete();
                    throw new FileFormatException();
                }
                if (line.trim().equalsIgnoreCase("</trkseg>")) {
                    String line2 = oldGpxReader.readLine();
                    if (line2.trim().equalsIgnoreCase("</trk>")) {
                        String line3 = oldGpxReader.readLine();
                        if (line3.trim().equalsIgnoreCase("</gpx>")) {
                            // replace file
                            gpxFile.flush();
                            oldGpxReader.close();
                            oldGpxFile.delete();
                            tempGpxFile.renameTo(oldGpxFile);
                            break;
                        } else {
                            gpxFile.write(line + "\n");
                            gpxFile.write(line2 + "\n");
                            gpxFile.write(line3 + "\n");
                        }
                    } else {
                        gpxFile.write(line + "\n");
                        gpxFile.write(line2 + "\n");
                    }
                } else {
                    gpxFile.write(line + "\n");
                }
            }
        }
    }

    public void reopenGpsWriter() throws IOException {
        openGpxWriter(true);
    }

    /**
     * Closes the current track and starts a new one.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void startNewTrack() throws IOException {
        gpxFile.write("\t\t</trk>\n");
        gpxFile.write("\t\t<trk>\n");
    }

    /**
     * Closes the current segment and starts a new one.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void startNewTrackSegment() throws IOException {
        gpxFile.write("\t\t</trkseg>\n");
        gpxFile.write("\t\t<trkseg>\n");
    }

    /**
     * Returns the complete path of the GPX file.
     */
    @Override
    public String toString() {
        return path;
    }
}
