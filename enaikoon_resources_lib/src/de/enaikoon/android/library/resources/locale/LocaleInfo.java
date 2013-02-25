/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.library.resources.locale;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 */
public class LocaleInfo implements Parcelable {

    public static final String DEFAULT_LOCALE_NAME = "default";

    private String localeName;

    private String localeUrl;

    public static final Parcelable.Creator<LocaleInfo> CREATOR =
            new Parcelable.Creator<LocaleInfo>() {
                @Override
                public LocaleInfo createFromParcel(Parcel in) {
                    String localeName = in.readString();
                    String localeUrl = in.readString();
                    return new LocaleInfo(localeName, localeUrl);
                }

                @Override
                public LocaleInfo[] newArray(int size) {
                    return new LocaleInfo[size];
                }
            };

    public LocaleInfo(String localeName, String localeUrl) {
        super();
        this.localeName = localeName;
        this.localeUrl = localeUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.Parcelable#describeContents()
     */
    @Override
    public int describeContents() {
        return 0;
    }

    public String getLocaleName() {
        return localeName;
    }

    public String getLocaleUrl() {
        return localeUrl;
    }

    public void setLocaleName(String localeName) {
        this.localeName = localeName;
    }

    public void setLocaleUrl(String localeUrl) {
        this.localeUrl = localeUrl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.Parcelable#writeToParcel(android.os.Parcel, int)
     */
    @Override
    public void writeToParcel(Parcel outParcel, int flags) {
        outParcel.writeString(localeName);
        outParcel.writeString(localeUrl);
    }
}
