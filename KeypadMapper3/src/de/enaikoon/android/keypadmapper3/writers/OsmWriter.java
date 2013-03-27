package de.enaikoon.android.keypadmapper3.writers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Map.Entry;

public class OsmWriter {
    private BufferedWriter osmFile;

    private String path;

    private int newNodeId = -1;

    private int lineNumber = 0;

    private int undoLine = -1;

    private String generatorName = "KeypadMapper";

    public OsmWriter(String path) {
        this.path = path;
    }

    public OsmWriter(String path, String generatorName) {
        this.path = path;
        this.generatorName = generatorName;
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
        undoLine = lineNumber;
        osmFile.write("\t<node id=\"" + newNodeId-- + "\" visible=\"true\" lat=\"" + lat
                + "\" lon=\"" + lon + "\">\n");
        lineNumber++;
        for (Entry<String, String> entry : tags.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length() != 0
                    && !entry.getValue().equalsIgnoreCase("null")) {
                osmFile.write("\t\t<tag k=\"" + entry.getKey() + "\" v=\"" + entry.getValue()
                        + "\"/>\n");
                lineNumber++;
            }
        }
        osmFile.write("\t</node>\n");
        lineNumber++;
    }

    /**
     * Closes this OSM file. The file will also be closed.
     * 
     * @throws IOException
     *             if an I/O error occurs
     */
    public void close() throws IOException {
        osmFile.write("</osm>\n");
        osmFile.close();
    }

    public void deleteLastNode() throws IOException {
        this.flush();
        this.close();
        int localUndoLine = undoLine;
        undoLine = -1;
        openOsmWriter(true, localUndoLine);
    }

    public void flush() throws IOException {
        osmFile.flush();
    }

    public int getUndoLine() {
        return undoLine;
    }

    public boolean isExist() {
        File sourceFile = new File(path);
        return sourceFile.exists();
    }

    public boolean isUndoAvailable() {
        return undoLine != -1;
    }

    /**
     * Creates or opens a OSM file. There is no check whether the file exists,
     * this has to be done by the caller. If append is set to false and the file
     * exists, it will be overwritten. If append is set to true the new data
     * will be added to the file. Please note that appending is only possible
     * for files created by this class, appending to third-party files may break
     * them.
     * 
     * @param append
     *            set to true to append to existing file
     * @throws FileNotFoundException
     *             if the file cannot be opened or created
     * @throws IOException
     *             if any other I/O error occurs
     * @throws FileFormatException
     *             if the end of the OSM file is not as expected
     */
    public void openOsmWriter(boolean append) throws IOException {
        openOsmWriter(append, -1);
    }

    public void reopenOsmWriter() throws IOException {
        openOsmWriter(true);
    }

    public void setUndoLine(int undoLine) {
        this.undoLine = undoLine;
    }

    /**
     * Returns the complete path of the GPX file.
     */
    @Override
    public String toString() {
        return path;
    }

    /**
     * Creates or opens a OSM file. There is no check whether the file exists,
     * this has to be done by the caller. If append is set to false and the file
     * exists, it will be overwritten. If append is set to true the new data
     * will be added to the file. Please note that appending is only possible
     * for files created by this class, appending to third-party files may break
     * them.
     * 
     * @param append
     *            set to true to append to existing file
     * @throws FileNotFoundException
     *             if the file cannot be opened or created
     * @throws IOException
     *             if any other I/O error occurs
     * @throws FileFormatException
     *             if the end of the OSM file is not as expected
     */
    private void openOsmWriter(boolean append, int lineLimit) throws IOException {
        if (!append) {
            // create/overwrite file
            osmFile =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));

            osmFile.write("<?xml version='1.0' encoding='UTF-8'?>\n");
            osmFile.write("<osm version='0.6' generator='" + generatorName + "'>\n");
            lineNumber = 2;
        } else {
            // append to existing (and initialised) file
            File oldOsmFile = new File(path);
            File tempOsmFile = new File(path + "~");
            BufferedReader oldOsmReader =
                    new BufferedReader(new InputStreamReader(new FileInputStream(oldOsmFile),
                            "UTF-8"));
            osmFile =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempOsmFile),
                            "UTF-8"));

            this.path = path;

            // replace file, reopening the last track segment (remove everything
            // added by close())
            String line;
            lineNumber = 0;
            while (true) {
                line = oldOsmReader.readLine();
                lineNumber++;
                if (line == null) {
                    // found end of file without </osm> - file is damaged,
                    // delete temporary file
                    osmFile.close();
                    tempOsmFile.delete();
                    throw new FileFormatException();
                }
                if (line.trim().equalsIgnoreCase("</osm>") || lineNumber > lineLimit
                        && lineLimit > 0) {
                    // replace file
                    osmFile.flush();
                    oldOsmReader.close();
                    oldOsmFile.delete();
                    tempOsmFile.renameTo(oldOsmFile);
                    lineNumber--;
                    break;
                } else {
                    osmFile.write(line + "\n");
                }
            }
        }
    }
}
