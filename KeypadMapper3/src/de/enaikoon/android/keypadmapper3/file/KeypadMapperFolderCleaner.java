/**************************************************************************
 * Copyright;
 *
 * $Id: KeypadMapperFolderCleaner.java 117 2013-01-20 14:07:53Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/file/KeypadMapperFolderCleaner.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.file;

import java.io.File;
import java.io.FileFilter;

import android.util.Log;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;

/**
 * The main purpose of this class is removing empty files in keypadmapper
 * directory
 */
public class KeypadMapperFolderCleaner {

    private static class ImageFileFilter implements FileFilter {

        private String prefix;

        private String postfix;

        public ImageFileFilter(String prefix, String postfix) {
            super();
            this.prefix = prefix;
            this.postfix = postfix;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.io.FileFilter#accept(java.io.File)
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().endsWith(postfix) && pathname.getName().startsWith(prefix)) {
                return true;
            }
            return false;
        }
    }

    private static class OsmFileFilter implements FileFilter {

        /*
         * (non-Javadoc)
         * 
         * @see java.io.FileFilter#accept(java.io.File)
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname.getName().endsWith(".osm")) {
                return true;
            }
            return false;
        }
    }

    public static final int EMPTY_OSM_FILE_SIZE = 79;

    private static final String TAG = "FileCleaner";

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

    /**
     * Remove *.osm and *.gpx files that do not contain any user generated data
     * 
     * @param kpmFolder
     *            KeypadMapper folder on sdcard
     */
    public static void cleanFolderFromEmptyFiles(File kpmFolder) {
        if (kpmFolder.exists()) {
            // making cleaning
            File[] osmFiles = kpmFolder.listFiles(new OsmFileFilter());
            for (File file : osmFiles) {
                Log.i(TAG, "" + file.getName());
                if (userDataNotExists(file, kpmFolder)) {
                    Log.i(TAG, "Osm file to delete " + file.getName());
                    file.delete();
                    String osmPath = file.getAbsolutePath();
                    File gpxFile = new File(osmPath.substring(0, osmPath.length() - 4) + ".gpx");
                    gpxFile.delete();
                } else {
                    Log.i(TAG, "failed to delete" + file.getName());
                }
            }
        } else {
            Log.i(TAG, "KeypadMapper folder is not exist");
        }
    }

    private static boolean userDataNotExists(File osmFile, File kpmFolder) {
        boolean osmFileEmpty =
                osmFile.length() <= (EMPTY_OSM_FILE_SIZE + kpmFolder.getName().length());
        String fileName = osmFile.getName();
        String prefix = fileName.substring(0, fileName.indexOf('.'));
        File[] imageFiles = kpmFolder.listFiles(new ImageFileFilter(prefix, ".jpg"));
        boolean noImages = imageFiles.length == 0;
        return osmFileEmpty && noImages;

    }
}
