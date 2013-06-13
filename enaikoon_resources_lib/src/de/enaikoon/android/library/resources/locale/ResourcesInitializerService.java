/**************************************************************************
 * Copyright
 *
 * $Id: RemoteLocaleInitializerService.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/locale/RemoteLocaleInitializerService.java $
 **************************************************************************/

package de.enaikoon.android.library.resources.locale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import de.enaikoon.android.library.resources.locale.parse.ImageDescription;
import de.enaikoon.android.library.resources.locale.parse.ImageResourceDescriptions;
import de.enaikoon.android.library.resources.locale.parse.ImageResourcesToDelete;
import de.enaikoon.android.library.resources.locale.parse.KeyToDelete;
import de.enaikoon.android.library.resources.locale.parse.Resources;
import de.enaikoon.android.library.resources.locale.parse.StringResource;
import de.enaikoon.android.library.resources.locale.parse.TextResourcesToDelete;
import de.enaikoon.android.library.resources.utils.ZipUtils;

/**
 * Service for downloading and saving resources from the Enaikoon resource
 * editor.<br>
 * Example of usage:<br>
 * <code>
 * ArrayList<LocaleInfo> localeInfos = new ArrayList<LocaleInfo>();
        localeInfos
                .add(new LocaleInfo(
                        "en",
                        "http://liferay.enaikoon.de:8080/10-resourceeditor-services/zip/resources?applicationCode=keypadmapper&domainName=general&languageCode=en_GB"));
        localeInfos
                .add(new LocaleInfo(
                        "de",
                        "http://liferay.enaikoon.de:8080/10-resourceeditor-services/zip/resources?applicationCode=keypadmapper&domainName=general&languageCode=de_DE"));
        localeInfos
                .add(new LocaleInfo(
                        "es",
                        "http://liferay.enaikoon.de:8080/10-resourceeditor-services/zip/resources?applicationCode=keypadmapper&domainName=general&languageCode=es_ES"));
        localeInfos
                .add(new LocaleInfo(
                        "fr",
                        "http://liferay.enaikoon.de:8080/10-resourceeditor-services/zip/resources?applicationCode=keypadmapper&domainName=general&languageCode=fr_FR"));
        ResourcesInitializerService.startResourceLoading(getApplicationContext(), localeInfos);
 * </code>
 */
public class ResourcesInitializerService extends IntentService {

    private static SimpleDateFormat serviceDateFormat;

    private static final String TEXT_RESOURCE_FILE_NAME = "text-resources.xml";

    private static final String DELETED_TEXT_RESOURCE_FILE_NAME = "deleted-text-keys.xml";

    private static final String DELETED_IMAGE_RESOURCE_FILE_NAME = "deleted-image-keys.xml";

    private static final String IMAGE_RESOURCE_FILE_NAME = "image-resource-descriptions.xml";

    private static final String TIMESTAMP_FILE_NAME = "current-time.xml";

    private static final String TAG = "RemoteLocaleInitializerService";
    
    /**
     * Initiate loading for resources in localeInfos
     * 
     * @param context
     * @param localeInfos
     */
    public static void startResourceLoading(Context context, ArrayList<LocaleInfo> localeInfos) {
        Intent localeInitializerIntent = new Intent(context, ResourcesInitializerService.class);
        localeInitializerIntent.putParcelableArrayListExtra("locale_infos", localeInfos);
        context.startService(localeInitializerIntent);
    }

    public static void startResourceLoading(Context context, String languagesCodeResourceName,
            String languagesNameResourceName, String languagesUrlResourceName) {
        if (languagesCodeResourceName == null || languagesNameResourceName == null
                || languagesUrlResourceName == null) {
            throw new IllegalArgumentException("Input arguments could not be null");
        }
        Intent localeInitializerIntent = new Intent(context, ResourcesInitializerService.class);
        localeInitializerIntent.putExtra("lang_codes", languagesCodeResourceName);
        localeInitializerIntent.putExtra("lang_names", languagesNameResourceName);
        localeInitializerIntent.putExtra("lang_urls", languagesUrlResourceName);
        context.startService(localeInitializerIntent);
    }

    private static HttpClient client;

    private static Localizer localizer;

