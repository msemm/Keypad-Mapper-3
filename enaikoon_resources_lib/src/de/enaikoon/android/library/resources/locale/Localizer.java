/**************************************************************************
 * Copyright
 *
 * $Id: Localizer.java 2 2012-12-07 08:13:11Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/locale/Localizer.java $
 **************************************************************************/

package de.enaikoon.android.library.resources.locale;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Chronometer;
import de.enaikoon.android.library.resources.utils.NinePatchChunk;

/**
 * Entry point for saving remote resources and retrieving saved resources.<BR>
 * If locale provider is not set then system locale is used (via
 * <code>System.getProperty("user.language");</code>)
 */
public class Localizer {

    public static interface LocaleProvider {
        String getLocale();
    }

    private static final String STORAGE_FILE_NAME = "resources";

    public SharedPreferences storage;

    private Context context;

    private LocaleProvider localeProvider = null;

    private String languagesCodeResourceName;

    private String densityDpi = null;

    private static final String TAG = "Localizer";

    public Localizer(Context context) {
        storage =
                context.getApplicationContext().getSharedPreferences(STORAGE_FILE_NAME,
                        Context.MODE_PRIVATE);
        this.context = context;
        densityDpi = getDensityDpi();
    }

    public Localizer(Context context, String languagesCodeResourceName) {
        this(context);
        setLanguagesCodeResourceName(languagesCodeResourceName);
    }

    /**
     * Returns 9-patch localized image. This method use locale that is provided
     * by locale provider or system settings.<br/>
     * <b>This image should be compiled.</b>
     * 
     * @param name
     * @return
     */
    public Drawable get9PatchDrawable(String name) {
        return get9PatchDrawable(name, getLocale());
    }

