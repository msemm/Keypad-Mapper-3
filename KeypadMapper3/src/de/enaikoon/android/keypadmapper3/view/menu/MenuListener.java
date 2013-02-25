/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view.menu;

/**
 * 
 */
public interface MenuListener {

    enum OptionType {
        CAMERA, EDITOR_TOGGLE, GPS_INFO, SETTINGS, UNDO, SHARE, FREEZE_GPS, KEYPAD
    };

    void onMenuOptionClicked(OptionType type);

}
