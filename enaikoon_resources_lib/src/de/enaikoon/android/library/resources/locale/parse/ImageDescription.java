/**************************************************************************
 * Copyright
 *
 * $Id: ImageDescription.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/locale/ImageDescription.java $
 **************************************************************************/

package de.enaikoon.android.library.resources.locale.parse;


/**
 * 
 */
public class ImageDescription {

    private String originalFileName;

    private String key;

    private String zipFileName;

    public String getKey() {
        return key;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getZipFileName() {
        return zipFileName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setZipFileName(String zipFileName) {
        this.zipFileName = zipFileName;
    }

}
