/**************************************************************************
 * Copyright
 *
 * $Id: LocationNotAvailableException.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/LocationNotAvailableException.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3;

/**
 * For handling case when LocationManager doesn't provide Location
 */
public class LocationNotAvailableException extends RuntimeException {

    public LocationNotAvailableException() {
        super();
    }

    public LocationNotAvailableException(String detailMessage) {
        super(detailMessage);
    }

    public LocationNotAvailableException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public LocationNotAvailableException(Throwable throwable) {
        super(throwable);
    }

}
