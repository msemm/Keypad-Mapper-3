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
    
    /**
     * Called when address has been updated and extended fragment has to refresh 
     * data.
     */
    public void onAddressUpdated();

    void showMessage(String messageKey);
}
