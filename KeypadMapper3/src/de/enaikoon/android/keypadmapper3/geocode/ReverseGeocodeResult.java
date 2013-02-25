/**************************************************************************
 * Copyright
 *
 * $Id: ReverseGeocodeResult.java 10 2012-12-09 18:00:36Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/geocode/ReverseGeocodeResult.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.geocode;


/**
 * 
 */
public class ReverseGeocodeResult {

    private String streetName;

    private String postcode;

    public String getPostcode() {
        return postcode;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

}
