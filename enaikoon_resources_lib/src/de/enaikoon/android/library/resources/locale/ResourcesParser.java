/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.library.resources.locale;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;
import de.enaikoon.android.library.resources.locale.parse.ImageDescription;
import de.enaikoon.android.library.resources.locale.parse.ImageResourceDescriptions;
import de.enaikoon.android.library.resources.locale.parse.ImageResourcesToDelete;
import de.enaikoon.android.library.resources.locale.parse.KeyToDelete;
import de.enaikoon.android.library.resources.locale.parse.Resources;
import de.enaikoon.android.library.resources.locale.parse.StringResource;
import de.enaikoon.android.library.resources.locale.parse.TextResourcesToDelete;

/**
 * 
 */
final public class ResourcesParser {

    private static final String TAG = "ResourcesParser";

    public static ImageResourcesToDelete parseDeletedImageResources(File resourcesFile) {
        ImageResourcesToDelete resources = new ImageResourcesToDelete();
        List<KeyToDelete> keyList = new ArrayList<KeyToDelete>();
        resources.setKeys(keyList);

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(resourcesFile);
            doc.getDocumentElement().normalize();

            NodeList textsNode = doc.getElementsByTagName("string");
            for (int temp = 0; temp < textsNode.getLength(); temp++) {

                Node nNode = textsNode.item(temp);

                String value = nNode.getChildNodes().item(0).getNodeValue();

                KeyToDelete key = new KeyToDelete();
                key.setKey(value);
                keyList.add(key);
            }
        } catch (Exception parsingError) {
            Log.e(TAG, "Error during parsing " + parsingError.getMessage());
        }

        return resources;
    }

    public static TextResourcesToDelete parseDeletedTextResources(File resourcesFile) {
        TextResourcesToDelete resources = new TextResourcesToDelete();
        List<KeyToDelete> keyList = new ArrayList<KeyToDelete>();
        resources.setKeys(keyList);

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(resourcesFile);
            doc.getDocumentElement().normalize();

            NodeList textsNode = doc.getElementsByTagName("string");
            for (int temp = 0; temp < textsNode.getLength(); temp++) {

                Node nNode = textsNode.item(temp);

                String value = nNode.getChildNodes().item(0).getNodeValue();

                KeyToDelete key = new KeyToDelete();
                key.setKey(value);
                keyList.add(key);
            }
        } catch (Exception parsingError) {
            Log.e(TAG, "Error during parsing " + parsingError.getMessage());
        }

        return resources;
    }

    public static ImageResourceDescriptions parseImageResources(File resourcesFile) {
        ImageResourceDescriptions descriptions = new ImageResourceDescriptions();
        List<ImageDescription> images = new ArrayList<ImageDescription>();
        descriptions.setImageResources(images);

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(resourcesFile);
            doc.getDocumentElement().normalize();

            NodeList descriptionsNode = doc.getElementsByTagName("description");
            for (int i = 0; i < descriptionsNode.getLength(); i++) {

                Node nNode = descriptionsNode.item(i);
                Element eElement = (Element) nNode;
                String key = getTagValue("resource-key", eElement);
                String originalName = getTagValue("original-filename", eElement);
                String zipFileName = getTagValue("zip-filename", eElement);

                ImageDescription image = new ImageDescription();
                image.setKey(key);
                image.setOriginalFileName(originalName);
                image.setZipFileName(zipFileName);
                images.add(image);
            }
        } catch (Exception parsingError) {
            Log.e(TAG, "Error during parsing " + parsingError.getMessage());
        }

        return descriptions;
    }

    public static String parseServerTime(File serverTimeFile) {
        if (!serverTimeFile.exists()) {
            return null;
        }
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(serverTimeFile);
            doc.getDocumentElement().normalize();

            NodeList textsNode = doc.getElementsByTagName("time");
            if (textsNode.getLength() > 0) {

                Node nNode = textsNode.item(0);

                String value = null;
                if (nNode.getChildNodes().getLength() == 1) {
                    value = nNode.getChildNodes().item(0).getNodeValue();
                }

                return value;
            }
        } catch (Exception parsingError) {
            Log.e(TAG, "Error during parsing " + parsingError.getMessage());
        }
        return null;
    }

    public static Resources parseTextResources(File resourcesFile) {
        Resources resources = new Resources();
        List<StringResource> texts = new ArrayList<StringResource>();
        resources.setStringResources(texts);

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(resourcesFile);
            doc.getDocumentElement().normalize();

            NodeList textsNode = doc.getElementsByTagName("string");
            for (int temp = 0; temp < textsNode.getLength(); temp++) {

                Node nNode = textsNode.item(temp);
                NamedNodeMap attributes = nNode.getAttributes();
                String name = attributes.getNamedItem("name").getNodeValue();

                String value = "";
                if (nNode.getChildNodes().getLength() == 1) {
                    value = nNode.getChildNodes().item(0).getNodeValue();
                }

                StringResource text = new StringResource();
                text.setName(name);
                text.setContent(value);
                texts.add(text);
            }
        } catch (Exception parsingError) {
            Log.e(TAG, "Error during parsing " + parsingError.getMessage());
        }

        return resources;
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = nlList.item(0);

        return nValue.getNodeValue();
    }

    private ResourcesParser() {

    }
}