    /**
     * @param name
     */
    public ResourcesInitializerService() {
        super("RemoteLocaleInitializerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init(this);
    }

    protected static boolean isDownloadAllowed() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
        	new Thread(new Runnable()
			{
				
				@Override
				public void run()
				{
		            ArrayList<LocaleInfo> infos = intent.getParcelableArrayListExtra("locale_infos");
		            String languagesCodeResourceName = intent.getStringExtra("lang_codes");
		            String languagesNameResourceName = intent.getStringExtra("lang_names");
		            String languagesUrlResourceName = intent.getStringExtra("lang_urls");
		            if (infos != null) {
		                for (LocaleInfo info : infos) {
		                    downloadResources(info.getLocaleName(), info.getLocaleUrl(), ResourcesInitializerService.this);
		                }
		            } else if (languagesCodeResourceName != null && languagesNameResourceName != null
		                    && languagesUrlResourceName != null) {
		                downloadResourcesWithLangDetection(languagesCodeResourceName,
		                        languagesNameResourceName, languagesUrlResourceName);
		            } else {
		                Log.e(TAG, "No LocaleInfo received");
		            }
				}
			}).start();
        }
    }

    
    public static void forceDownload(LocaleInfo linfo, Context context) {
        if (linfo != null) {
            Log.i(TAG, "FORCE Downloading: " + linfo.getLocaleName() + " from " + linfo.getLocaleUrl());
            downloadResources(linfo.getLocaleName(), linfo.getLocaleUrl(), context);
            Log.i(TAG, "Finished!");
        }
    }
    
    private static void downloadResources(final String locale, final String url, Context context) {
    	try
		{
			File tmpZipFile = new File(context.getCacheDir().getAbsolutePath() + "/" + "res.zip");
	        String fileUrl = url;
	        Date now = new Date();
	        Date yesterday = new Date(now.getTime() - 24L * 60 * 60 * 1000);
	        String requestTimeText = serviceDateFormat.format(yesterday);
	        if (localizer.getLastUpdate(locale) != null) {
	            fileUrl += "&date=" + localizer.getLastUpdate(locale).replaceFirst(" ", "%20");
	        }
	        boolean fileLoaded = downloadToFile(fileUrl, tmpZipFile);
	        Resources resources = null;
	        ImageResourceDescriptions imageResourceDescriptions = null;
	        TextResourcesToDelete textsToDelete = null;
	        ImageResourcesToDelete imagesToDelete = null;
	        String serverTime = null;
	        if (fileLoaded) {
	            boolean fileUnzipped = ZipUtils.unzipArchive(tmpZipFile, context.getCacheDir());
	            if (fileUnzipped) {
	                resources =
	                        ResourcesParser.parseTextResources(new File(context.getCacheDir().getAbsolutePath()
	                                + "/" + TEXT_RESOURCE_FILE_NAME));
	                imageResourceDescriptions =
	                        ResourcesParser.parseImageResources(new File(context.getCacheDir()
	                                .getAbsolutePath() + "/" + IMAGE_RESOURCE_FILE_NAME));
	                textsToDelete =
	                        ResourcesParser.parseDeletedTextResources(new File(context.getCacheDir()
	                                .getAbsolutePath() + "/" + DELETED_TEXT_RESOURCE_FILE_NAME));
	                imagesToDelete =
	                        ResourcesParser.parseDeletedImageResources(new File(context.getCacheDir()
	                                .getAbsolutePath() + "/" + DELETED_IMAGE_RESOURCE_FILE_NAME));

	                serverTime =
	                        ResourcesParser.parseServerTime(new File(context.getCacheDir().getAbsolutePath()
	                                + "/" + TIMESTAMP_FILE_NAME));
	            }
	        }
	        if (serverTime != null) {
	            requestTimeText = serverTime;
	        }
	        if (resources != null && resources.getStringResources() != null) {
	        	
	        	Editor editor = localizer.storage.edit();
	            //Log.e(TAG, "EDITOR start");
	            
	            for (StringResource resource : resources.getStringResources()) {
	                //Log.e(locale, resource.getName() + " = " + resource.getContent());
	                localizer.putStringResource(editor, locale, resource.getName(), resource.getContent());
	            }
	            localizer.saveLastUpdate(editor, locale, requestTimeText);
	            
	            editor.commit();
	            //Log.e(TAG, "EDITOR commit");
	        }
	        if (textsToDelete != null && textsToDelete.getKeys() != null) {
	            for (KeyToDelete resource : textsToDelete.getKeys()) {
	                localizer.deleteString(resource.getKey());
	            }
	            localizer.saveLastUpdate(locale, requestTimeText);
	        }
	        if (imageResourceDescriptions != null
	                && imageResourceDescriptions.getImageResources() != null) {
	            for (ImageDescription image : imageResourceDescriptions.getImageResources()) {
	                File src = new File(context.getCacheDir().getAbsolutePath() + "/" + image.getZipFileName());
	                if (src.length() > 0) {
	                    File dst =
	                            new File(context.getFilesDir().getAbsolutePath() + "/" + locale + "_"
	                                    + image.getKey());
	                    src.renameTo(dst);
	                }
	            }
	            localizer.saveLastUpdate(locale, requestTimeText);
	        }
	        if (imagesToDelete != null && imagesToDelete.getKeys() != null) {
	            for (KeyToDelete resource : imagesToDelete.getKeys()) {
	                File fileToDelete =
	                        new File(context.getFilesDir().getAbsolutePath() + "/" + locale + "_"
	                                + resource.getKey());
	                fileToDelete.delete();
	            }
	            localizer.saveLastUpdate(locale, requestTimeText);
	        }
	        // clean up
	        File[] files = context.getCacheDir().listFiles();
	        for (File file : files) {
	            if (file.isFile() && file.exists()) {
	                file.delete();
	            }
	        }					
		}catch(Exception ex)
		{
			Log.e("ResourcesInitializerService", ex.getMessage(), ex);
		}
    }

    private void downloadResourcesWithLangDetection(final String languagesCodeResourceName,
            final String languagesNameResourceName, final String languagesUrlResourceName) {
        
    	new Thread(new Runnable()
		{
			
			@Override
			public void run()
			{
		    	localizer.setLanguagesCodeResourceName(languagesCodeResourceName);

		        String[] codes = localizer.getStringArray(languagesCodeResourceName);
		        String[] names = localizer.getStringArray(languagesNameResourceName);
		        String[] urls = localizer.getStringArray(languagesUrlResourceName);

		        for (int i = 0; i < codes.length && i < names.length && i < urls.length; i++) {
		            downloadResources(codes[i], urls[i], ResourcesInitializerService.this);
		        }

		        // search and download new locales
		        codes = localizer.getStringArray(languagesCodeResourceName);
		        names = localizer.getStringArray(languagesNameResourceName);
		        urls = localizer.getStringArray(languagesUrlResourceName);

		        for (int i = 0; i < codes.length && i < names.length && i < urls.length; i++) {
		            if (!localizer.isLocaleLoaded(codes[i])) {
		                downloadResources(codes[i], urls[i], ResourcesInitializerService.this);
		            }
		        }
			}
		}).start();
    }

    /**
     * 
     * @param url
     * @param outputFile
     * @return true if file was downloaded successfully
     */
     private static boolean downloadToFile(String url, File output) {
        if (!isDownloadAllowed()) {
            return false;
        }
        
        url = url.replaceAll("&amp;", "&");
        HttpGet getMethod = new HttpGet(url);
       
        try {
            ResponseHandler<byte[]> responseHandler = new ByteArrayResponseHandler();
            byte[] responseBody = client.execute(getMethod, responseHandler);
            if (output.exists()) {
                output.delete();
            }

            FileOutputStream fos = new FileOutputStream(output.getPath());

            fos.write(responseBody);
            fos.close();

        } catch (IOException ignore) {
            Log.i(TAG, "Failed to download locale: " + url, ignore);
            return false;
        } catch (NullPointerException ignore) {
            Log.i(TAG, "Failed to save locale: " + url, ignore);
            return false;
        }
        return true;
     }

     private static void init(Context c) {
         localizer = new Localizer(c);
         client = new DefaultHttpClient();
         serviceDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
         serviceDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
     }
}