    /**
     * Returns 9-patch localized image<br/>
     * <b>This image should be compiled.</b>
     * 
     * @param name
     * @param locale
     * @return
     */
    public Drawable get9PatchDrawable(String name, String locale) {
        File imageFile =
                new File(context.getFilesDir() + "/" + locale + "_" + densityDpi + "_" + name);
        if (!imageFile.exists()) {
            imageFile = new File(context.getFilesDir() + "/" + locale + "_" + name);
        }
        Drawable image = null;
        if (imageFile.exists() && imageFile.isFile()) {
            Resources res = context.getResources();
            try {
                InputStream stream = new FileInputStream(imageFile);
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) {
                    byte[] chunk = bitmap.getNinePatchChunk();
                    boolean result = NinePatch.isNinePatchChunk(chunk);
                    NinePatchChunk npc = NinePatchChunk.deserialize(chunk);
                    image = new NinePatchDrawable(res, bitmap, chunk, npc.mPaddings, null);
                }
            } catch (IOException e) {
                Log.w(TAG,
                        "Failed to open 9-patch image: " + imageFile.getName() + ". Error: "
                                + e.getMessage());
            }
        }
        if (image == null) {
            int id =
                    context.getResources()
                            .getIdentifier(name, "drawable", context.getPackageName());
            if (id != 0) {
                image = context.getResources().getDrawable(id);
            }
        }
        return image;
    }

    /**
     * Returns localized image. This method use locale that is provided by
     * locale provider or system settings
     * 
     * 
     * @param name
     * @return
     */
    public Drawable getDrawable(String name) {
        return getDrawable(name, getLocale());
    }

    /**
     * Returns localized image
     * 
     * @param name
     * @param locale
     * @return
     */
    public Drawable getDrawable(String name, String locale) {
        File imageFile =
                new File(context.getFilesDir() + "/" + locale + "_" + densityDpi + "_" + name);
        if (!imageFile.exists()) {
            imageFile = new File(context.getFilesDir() + "/" + locale + "_" + name);
        }
        Drawable image = null;
        if (imageFile.exists() && imageFile.isFile()) {
            // Drawable image =
            // Drawable.createFromPath(imageFile.getAbsolutePath());
            Bitmap bmp = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bmp != null) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                bmp.setDensity(dm.densityDpi);
                image = new BitmapDrawable(context.getResources(), bmp);
            }
        }
        if (image == null) {
            int id =
                    context.getResources()
                            .getIdentifier(name, "drawable", context.getPackageName());
            if (id != 0) {
                image = context.getResources().getDrawable(id);
            }

        }
        return image;
    }

    /**
     * Returns localized image. This method use locale that is provided by
     * locale provider or system settings
     * 
     * @deprecated use getImage(String name) instead
     * 
     * @param name
     * @return
     */
    @Deprecated
    public Drawable getLocalizedImage(String name) {
        return getDrawable(name, getLocale());
    }

    /**
     * Returns localized image
     * 
     * @deprecated use getImage(String name, String locale) instead
     * 
     * @param name
     * @param locale
     * @return
     */
    @Deprecated
    public Drawable getLocalizedImage(String name, String locale) {
        return getDrawable(name, locale);
    }

    /**
     * Returns localized text resource. This method use locale that is provided
     * by locale provider or system settings
     * 
     * @deprecated use getString(String name) instead
     * 
     * @param name
     *            resource name
     * @return localized text
     */
    @Deprecated
    public String getLocalizedString(String name) {
        return getString(name, getLocale());
    }

    /**
     * Returns localized text resource
     * 
     * @deprecated use getString(String name, String locale) instead
     * 
     * @param name
     * @param locale
     * @return
     */
    @Deprecated
    public String getLocalizedString(String name, String locale) {
        return getString(name, locale);
    }

    /**
     * Returns localized text resource. This method use locale that is provided
     * by locale provider or system settings
     * 
     * @param name
     *            resource name
     * @return localized text
     */
    public String getString(String name) {
        return getString(name, getLocale());
    }

    /**
     * Returns localized text resource
     * 
     * @param name
     * @param locale
     * @return
     */
    public String getString(String name, String locale) {
        String text = storage.getString(locale + "_" + name, null);
        if (text != null && !text.equals("")) {
        	if (text.contains("&lt;") || text.contains("&gt;"))
        	{
        		text = Html.fromHtml(text).toString();
        	}
        	
        	text = text.replace("\\'", "\'").replace("\\u0020", " ").replace("\\n", "\n");
        	
            return text;
        } else {
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            if (id == 0) {
                return null;
            }
            return context.getString(id);
        }
    }

    /**
     * Returns the array of String. String should be delimited by "\n".
     * 
     * @param name
     * @return array of the strings
     */
    public String[] getStringArray(String name) {
        return getStringArray(name, getLocale());
    }

    /**
     * Returns the array of String. String should be delimited by "\n".
     * 
     * @param name
     * @param locale
     * @return array of the strings
     */
    public String[] getStringArray(String name, String locale) {
        String text = getString(name, locale);
        if (text != null) {
            return text.split("\n");
        } else {
            return new String[0];
        }
    }

    /**
     * This method should be used when application using resources autoupdate
     * functionality. Before switch locale it should be checked for
     * availability.
     * 
     * @param localeCode
     * @return if resources for specified locale was loaded return true
     *         otherwise false
     */
    public boolean isLocaleLoaded(String localeCode) {
        int id =
                context.getResources().getIdentifier(languagesCodeResourceName, "string",
                        context.getPackageName());
        String[] buildInLanguages = context.getString(id).split("\n");
        for (String buildLang : buildInLanguages) {
            if (localeCode.equalsIgnoreCase(buildLang)) {
                return true;
            }
        }
        if (getLastUpdate(localeCode) != null) {
            return true;
        }
        return false;
    }

    /**
     * Save the date when specified locale was updated last time
     * 
     * @param locale
     * @param dateInString
     *            date format yyyy-MM-dd HH:mm
     */
    public void saveLastUpdate(String locale, String dateInString) {
        Editor editor = storage.edit();
        editor.putString("date_" + locale, dateInString);
        editor.commit();
    }
    
    /**
     * Save the date when specified locale was updated last time
     * 
     * @param locale
     * @param dateInString
     *            date format yyyy-MM-dd HH:mm
     */
    public void saveLastUpdate(Editor editor, String locale, String dateInString) {
        editor.putString("date_" + locale, dateInString);
    }    

    /**
     * Set locale provider for indicating what text resources should be used
     * 
     * @param localeProvider
     */
    public void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    /**
     * Remove text resource from the storage
     * 
     * @param key
     */
    protected void deleteString(String key) {
        Editor editor = storage.edit();
        editor.remove(key);
        editor.commit();
    }

    protected String getDensityDpi() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        switch (metrics.densityDpi) {
        case DisplayMetrics.DENSITY_LOW:
            return "ldpi";
        case DisplayMetrics.DENSITY_MEDIUM:
            return "mdpi";
        case DisplayMetrics.DENSITY_HIGH:
            return "hdpi";
        case DisplayMetrics.DENSITY_XHIGH:
            return "xhdpi";
            // case DisplayMetrics.DENSITY_DEFAULT:
            // return "";
        default:
            return "";

        }
    }

    /**
     * returns date when <code>locale</code> recieve update last time.
     * 
     * @param locale
     * @return date in format yyyy-MM-dd HH:mm
     */
    protected String getLastUpdate(String locale) {
        return storage.getString("date_" + locale, null);
    }

    /**
     * Save the text resource for specified locale and value
     * 
     * @param locale
     *            resource locale
     * @param name
     *            resource name
     * @param value
     *            resource value
     */
    protected void putStringResource(String locale, String name, String value) {
        Editor editor = storage.edit();
        editor.putString(locale + "_" + name, value);
        editor.commit();
    }
    
    /**
     * Save the text resource for specified locale and value
     * 
     * @param locale
     *            resource locale
     * @param name
     *            resource name
     * @param value
     *            resource value
     */
    protected void putStringResource(Editor editor, String locale, String name, String value) {
        editor.putString(locale + "_" + name, value);
    }    

    protected void setLanguagesCodeResourceName(String languagesCodeResourceName) {
        if (context.getResources().getIdentifier(languagesCodeResourceName, "string",
                context.getPackageName()) == 0) {
            throw new IllegalArgumentException("Specified resource is not found");
        }
        this.languagesCodeResourceName = languagesCodeResourceName;
    }

    private String getLocale() {
        if (localeProvider == null) {
            return System.getProperty("user.language");
        } else {
            return localeProvider.getLocale();
        }
    }
    
    /**
     * Return a localized formatted resource, substituting the format arguments
     * 
     * @param name
     * @param locale - if null, getLocale() will be used
     * @param string arguments
     * @return
     */
    public String getString(String name, String locale, Object... formatArgs) {
    	if (locale==null)
    	{
    		locale=getLocale();
    	}
    	
        String text = storage.getString(locale + "_" + name, null);
        if (text != null && !text.equals("")) {
        	String formattedText = String.format(text,formatArgs); 
        	if (formattedText.contains("&lt;") || formattedText.contains("&gt;"))
        	{
        		formattedText = Html.fromHtml(formattedText).toString();
        	}
        	
        	formattedText = formattedText.replace("\\'", "\'").replace("\\u0020", " ").replace("\\n", "\n");
        	
            return formattedText;
        } else {
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            if (id == 0) {
                return null;
            }
            
            text = context.getString(id);
            
            return String.format(text,formatArgs);
        }
    }    

}
