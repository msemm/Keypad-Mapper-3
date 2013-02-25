package de.enaikoon.android.keypadmapper3;

import java.io.File;

import android.app.Application;
import android.os.Environment;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.library.resources.locale.Localizer;
import de.enaikoon.android.library.resources.locale.Localizer.LocaleProvider;

public class KeypadMapperApplication extends Application {

    public static KeypadMapperApplication getInstance() {
        return instance;
    }

    private Localizer localizer;

    private String cachedStreetFromNominatin;

    private String cachedPostCodeFromNominatin;

    private String osmInfoName = "";

    private LocationProvider locationProvider;

    private static KeypadMapperApplication instance;

    private File lastPhotoFile;

    private KeypadMapperSettings settings;

    private boolean extendedEditorEnabled = false;

    private Mapper mapper;

    private String screenToActivate = null;

    public String getCachedPostCodeFromNominatin() {
        return cachedPostCodeFromNominatin;
    }

    public String getCachedStreetFromNominatin() {
        return cachedStreetFromNominatin;
    }

    public File getKeypadMapperDirectory() {
        File extStorage = Environment.getExternalStorageDirectory();
        File kpmFolder =
                new File(extStorage.getAbsolutePath() + File.separatorChar
                        + localizer.getString("app_name"));
        return kpmFolder;
    }

    public File getLastPhotoFile() {
        return lastPhotoFile;
    }

    public Localizer getLocalizer() {
        return localizer;
    }

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    public Mapper getMapper() {
        return mapper;
    }

    public String getScreenToActivate() {
        return screenToActivate;
    }

    public KeypadMapperSettings getSettings() {
        return settings;
    }

    public boolean isExtendedEditorEnabled() {
        return extendedEditorEnabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));

        settings = new KeypadMapperSettings(getApplicationContext());

        localizer = new Localizer(getApplicationContext(), "lang_support_codes");
        localizer.setLocaleProvider(new LocaleProvider() {

            @Override
            public String getLocale() {
                return settings.getCurrentLanguageCode();
            }
        });

        // final LogConfigurator logConfigurator = new LogConfigurator();
        //
        // logConfigurator.setFileName(getKeypadMapperDirectory() +
        // File.separator
        // + "keypadmapper.log");
        // logConfigurator.setRootLevel(Level.DEBUG);
        // // Set log level of a specific logger
        // logConfigurator.setLevel("org.apache", Level.ERROR);
        // logConfigurator.configure();

        locationProvider = new LocationProvider(getApplicationContext());

        mapper = new Mapper(this);
    }

    public void setCachedPostCodeFromNominatin(String cachedPostCodeFromNominatin) {
        this.cachedPostCodeFromNominatin = cachedPostCodeFromNominatin;
    }

    public void setCachedStreetFromNominatin(String cachedStreetFromNominatin) {
        this.cachedStreetFromNominatin = cachedStreetFromNominatin;
    }

    public void setExtendedEditorEnabled(boolean extendedEditorEnabled) {
        this.extendedEditorEnabled = extendedEditorEnabled;
    }

    public void setLastPhotoFile(File lastPhotoFile) {
        this.lastPhotoFile = lastPhotoFile;
    }

    public void setScreenToActivate(String screenToActivate) {
        this.screenToActivate = screenToActivate;
    }

}
