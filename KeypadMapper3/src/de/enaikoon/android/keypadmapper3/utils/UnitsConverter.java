/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.utils;

/**
 * 
 */
public final class UnitsConverter {
    public static double convertFeetsToMeters(double feets) {
        return feets * 0.3048006d;
    }

    public static double convertMetersToFeets(double meters) {
        return meters * 3.2808334366796d;
    }

    private UnitsConverter() {

    }
}
