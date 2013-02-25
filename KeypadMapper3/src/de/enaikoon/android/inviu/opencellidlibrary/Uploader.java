/**
 * 
 */
package de.enaikoon.android.inviu.opencellidlibrary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import de.enaikoon.android.keypadmapper3.utils.ConnectivityUtils;

/**
 * @author Marcus Wolschon
 * 
 */
public class Uploader implements Runnable {

    private Database myDatabase;

    private Context myContext;

    private boolean silent = false;

    public Uploader(final Database db, final Context aContext, boolean silent)
            throws MalformedURLException {
        this.myDatabase = db;
        this.myContext = aContext;
        this.silent = silent;
    }

    public void onStatus(final int count, final int max) {
        // do nothing
    }

    @Override
    public void run() {
        if (!ConnectivityUtils.isDownloadAllowed(myContext)) {
            FileLog.writeToLog("Do not upload due to settings");
            return;
        }
        FileLog.writeToLog("Upload STARTED");

        String exsistingFileName = "upload.csv";

        String logTag = getClass().getName() + "run()";
        boolean success = false;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
                    HttpVersion.HTTP_1_0);

            String IP = "";
            if (Configurator.isPRODUCTION_VERSION()) {
                IP = "www.enaikoon.de";
            } else {
                IP = "217.70.136.102";
            }

            HttpPost httppost = new HttpPost("http://" + IP + "/gpsSuiteCellId/measure/uploadCsv");

            FileLog.writeToLog("Upload request URL: " + "http://" + IP
                    + "/gpsSuiteCellId/measure/uploadCsv");

            StringBuilder sb = null;
            NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
            format.setMaximumFractionDigits(10);
            Database.DBIterator<Meassurement> next = myDatabase.getNonUploadedMeassurements();
            HttpResponse response = null;
            int max = -1;
            try {
                int count = 0;
                max = next.getCount();

                FileLog.writeToLog(logTag + ": max=" + max);

                if (max > 0) {
                    while (next.hasNext()) {
                        // FileLog.writeToLog.d(logTag, "next");
                        int t = 0;
                        final int maxt = 128;

                        sb =
                                new StringBuilder(
                                        "lat,lon,mcc,mnc,lac,cellid,timestamp,signal level,userid,extrainfo,create_at,updated_at,speed,direction\n");
                        while (t < maxt && next.hasNext()) {
                            t++;
                            Meassurement meassurement = next.next();
                            onStatus(count, max);
                            count++;
                            sb.append(format.format(meassurement.getLat())
                                    + ","
                                    + format.format(meassurement.getLon())
                                    + ","
                                    + meassurement.getMcc()
                                    + ","
                                    + meassurement.getMnc()
                                    + ","
                                    + meassurement.getLac()
                                    + ","
                                    + meassurement.getCellid()
                                    + ","
                                    + meassurement.getTimestamp()
                                    + ","
                                    + meassurement.getGsmSignalStrength()
                                    + ","
                                    + "TODO: UserID "
                                    + "," // TODO: UserID
                                    + "Origin: ENAiKOON software"
                                    + "," // extraInfo
                                    + myDatabase.getFirstMeassurement(meassurement.getCellid(),
                                            meassurement.getLac(), meassurement.getMcc(),
                                            meassurement.getMnc(), meassurement.getTimestamp())
                                    + "," + meassurement.getTimestamp() + ","
                                    + (int) meassurement.getSpeed() + ","
                                    + (int) meassurement.getBearing() + "\n");
                        }

                        FileLog.writeToLog(logTag + ": " + sb.toString());

                        MultipartEntity mpEntity = new MultipartEntity();
                        mpEntity.addPart("key", new StringBody("NoUserID"));
                        mpEntity.addPart("datafile", new InputStreamBody(new ByteArrayInputStream(
                                sb.toString().getBytes()), "text/csv", exsistingFileName));

                        ByteArrayOutputStream bArrOS = new ByteArrayOutputStream();
                        // reqEntity is the MultipartEntity instance
                        mpEntity.writeTo(bArrOS);
                        bArrOS.flush();
                        ByteArrayEntity bArrEntity = new ByteArrayEntity(bArrOS.toByteArray());
                        bArrOS.close();

                        bArrEntity.setChunked(false);
                        bArrEntity.setContentEncoding(mpEntity.getContentEncoding());
                        bArrEntity.setContentType(mpEntity.getContentType());

                        httppost.setEntity(bArrEntity);

                        response = httpclient.execute(httppost);
                        if (response == null) {
                            FileLog.writeToLog(logTag + ": null HTTP-response");
                            throw new IllegalStateException("no HTTP-response from server");
                        }

                        HttpEntity resEntity = response.getEntity();

                        FileLog.writeToLog(logTag + ": " + response.getStatusLine().getStatusCode()
                                + " - " + response.getStatusLine());

                        if (resEntity != null) {
                            FileLog.writeToLog(logTag + ": " + EntityUtils.toString(resEntity));
                            resEntity.consumeContent();
                        }
                        if (response.getStatusLine().getStatusCode() != 200) {
                            break;
                        }
                    }
                }
            } finally {
                next.close();
            }

            httpclient.getConnectionManager().shutdown();

            if (max > 0) {
                if (response == null) {
                    FileLog.writeToLog(logTag + ": " + "null response");

                    throw new IllegalStateException("no response");
                }
                if (response.getStatusLine() == null) {
                    FileLog.writeToLog(logTag + ": " + "null HTTP-status-line");

                    throw new IllegalStateException("no HTTP-status returned");
                }
                if (response.getStatusLine().getStatusCode() == 200) {
                    myDatabase.setAllUploaded();
                }
            }

            onStatus(max, max);
            success = true;

        } catch (Exception ioe) {
            FileLog.writeExceptionToLog(ioe);
            if (!silent) {
                throw new IllegalStateException("IO-Error: " + ioe.getMessage());
            }
        }
        if (!success && !silent) {
            throw new IllegalStateException("unknown error");
        }
    }

}
