/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view.menu;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener.OptionType;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class PopUpMenu extends Dialog implements android.view.View.OnClickListener {

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private View address;
    private ImageView addressIcon;

    private View share;

    private View undo;

    private View freeze;

    private View camera;

    private View gpsAccuracy;
    
    private View startStopGps;

    private View audio;
    
    private View settings;
    private ImageView settingsImage;
    
    private View contaner;

    private KeypadMapperMenu menu;

    private ImageView undoImage;

    private ImageView freezeImage;

    private ImageView satelliteImage;
    
    private ImageView startStopImage;
    
    private View keypad;
    private ImageView keypadImage;
    private TextView keypadCaption;

    private TextView freezeTxt;
    
    /**
     * @param context
     */
    public PopUpMenu(Context context, int topPadding, int leftPadding, KeypadMapperMenu menu) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.pop_up_menu);
        this.menu = menu;
        View view = findViewById(R.id.popUpMenuContainer);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.leftMargin = leftPadding;
        } else {
            params.topMargin = topPadding;
        }

        view.setLayoutParams(params);
        contaner = view;

        address = view.findViewById(R.id.popUpAddressEditorLine);
        address.setOnClickListener(this);
        addressIcon = (ImageView) view.findViewById(R.id.popUpAddressEditor);
        if (context.getResources().getBoolean(R.bool.is_tablet)) {
            address.setVisibility(View.GONE);
        } else {
            address.setVisibility(View.VISIBLE);
        }
        
        share = view.findViewById(R.id.popUpShareLine);
        share.setOnClickListener(this);
        
        undo = view.findViewById(R.id.popUpUndoLine);
        undo.setOnClickListener(this);

        freeze = view.findViewById(R.id.popUpFreezeGpsLine);
        freeze.setOnClickListener(this);

        camera = view.findViewById(R.id.popUpCameraLine);
        camera.setOnClickListener(this);

        gpsAccuracy = view.findViewById(R.id.popUpGpsAccuracyLine);
        gpsAccuracy.setOnClickListener(this);

        startStopGps = view.findViewById(R.id.popUpStartStopGpsLine);
        startStopGps.setOnClickListener(this);
        startStopImage = (ImageView) view.findViewById(R.id.popUpStartStopGps);

        audio = view.findViewById(R.id.popupAudioNoteLine);
        audio.setOnClickListener(this);
        TextView popupAudioText = (TextView) audio.findViewById(R.id.popupAudioText);
        popupAudioText.setText(localizer.getString("popupAudioText"));
        
        keypad = view.findViewById(R.id.popupKeypadLine);
        keypadImage = (ImageView) keypad.findViewById(R.id.popupKeypad);
        keypadCaption = (TextView) keypad.findViewById(R.id.popupKeypadText);
        
        keypadCaption.setText(localizer.getString("menu_address_keypad"));
        keypad.setOnClickListener(this);
        
        settings = view.findViewById(R.id.popUpSettingsLine);
        settings.setOnClickListener(this);
        settingsImage = (ImageView) view.findViewById(R.id.popUpSettingsImage);

        undoImage = (ImageView) view.findViewById(R.id.popUpUndo);
        freezeImage = (ImageView) view.findViewById(R.id.popUpFreezeGpsImage);
        freezeTxt = (TextView) view.findViewById(R.id.popUpFreezeGpsText);

        satelliteImage = (ImageView) view.findViewById(R.id.popUpGpsAccuracy);
        
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!super.dispatchTouchEvent(ev)) {
            this.hide();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        if (view == address) {
            menu.interceptClick(OptionType.ADDRESS_EDITOR);
            dismiss();
        } else if (view == share) {
            // don't handle clicks unless there's data
            if (KeypadMapperApplication.getInstance().isAnyDataAvailable()) {
                menu.interceptClick(OptionType.SHARE);
                dismiss();
            }
        } else if (view == undo) {
            menu.interceptClick(OptionType.UNDO);
            dismiss();
        } else if (view == freeze) {
            menu.interceptClick(OptionType.FREEZE_GPS);
            dismiss();
        } else if (view == camera) {
            menu.interceptClick(OptionType.CAMERA);
            dismiss();
        } else if (view == gpsAccuracy) {
            menu.interceptClick(OptionType.GPS_INFO);
            dismiss();
        } else if (view == startStopGps) {
            resolveStartStopGps();
            dismiss();
        } else if (view == settings) {
            menu.interceptClick(OptionType.SETTINGS);
            dismiss();
        } else if (view == keypad) {
            menu.interceptClick(OptionType.KEYPAD);
            dismiss();
        } else if (view == audio) {
            menu.interceptClick(OptionType.AUDIO);
            dismiss();
        }
    }

    public void setSatelliteInfoStatus(boolean active, boolean useCompass) {
        if (active) {
            if (!useCompass) {
                satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_wo_digits_glow"));
            } else {
                satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_precision_wo_digits_needle_glow"));
            }
        } else {
            if (!useCompass) {
                satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_wo_digits"));
            } else {
                satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_precision_wo_digits_needle"));
            }
        }
    }

    public void setUndoImageStatusPossible(boolean possible) {
        if (possible) {
            undoImage.setImageDrawable(localizer.getDrawable("icon_undo"));
        } else {
            undoImage.setImageDrawable(localizer.getDrawable("icon_undo_grey"));
        }
    }

    
    @Override
    protected void onStart() {
        super.onStart();
        init(contaner);
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.gc();
    }

    private void init(View view) {
        View background = view.findViewById(R.id.popUpMenuContainer);
        if (view.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            background.setBackgroundDrawable(localizer
                    .get9PatchDrawable("pop_up_window_small_land"));
        } else {
            background.setBackgroundDrawable(localizer.get9PatchDrawable("pop_up_window_small"));
        }

        if (KeypadMapperApplication.getInstance().isGpsRecording()) {
            startStopImage.setImageDrawable(localizer.getDrawable("stop_recording_gps"));
        } else {
            startStopImage.setImageDrawable(localizer.getDrawable("start_recording_gps"));
        }
        
        if (!menu.isGpsInfoMode() && menu.isExtendedEditorEnabled()) {
            addressIcon.setImageDrawable(localizer.getDrawable("icon_adress_editor_glow"));
        } else {
            addressIcon.setImageDrawable(localizer.getDrawable("icon_adress_editor"));
        }
        
        if (menu.isExtendedEditorEnabled()) {
            ((ImageView) view.findViewById(R.id.popUpAddressEditor)).setImageDrawable(localizer
                    .getDrawable("icon_adress_editor_glow"));
        } else {
            ((ImageView) view.findViewById(R.id.popUpAddressEditor)).setImageDrawable(localizer
                    .getDrawable("icon_adress_editor"));
        }
        
        ImageView popupShare = (ImageView) share.findViewById(R.id.popUpShare);
        if (!KeypadMapperApplication.getInstance().isAnyDataAvailable()) {
            popupShare.setImageDrawable(localizer.getDrawable("icon_share_grey"));
        } else {
            popupShare.setImageDrawable(localizer.getDrawable("icon_share"));
        }

        ((ImageView) view.findViewById(R.id.popUpUndo)).setImageDrawable(localizer
                .getDrawable("icon_undo"));

        if (KeypadMapperApplication.getInstance().getMapper().getFreezedLocation() == null) {
            freezeImage.setImageDrawable(localizer.getDrawable("icon_freeze_inactive"));
        } else {
            freezeImage.setImageDrawable(localizer.getDrawable("icon_freeze_active"));
        }

        ((ImageView) view.findViewById(R.id.popUpCamera)).setImageDrawable(localizer
                .getDrawable("icon_camera"));

        ((ImageView) view.findViewById(R.id.popUpGpsAccuracy)).setImageDrawable(localizer
                .getDrawable("icon_gps_wo_digits"));
       
        if (KeypadMapperApplication.getInstance().isGpsRecording()) {
            ((ImageView) view.findViewById(R.id.popUpStartStopGps)).setImageDrawable(localizer.getDrawable("stop_recording_gps"));
        } else {
            ((ImageView) view.findViewById(R.id.popUpStartStopGps)).setImageDrawable(localizer.getDrawable("start_recording_gps"));
        }
        
        if (menu.isPreferenceMode()) {
            settingsImage.setImageDrawable(localizer.getDrawable("icon_settings_glow"));
        } else {
            settingsImage.setImageDrawable(localizer.getDrawable("icon_settings"));
        }
      
        ((TextView) view.findViewById(R.id.popUpAddressEditorText)).setText(localizer
                    .getString("menu_address_editor"));
        
        ((TextView) view.findViewById(R.id.popUpUndoText))
                .setText(localizer.getString("menu_undo"));

        ((TextView) view.findViewById(R.id.popUpShareText)).setText(localizer
                .getString("menu_share"));

        ((TextView) view.findViewById(R.id.popUpFreezeGpsText)).setText(localizer
                .getString("menu_freeze_gps"));

        ((TextView) view.findViewById(R.id.popUpCameraText)).setText(localizer
                .getString("menu_camera"));

        ((TextView) view.findViewById(R.id.popUpGpsAccuracyText)).setText(localizer
                .getString("menu_gps_accuracy"));
        
        ((TextView) view.findViewById(R.id.popUpStartStopGpsText)).setText(localizer.getString("menu_recording_GPS"));
        
        ((TextView) view.findViewById(R.id.popUpSettingsText)).setText(localizer
                .getString("menu_settings"));
        
        keypadCaption.setText(localizer.getString("menu_address_keypad"));
                
        if (!menu.isGpsInfoMode() && !menu.isExtendedEditorEnabled() && !menu.isPreferenceMode()) {
            keypadImage.setImageDrawable(localizer.getDrawable("icon_keypad_active"));
        } else {
            keypadImage.setImageDrawable(localizer.getDrawable("icon_keypad"));
        }
        
        TextView popupAudioText = (TextView) audio.findViewById(R.id.popupAudioText);
        popupAudioText.setText(localizer.getString("popupAudioText"));
        
        setUndoImageStatusPossible(KeypadMapperApplication.getInstance().getMapper().isUndoAvailable());
        setSatelliteInfoStatus(menu.isGpsInfoMode(), menu.isUseCompass());
    }
    
    private void resolveStartStopGps() {
        if (KeypadMapperApplication.getInstance().isGpsRecording()) {
            // stop recording into the current file and
            // start recording into the next file
            KeypadMapperApplication.getInstance().stopGpsRecording();
            menu.setUseCompass(false);
            // show start icon
            startStopImage.setImageDrawable(localizer.getDrawable("start_recording_gps"));
            // move to keypad if satellite info view is active
        } else {
            startStopImage.setImageDrawable(localizer.getDrawable("stop_recording_gps"));
            KeypadMapperApplication.getInstance().startGpsRecording();
        }
        
        // adjust icons
        setSatelliteInfoStatus(menu.isGpsInfoMode(), menu.isUseCompass());
        menu.setGpsInfoMode(menu.isGpsInfoMode());
    }
}