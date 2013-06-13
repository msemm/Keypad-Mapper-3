package de.enaikoon.android.keypadmapper3.writers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.ByteSearch;

public class OsmWriter {
    private int newNodeId = -1;
    
    private static final String OSM_HEADER = "<?xml version='1.0' encoding='UTF-8'?>\n" + 
                                             "<osm version='0.6' generator='" + 
                                             KeypadMapperApplication.getInstance().getLocalizer().getString("app_name") +
                                             "'>\n";
    private static final String OSM_FOOTER = "</osm>\n";
    private KeypadMapperSettings settings;
    
    private RandomAccessFile osmFile;

    public OsmWriter() {
        settings = KeypadMapperApplication.getInstance().getSettings();
    }
    
    private String getNewFilename() {
        String path = KeypadMapperApplication.getInstance().getKeypadMapperDirectory().getAbsolutePath();

        Calendar cal = Calendar.getInstance();
        return path + "/" + String.format("%tF_%tH-%tM-%tS", cal, cal, cal, cal) + ".osm";
    }
    
    /**
     * Adds a new node to the OSM file.
     * 
     * @param lat
     *            WGS84 latitude of the new node
     * @param lon
     *            WGS84 longitude of the new node
     * @param tags
     *            Map containing the tags for the new node.
     * @throws IOException
     *             if an I/O error occurs
     */
    public void addNode(double lat, double lon, Map<String, String> tags) throws IOException {
        int nodeId = -KeypadMapperApplication.getInstance().getMapper().getHouseNumberCount();
        
        osmFile.writeBytes("\t<node id=\"" + nodeId + "\" visible=\"true\" lat=\"" + lat
                + "\" lon=\"" + lon + "\">\n");
        for (Entry<String, String> entry : tags.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() != 0
                    && !entry.getValue().equalsIgnoreCase("null")) {
                osmFile.writeBytes("\t\t<tag k=\"" + entry.getKey() + "\" v=\"" + entry.getValue()
                        + "\"/>\n");
            }
        }
        osmFile.writeBytes("\t</node>\n");

    }

    public void close() throws IOException {
        osmFile.writeBytes(OSM_FOOTER);
        osmFile.close();
    }

    /**
     * Removes last node.
     * File will be open and closed by this function.
     */
    public void deleteLastNode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            final int bufferSize = 128;
            byte [] buffer = new byte[ bufferSize ];
            
            osmFile = new RandomAccessFile(settings.getLastOsmFile(), "rw");
            if (osmFile.length() < (OSM_HEADER.getBytes().length + OSM_FOOTER.getBytes().length)) {
                Log.e("KeypadMapper", "Cannot undo - no nodes");
                return;
            }
            
            osmFile.seek(osmFile.length());
            
            long seekTo = osmFile.length();
            while (true) {
                seekTo = seekTo - bufferSize;
                if (seekTo >= 0) {
                    osmFile.seek(seekTo);
                } else {
                    osmFile.seek(osmFile.length());
                    osmFile.close();
                    return;
                }
                
                int read = osmFile.read(buffer);
                // read line by line
                if (read == -1) {
                    osmFile.close();
                    baos.close();
                    return;
                }
                
                if (baos.size() == 0) {
                    baos.write(buffer);
                } else {
                    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                    baos2.write(buffer);
                    baos2.write(baos.toByteArray());
                    baos = baos2;
                    baos2 = null;
                }
                
                int idx = ByteSearch.indexOf(baos.toByteArray(), "<node id".getBytes("UTF-8"));
                if (idx >= 0) {
                    
                    osmFile.seek(seekTo + idx);
                    osmFile.write(OSM_FOOTER.getBytes("UTF-8"));
                    osmFile.setLength(osmFile.getFilePointer());
                    osmFile.close();
                    
                    //Log.d("KeypadMapper", "Last node removed!");
                    baos.close();
                    return;
                }
            }
        } catch (Exception e) {
            Log.e("KeypadMapper", "failed to delete last node", e);
            if (osmFile != null) {
                try {
                    osmFile.close();
                    baos.close();
                } catch (Exception ignored) {}
            }
        } 
    }
    
    public void openOsmWriter(boolean append) throws IOException {
        if (!append || settings.getLastOsmFile() == null) {
            // create/overwrite file
            String newFile = getNewFilename();
            settings.setLastOsmFile(newFile);
            osmFile = new RandomAccessFile(newFile, "rw");
            
            osmFile.writeBytes(OSM_HEADER);
        } else {
            osmFile = new RandomAccessFile(settings.getLastOsmFile(), "rw");
            Log.d("KeypadMapper", "header + footer len: " + (OSM_HEADER.getBytes().length + OSM_FOOTER.getBytes().length) + " osm len:" + osmFile.length());
            if (osmFile.length() > 0) {
                if (osmFile.length() >= (OSM_HEADER.getBytes().length + OSM_FOOTER.getBytes().length)) {
                    osmFile.seek(osmFile.length() - OSM_FOOTER.getBytes().length);
                } else {
                    osmFile.seek(osmFile.length());
                }
            }
            
        }
    }
    
    public static int getEmptyFileSize() {
        return OSM_HEADER.getBytes().length + OSM_FOOTER.getBytes().length;
    }
}
