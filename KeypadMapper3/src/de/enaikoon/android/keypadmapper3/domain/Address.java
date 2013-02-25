/**************************************************************************
 * TODO Copyright
 *
 * $Id: Address.java 93 2013-01-11 14:20:54Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/domain/Address.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.domain;

/**
 * 
 */
public class Address {

    private String number = "";

    private String notes = "";

    private String housename = "";

    private String street = "";

    private String city = "";

    private String countryCode = "";

    private double latitude;

    private double longitude;

    private String postcode = "";

    public Address() {

    }

    public Address(Address address) {
        this.city = address.city;
        this.countryCode = address.countryCode;
        this.housename = address.housename;
        this.latitude = address.latitude;
        this.longitude = address.longitude;
        this.notes = address.notes;
        this.number = address.number;
        this.street = address.street;
        this.postcode = address.postcode;
    }

    public String getCity() {
        return city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getHousename() {
        return housename;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getNotes() {
        return notes;
    }

    public String getNumber() {
        return number;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getStreet() {
        return street;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void setHousename(String housename) {
        this.housename = housename;
    }

    public void setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public void setStreet(String street) {
        this.street = street;
    }

}
