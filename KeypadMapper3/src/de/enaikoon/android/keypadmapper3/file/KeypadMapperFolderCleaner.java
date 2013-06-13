/**************************************************************************
 * Copyright;
 *
 * $Id: KeypadMapperFolderCleaner.java 117 2013-01-20 14:07:53Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/file/KeypadMapperFolderCleaner.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.file;

import java.io.File;

import android.util.Log;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;

/**
 * The main purpose of this class is removing empty files in keypadmapper
 * directory
 */
public class KeypadMapperFolderCleaner {
    private static final String TAG = "KeypadMapper filecleaner";

    /**
     * Removes all files from KeypadMapper folder
     * 
     * @param kpmFolder
     *            KeypadMapper folder on sdcard
     */
    public static void cleanFolder(File kpmFolder) {
        KeypadMapperApplication.getInstance().getMapper().clearAllCurrentData();
        if (kpmFolder.exists()) {
            // making cleaning
            File[] osmFiles = kpmFolder.listFiles();
            for (File file : osmFiles) {
                if (file.isFile()) {
                    Log.i(TAG, "Deleting file: " + file.getName());
                    boolean result = file.delete();
                    Log.i(TAG, "Deleting result: " + result);
                } else if (file.isDirectory()) {
                    cleanFolder(file);
                    Log.i(TAG, "Deleting folder: " + file.getName());
                    file.delete();
                }
            }
        } else {
            Log.i(TAG, kpmFolder.getAbsolutePath() + " folder is not exist");
        }
        KeypadMapperApplication.getInstance().getMapper().clearAllCurrentData();
    }
}
