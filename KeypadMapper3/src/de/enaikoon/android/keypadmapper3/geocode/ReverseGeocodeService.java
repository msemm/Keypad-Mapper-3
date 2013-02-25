/**************************************************************************
 * Copyright
 *
 * $Id: ReverseGeocodeService.java 130 2013-01-23 17:04:57Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/geocode/ReverseGeocodeService.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.geocode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 */
public class ReverseGeocodeService {

    private static final String URL = "http://nominatim.enaikoon.de/reverse?format=json";

    // private final Logger log = Logger.getLogger(ReverseGeocodeService.class);

    private static final String TAG = "ReverseGeocodeService";

    private static final String[] STREET_NAME_TYPES = { "road", "pedestrian", "bridleway",
            "bus_guideway", "byway", "construction", "cycleway", "footway", "living_street",
            "motorway", "motorway_link", "path", "primary", "primary_link", "proposed", "raceway",
            "residential", "secondary", "secondary_link", "service", "steps", "tertiary",
            "tertiary_link", "track", "trunk", "trunk_link", "unclassified", "mini_roundabout",
            "motorway_junction", "roundabout", "stop", "ford", "public_transport platform",
            "rest_area", "speed_camera", "services", "turning_circle", "User Defined" };

    synchronized public ReverseGeocodeResult reverseGeocode(double latitude, double longitude,
            String lang) throws GeoReversingError {
        String url = generateUrl(latitude, longitude, lang);
        try {
            // if (BuildConfig.DEBUG) {
            // log.info("Requested url: " + url);
            // }
            String data = loadData(url);
            // if (BuildConfig.DEBUG) {
            // log.info("data: " + data);
            // }
            ReverseGeocodeResult result = parseResult(data);
            return result;
        } catch (IOException exception) {
            // log.error(exception.getMessage());
            throw new GeoReversingError(exception);
        } catch (URISyntaxException exception) {
            // log.error(exception.getMessage());
            throw new GeoReversingError(exception);
        } catch (JSONException exception) {
            // log.error(exception.getMessage());
            throw new GeoReversingError(exception);
        }
    }

    protected String generateUrl(double latitude, double longitude, String lang) {
        return URL + "&addressdetails=1&lat=" + latitude + "&lon=" + longitude
                + "&accept-language=" + lang;
    }

    protected String loadData(String requestedUrl) throws IOException, URISyntaxException {

        URL url;
        HttpURLConnection httpUrlConnection = null;
        InputStream is = null;
        try {
            url = new URL(requestedUrl);

            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setDoInput(true);
            httpUrlConnection.setUseCaches(false);

            int responceCode = httpUrlConnection.getResponseCode();
            if (responceCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("Wrong response code: " + responceCode);
            }

            is = httpUrlConnection.getInputStream();

            byte[] buffer = new byte[32 * 1024];
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            int n = 0;

            while ((n = is.read(buffer)) > 0) {
                result.write(buffer, 0, n);
            }
            String content = new String(result.toByteArray());
            return content;
        } catch (IOException e) {
            throw e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // skip it
                }
            }
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
    }

    private ReverseGeocodeResult parseResult(String jsonText) throws JSONException {
        JSONObject jsonResponse = new JSONObject(jsonText);
        ReverseGeocodeResult result = new ReverseGeocodeResult();
        if (jsonResponse.has("address")) {
            JSONObject jsonAddress = jsonResponse.getJSONObject("address");
            if (jsonAddress.has("postcode")) {
                String postcode = jsonAddress.getString("postcode");
                result.setPostcode(postcode);
            }
            String addressName = null;

            for (int i = 0; i < STREET_NAME_TYPES.length; i++) {
                if (!jsonAddress.optString(STREET_NAME_TYPES[i]).equals("")) {
                    addressName = jsonAddress.optString(STREET_NAME_TYPES[i]);
                    break;
                }
            }
            result.setStreetName(addressName);
        }
        return result;
    }

}
