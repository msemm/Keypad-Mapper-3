/**************************************************************************
 * TODO Copyright
 *
 * $Id: KeypadMapperMenu.java 182 2013-02-19 15:52:42Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/view/menu/KeypadMapperMenu.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view.menu;

import java.util.Calendar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.ShareFilesActivity;
import de.enaikoon.android.keypadmapper3.domain.FreezedLocationListener;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.domain.UndoAvailabilityListener;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;
import de.enaikoon.android.keypadmapper3.view.AudioNoteDialog;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener.OptionType;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class KeypadMapperMenu implements OnClickListener, FreezedLocationListener,
        UndoAvailabilityListener, LocationListener, OnDismissListener {

    private View homeBtn;

    private ImageButton undoBtn;

    private ImageButton photoBtn;

    private ImageButton freezeGpsBtn;

    private ImageButton audioBtn;

    private ImageButton accuracyBtn;

    private TextView accuracyText;

    private TextView housenumberCount;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private Mapper mapper = KeypadMapperApplication.getInstance().getMapper();

    private LocationProvider locationProvider = KeypadMapperApplication.getInstance()
            .getLocationProvider();

    private KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();

    private Context context;

    private View menuView;

    private boolean gpsInfoMode = false;
    
    private boolean useCompass = false;

    private MenuListener menuListener;
    
    private ImageButton shareButton;
    private ImageButton settingsButton;
    private ImageButton keypadButton;
   
    private boolean preferenceMode = false;
    
    private MediaPlayer mplayer;
    private String tempFile;
    private String actualFile;
    
    private AudioRecord ar = null;
    
    public KeypadMapperMenu(View menu) {

        this.menuView = menu;
        this.context = menu.getContext();
        
        mplayer = new MediaPlayer();
       
        homeBtn = menu.findViewById(R.id.menu_home);
        homeBtn.setOnClickListener(this);

        undoBtn = (ImageButton) menu.findViewById(R.id.menu_undo);
        undoBtn.setOnClickListener(this);
        initUndoButton();

        photoBtn = (ImageButton) menu.findViewById(R.id.menu_make_photo);
        photoBtn.setOnClickListener(this);
        freezeGpsBtn = (ImageButton) menu.findViewById(R.id.menu_freeze_gps);
        freezeGpsBtn.setOnClickListener(this);
        initFreezeGpsButton();
       
        accuracyBtn = (ImageButton) menu.findViewById(R.id.menu_gps_accuracy);
        accuracyBtn.setOnClickListener(this);
        
        accuracyText = (TextView) menu.findViewById(R.id.menu_gps_accureacy_info);

        housenumberCount = (TextView) menu.findViewById(R.id.menu_addresses_count);

        audioBtn = (ImageButton) menu.findViewById(R.id.menu_audio);
        audioBtn.setOnClickListener(this);
        
        shareButton = (ImageButton) menu.findViewById(R.id.menu_share);
        if (shareButton != null) {
            shareButton.setOnClickListener(this);
            updateShareIcon();
        }
        
        settingsButton = (ImageButton) menu.findViewById(R.id.menu_settings);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(this);
            updateSettingsButton();
        }
        
        keypadButton = (ImageButton) menu.findViewById(R.id.menu_keypad);
        if (keypadButton != null) {
            keypadButton.setOnClickListener(this);
            updateKeypadButton();
        }
        
        if (mapper.getCurrentLocation() == null) {
            determineToUseCompass(0.0f);
            updateGpsIcon();
        } else {
            updateLocation(mapper.getCurrentLocation());
        }
        
        init(menu);
    }

    public MenuListener getMenuListener() {
        return menuListener;
    }

    public void interceptClick(MenuListener.OptionType type) {
        switch (type) {
        case SHARE:
            if (KeypadMapperApplication.getInstance().isAnyDataAvailable()) {
                Intent share = new Intent(context, ShareFilesActivity.class);
                context.startActivity(share);
            }
            break;
        case ADDRESS_EDITOR:
            if (menuListener != null) {
                menuListener.onMenuOptionClicked(OptionType.ADDRESS_EDITOR);
            }
            break;
        case AUDIO:
            onClick(audioBtn);
            break;
        case FREEZE_GPS:
            onClick(freezeGpsBtn);
            break;
        case CAMERA:
            onClick(photoBtn);
            break;
        case GPS_INFO:
            onClick(accuracyBtn);
            break;
        case UNDO:
            onClick(undoBtn);
            break;
        case KEYPAD:
            if (menuListener != null) {
                menuListener.onMenuOptionClicked(OptionType.KEYPAD);
            }
            break;
        case SETTINGS:
            menuListener.onMenuOptionClicked(OptionType.SETTINGS);
            break;
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        if (view == homeBtn) {
            Log.d("Keypad", "recreating menu");
            PopUpMenu dialog = null;
            dialog = new PopUpMenu(context, menuView.getHeight(), menuView.getWidth(), this);
            dialog.show();
        } else if (view == audioBtn) {
            audioDialog();
        } else if (view == shareButton && view != null) {
            interceptClick(OptionType.SHARE);
        } else if (view == settingsButton && view != null) {
            interceptClick(OptionType.SETTINGS);
        } else if (view == keypadButton && view != null) {
            interceptClick(OptionType.KEYPAD);
        } else if (menuListener != null) {
            if (view == undoBtn) {
                menuListener.onMenuOptionClicked(OptionType.UNDO);
            } else if (view == photoBtn) {
                menuListener.onMenuOptionClicked(OptionType.CAMERA);
            } else if (view == accuracyBtn) {
                menuListener.onMenuOptionClicked(OptionType.GPS_INFO);
            } else if (view == freezeGpsBtn) {
                menuListener.onMenuOptionClicked(OptionType.FREEZE_GPS);
                initFreezeGpsButton();
            }
        } 
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.enaikoon.android.keypadmapper3.domain.FreezedLocationListener#
     * onFreezedLocationChanged(android.location.Location)
     */
    @Override
    public void onFreezedLocationChanged(Location location) {
        initFreezeGpsButton();
    }

    public void onPause() {
        locationProvider.removeLocationListener(this);
        mapper.removeFreezedLocationListener(this);
        mapper.removeUndoListener(this);
       
        mplayer.release();
        // just in case
        KeypadMapperApplication.getInstance().releaseRecorder();

        System.gc();
    }

    public void onResume() {
        mplayer = new MediaPlayer();
        
        locationProvider.addLocationListener(this);
        mapper.addFreezedLocationListener(this);
        mapper.addUndoListener(this);
        homeBtn.setKeepScreenOn(settings.isKeepScreenOnEnabled());
        initFreezeGpsButton();
        initUndoButton();
        updateHousenumberCount();
        if (mapper.getCurrentLocation() == null) {
            determineToUseCompass(0.0f);
            updateGpsIcon();
        } else {
            updateLocation(mapper.getCurrentLocation());
        }
    }

    public void setGpsInfoMode(boolean gpsInfoModeEnabled) {
        this.gpsInfoMode = gpsInfoModeEnabled;
        updateGpsIcon();
        if (gpsInfoMode) {
            accuracyText.setTextColor(Color.WHITE);
            setPreferenceMode(false);
            setKeypadMode(false);
            
        } else {
            accuracyText.setTextColor(Color.rgb(249, 190, 39));
        }
    }

    public void setKeypadMode(boolean enabled) {
        if (enabled) {
            setPreferenceMode(false);
            setGpsInfoMode(false);
        } 
        updateKeypadButton();
    }

    public void setMenuListener(MenuListener menuListener) {
        this.menuListener = menuListener;
    }

    public void setPreferenceMode(boolean preferenceModeEnabled) {
        preferenceMode = preferenceModeEnabled;
        if (preferenceMode) {
            setGpsInfoMode(false);
            setKeypadMode(false);
        }
        updateSettingsButton();
        updateKeypadButton();
    }
    
    public boolean isPreferenceMode() {
        return preferenceMode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.enaikoon.android.keypadmapper3.domain.UndoAvailabilityListener#
     * undoStateChanged(boolean)
     */
    @Override
    public void undoStateChanged(boolean undoAvailable) {
        initUndoButton();
        updateHousenumberCount();
    }

    protected boolean isExtendedEditorEnabled() {
        return KeypadMapperApplication.getInstance().isExtendedEditorEnabled();
    }

    private void init(View view) {

        ((FrameLayout) view.findViewById(R.id.menu_home)).setBackgroundResource(R.drawable.icon_app_empty);
        audioBtn.setImageDrawable(localizer.getDrawable("audio"));
        updateGpsIcon();
       
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((ImageView) view.findViewById(R.id.menu_delimenter)).setImageDrawable(localizer.getDrawable("icon_line_menu"));
        } else {
            ((ImageView) view.findViewById(R.id.menu_delimenter)).setImageDrawable(localizer.getDrawable("line_yellow"));
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocation(location);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void initFreezeGpsButton() {
        if (mapper.getFreezedLocation() == null) {
            freezeGpsBtn.setImageResource(R.drawable.icon_freeze_inactive);
        } else {
            freezeGpsBtn.setImageResource(R.drawable.icon_freeze_active);
        }
    }

    private void initUndoButton() {
        if (mapper.isUndoAvailable()) {
            undoBtn.setImageResource(R.drawable.icon_undo);
        } else {
            undoBtn.setImageResource(R.drawable.icon_undo_grey);
        }
        undoBtn.invalidate();
    }

    private void updateHousenumberCount() {
        housenumberCount.setText("" + mapper.getHouseNumberCount());
    }
    
    private void updateGpsIcon() {
        if (useCompass) {
            if (gpsInfoMode) {
                accuracyBtn.setImageResource(R.drawable.icon_gps_precision_needle_glow);
            } else {
                accuracyBtn.setImageResource(R.drawable.icon_gps_precision_needle);    
            }
        } else {
            if (gpsInfoMode) {
                accuracyBtn.setImageResource(R.drawable.icon_gps_precision_glow);
            } else {
                accuracyBtn.setImageResource(R.drawable.icon_gps_precision);    
            }
        }
    }

    private void updateLocation(Location currentLocation) {
        String locationStatus = "";
        if (currentLocation != null) {
            float recordedSpeed = currentLocation.getSpeed();
            
            // convert meters per second to km/h or mph
            if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
                // meters
                recordedSpeed = recordedSpeed * KeypadMapperSettings.M_PER_SEC_HAS_KM_PER_HOUR;
            } else {
                // feet
                recordedSpeed = recordedSpeed * KeypadMapperSettings.M_PER_SEC_HAS_MILES_PER_HOUR;
            }
            
            determineToUseCompass(recordedSpeed);
            updateGpsIcon();
            
            if (settings.getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
                locationStatus = localizer.getString("statusAccuracy");
                int accuracy = (int) currentLocation.getAccuracy();
                // locationStatus = String.format(locationStatus, accuracy);
                locationStatus = locationStatus.replaceAll("%d", "" + accuracy);
            } else {
                locationStatus = localizer.getString("statusAccuracyFt");
                int accuracy =
                        (int) UnitsConverter.convertMetersToFeets(currentLocation.getAccuracy());
                // locationStatus = String.format(locationStatus, accuracy);
                locationStatus = locationStatus.replaceAll("%d", "" + accuracy);
            }
        }
        accuracyText.setTypeface(Typeface.DEFAULT);
        accuracyText.setText(locationStatus);
    }
    
    public void determineToUseCompass(float speed) {
        if ((speed > 0.0 && settings.getUseCompassAtSpeed() > 0.0 && speed > settings.getUseCompassAtSpeed()) ||
                settings.getUseCompassAtSpeed() == 0 || !settings.isCompassAvailable()) {
            useCompass = false;
        } else {
            useCompass = true;
        }
    }
    
    public void setUseCompass(boolean uc) {
        useCompass = uc;
    }
    
    public boolean isUseCompass() {
        return useCompass;
    }
    
    public boolean isGpsInfoMode() {
        return gpsInfoMode;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog instanceof AudioNoteDialog) {
            audioBtn.setImageDrawable(localizer.getDrawable("audio"));
            
            KeypadMapperApplication.getInstance().releaseRecorder();
            dialog = null;
        }
    }
    
    public void audioDialog() {
        Location location = mapper.getCurrentLocation();
        
        if (!settings.isRecording()) {
            String message = localizer.getString("error_not_recording");
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }
        
        if (location == null) {
            String message = localizer.getString("error_no_location_for_audio");
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            return;
        }
        
        ar = KeypadMapperApplication.getInstance().getAudioRecorder();
        if (ar == null) {
            Toast.makeText(context, localizer.getString("error_audio_init"), Toast.LENGTH_LONG).show();
            return;
        }
        
        audioBtn.setImageDrawable(localizer.getDrawable("audio_glow"));
        Calendar cal = Calendar.getInstance();
        String basename = String.format("%tF_%tH-%tM-%tS", cal, cal, cal, cal);
        actualFile = KeypadMapperApplication.getInstance().getKeypadMapperDirectory().getAbsolutePath() + "/" + basename + ".wav";
        tempFile = KeypadMapperApplication.getInstance().getKeypadMapperDirectory().getAbsolutePath() + "/" + basename + ".tmp";
        
        AudioNoteDialog and = new AudioNoteDialog(context, ar, mplayer, tempFile, actualFile, location);
        WindowManager.LayoutParams lWindowParams = new WindowManager.LayoutParams();
        lWindowParams.copyFrom(and.getWindow().getAttributes());
        lWindowParams.width = WindowManager.LayoutParams.FILL_PARENT; // this is where the magic happens
        lWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        and.setOnDismissListener(this);
        and.show();
    }
    
    public void updateShareIcon() {
        if (shareButton == null)
            return;
        
        if (KeypadMapperApplication.getInstance().isAnyDataAvailable()) {
            shareButton.setImageResource(R.drawable.icon_share);
        } else {
            shareButton.setImageResource(R.drawable.icon_share_grey);
        }
    }
    
    public void updateSettingsButton() {
        if (settingsButton == null)
            return;
        
        if (isPreferenceMode()) {
            settingsButton.setImageResource(R.drawable.icon_settings_glow);
        } else {
            settingsButton.setImageResource(R.drawable.icon_settings);
        }
    }
    
    public void updateKeypadButton() {
        if (keypadButton == null)
            return;
        
        if (!isExtendedEditorEnabled() && !isPreferenceMode() && !gpsInfoMode) {
            keypadButton.setImageResource(R.drawable.icon_keypad_active);
        } else {
            keypadButton.setImageResource(R.drawable.icon_keypad);
        }
    }
}
