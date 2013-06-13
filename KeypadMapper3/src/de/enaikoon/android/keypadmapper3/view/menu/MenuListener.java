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
        CAMERA, ADDRESS_EDITOR, GPS_INFO, SETTINGS, UNDO, SHARE, FREEZE_GPS, START_STOP_GPS, KEYPAD, AUDIO
    };

    void onMenuOptionClicked(OptionType type);

}
