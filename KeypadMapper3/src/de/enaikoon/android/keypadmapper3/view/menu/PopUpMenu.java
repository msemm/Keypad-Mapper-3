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
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener.OptionType;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class PopUpMenu extends Dialog implements android.view.View.OnClickListener {

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private View address;

    private View share;

    private View undo;

    private View freeze;

    private View camera;

    private View gpsAccuracy;

    private View settings;

    private View contaner;

    private KeypadMapperMenu menu;

    private ImageView undoImage;

    private ImageView freezeImage;

    private ImageView satelliteImage;

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

        settings = view.findViewById(R.id.popUpSettingsLine);
        settings.setOnClickListener(this);

        undoImage = (ImageView) view.findViewById(R.id.popUpUndo);
        freezeImage = (ImageView) view.findViewById(R.id.popUpFreezeGps);
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
            menu.interceptClick(OptionType.EDITOR_TOGGLE);
            this.hide();
        } else if (view == share) {
            menu.interceptClick(OptionType.SHARE);
            this.hide();
        } else if (view == undo) {
            menu.interceptClick(OptionType.UNDO);
            this.hide();
        } else if (view == freeze) {
            menu.interceptClick(OptionType.FREEZE_GPS);
            this.hide();
        } else if (view == camera) {
            menu.interceptClick(OptionType.CAMERA);
            this.hide();
        } else if (view == gpsAccuracy) {
            menu.interceptClick(OptionType.GPS_INFO);
            this.hide();
        } else if (view == settings) {
            menu.interceptClick(OptionType.SETTINGS);
            this.hide();
        }
    }

    public void setFreezeImageStatus(boolean frozen) {
        if (frozen) {
            freezeImage.setImageDrawable(localizer.getDrawable("icon_freeze_active"));
            freezeTxt.setText(localizer.getString("menu_unfreeze_gps"));
        } else {
            freezeImage.setImageDrawable(localizer.getDrawable("icon_freeze_inactive"));
            freezeTxt.setText(localizer.getString("menu_freeze_gps"));
        }
    }

    public void setSatelliteInfoStatus(boolean active) {
        if (active) {
            satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_wo_digits_glow"));
        } else {
            satelliteImage.setImageDrawable(localizer.getDrawable("icon_gps_wo_digits"));
        }
    }

    public void setSettingsImageStatus(boolean glow) {
        if (glow) {
            ((ImageView) contaner.findViewById(R.id.popUpSettigs)).setImageDrawable(localizer
                    .getDrawable("icon_settings_glow"));
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

        if (menu.isExtendedEditorEnabled()) {
            ((ImageView) view.findViewById(R.id.popUpAddressEditor)).setImageDrawable(localizer
                    .getDrawable("icon_keypad"));
        } else {
            ((ImageView) view.findViewById(R.id.popUpAddressEditor)).setImageDrawable(localizer
                    .getDrawable("icon_adress_editor"));
        }

        ((ImageView) view.findViewById(R.id.popUpUndo)).setImageDrawable(localizer
                .getDrawable("icon_undo"));

        ((ImageView) view.findViewById(R.id.popUpShare)).setImageDrawable(localizer
                .getDrawable("icon_share"));

        ((ImageView) view.findViewById(R.id.popUpFreezeGps)).setImageDrawable(localizer
                .getDrawable("icon_freeze_inactive"));

        ((ImageView) view.findViewById(R.id.popUpCamera)).setImageDrawable(localizer
                .getDrawable("icon_camera"));

        ((ImageView) view.findViewById(R.id.popUpGpsAccuracy)).setImageDrawable(localizer
                .getDrawable("icon_gps_wo_digits"));

        ((ImageView) view.findViewById(R.id.popUpSettigs)).setImageDrawable(localizer
                .getDrawable("icon_settings"));

        if (menu.isExtendedEditorEnabled()) {
            ((TextView) view.findViewById(R.id.popUpAddressEditorText)).setText(localizer
                    .getString("menu_address_keypad"));
        } else {
            ((TextView) view.findViewById(R.id.popUpAddressEditorText)).setText(localizer
                    .getString("menu_address_editor"));
        }

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

        ((TextView) view.findViewById(R.id.popUpSettingsText)).setText(localizer
                .getString("menu_settings"));
    }

}