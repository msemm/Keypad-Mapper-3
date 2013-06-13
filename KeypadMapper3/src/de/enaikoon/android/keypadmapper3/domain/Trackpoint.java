/**************************************************************************
 * TODO Copyright
 *
 * $Id: Trackpoint.java 93 2013-01-11 14:20:54Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/domain/Trackpoint.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.domain;

/**
 * 
 */
public class Trackpoint {

    private double latitude;

    private double longitude;

    private long time;

    private double altitude = Double.MIN_VALUE;
    
    /** WAV filename */
    private String filename;

    private double speed;
    /**
     * 
     * @param latitude
     * @param longitude
     * @param time
     * @param filename set this to null for trackpoints which don't have a WAV file name
     */
    public Trackpoint(double latitude, double longitude, long time, String filename, double speed) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.filename = filename;
        this.setSpeed(speed);
    }

    public double getAltitude() {
        return altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public long getTime() {
        return time;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

}
