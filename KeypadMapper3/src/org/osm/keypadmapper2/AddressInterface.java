package org.osm.keypadmapper2;


public interface AddressInterface {

    public void extendedAddressActive();

    public void extendedAddressInactive();

    /**
     * Called when the user changes the housenumber.
     * 
     * @param newHousenumber
     *            Currently entered housenumber.
     */
    public void onHousenumberChanged(String newHousenumber);

    void showMessage(String messageKey);
}
