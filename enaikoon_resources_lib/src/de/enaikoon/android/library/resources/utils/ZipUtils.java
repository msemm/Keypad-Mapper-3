/**************************************************************************
 * Copyright
 *
 * $Id: ZipUtils.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/utils/ZipUtils.java $
 **************************************************************************/

package de.enaikoon.android.library.resources.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 */
public class ZipUtils {

    public static boolean unzipArchive(File archive, File outputDir) {
        boolean result = false;
        ZipFile zipfile;
        try {
            zipfile = new ZipFile(archive);
            for (Enumeration e = zipfile.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                unzipEntry(zipfile, entry, outputDir);
            }
            result = true;
        } catch (IOException e1) {
            // ignore
        }
        return result;
    }

    private static void createDir(File dir) throws IOException {
        if (!dir.mkdirs())
            throw new IOException("Can not create dir " + dir);
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir)
            throws IOException {

        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }

        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }

        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream =
                new BufferedOutputStream(new FileOutputStream(outputFile));

        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }
}
