/**************************************************************************
 * Copyright
 *
 * $Id: GeoReversingError.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/geocode/GeoReversingError.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.geocode;

/**
 * 
 */
public class GeoReversingError extends Exception {

    public GeoReversingError() {
        super();
    }

    public GeoReversingError(String detailMessage) {
        super(detailMessage);
    }

    public GeoReversingError(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public GeoReversingError(Throwable throwable) {
        super(throwable);
    }

}
