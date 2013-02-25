/**************************************************************************
 * TODO Copyright
 *
 * $Id: KeypadMapperMenu.java 182 2013-02-19 15:52:42Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/view/menu/KeypadMapperMenu.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view.menu;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.ShareFilesActivity;
import de.enaikoon.android.keypadmapper3.domain.FreezedLocationListener;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.domain.UndoAvailabilityListener;
import de.enaikoon.android.keypadmapper3.location.LocationProvider;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.utils.UnitsConverter;
import de.enaikoon.android.keypadmapper3.view.menu.MenuListener.OptionType;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class KeypadMapperMenu implements OnClickListener, FreezedLocationListener,
        UndoAvailabilityListener {

    private View homeBtn;

    private Button undoBtn;

    private Button photoBtn;

    private Button freezeGpsBtn;

    private Button keypadBtn;

    private View accuracyBtn;

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

    private MenuListener menuListener;

    private LocationListener listener = new LocationListener() {

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
    };

    private boolean preferenceMode = false;

    public KeypadMapperMenu(View menu) {

        this.menuView = menu;
        this.context = menu.getContext();

        homeBtn = menu.findViewById(R.id.menu_home);
        homeBtn.setOnClickListener(this);

        undoBtn = (Button) menu.findViewById(R.id.menu_undo);
        undoBtn.setOnClickListener(this);
        initUndoButton();

        photoBtn = (Button) menu.findViewById(R.id.menu_make_photo);
        photoBtn.setOnClickListener(this);
        freezeGpsBtn = (Button) menu.findViewById(R.id.menu_freeze_gps);
        freezeGpsBtn.setOnClickListener(this);
        initFreezeGpsButton();

        keypadBtn = (Button) menu.findViewById(R.id.menu_keypad);
        keypadBtn.setOnClickListener(this);
        accuracyBtn = menu.findViewById(R.id.menu_gps_accuracy);
        accuracyBtn.setOnClickListener(this);
        // init(menu.getContext());

        accuracyText = (TextView) menu.findViewById(R.id.menu_gps_accureacy_info);

        housenumberCount = (TextView) menu.findViewById(R.id.menu_addresses_count);

        updateLocation(locationProvider.getLastKnownLocation());

        init(menu);
    }

    public MenuListener getMenuListener() {
        return menuListener;
    }

    public void interceptClick(MenuListener.OptionType type) {
        switch (type) {
        case SHARE:
            Intent share = new Intent(context, ShareFilesActivity.class);
            context.startActivity(share);
            break;
        case EDITOR_TOGGLE:
            if (menuListener != null) {
                menuListener.onMenuOptionClicked(OptionType.EDITOR_TOGGLE);
            }
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
            onClick(keypadBtn);
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
            PopUpMenu dialog = null;
            dialog = new PopUpMenu(context, menuView.getHeight(), menuView.getWidth(), this);

            dialog.show();
            dialog.setFreezeImageStatus(mapper.getFreezedLocation() != null);
            dialog.setUndoImageStatusPossible(mapper.isUndoAvailable());
            dialog.setSettingsImageStatus(preferenceMode);
            dialog.setSatelliteInfoStatus(gpsInfoMode);
        } else if (menuListener != null) {
            if (view == undoBtn) {
                menuListener.onMenuOptionClicked(OptionType.UNDO);
            } else if (view == photoBtn) {
                menuListener.onMenuOptionClicked(OptionType.CAMERA);
            } else if (view == keypadBtn) {
                menuListener.onMenuOptionClicked(OptionType.KEYPAD);
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
        locationProvider.removeLocationListener(listener);
        mapper.removeFreezedLocationListener(this);
        mapper.removeUndoListener(this);
        System.gc();
    }

    public void onResume() {
        locationProvider.addLocationListener(listener);
        mapper.addFreezedLocationListener(this);
        mapper.addUndoListener(this);
        homeBtn.setKeepScreenOn(settings.isKeepScreenOnEnabled());
        initFreezeGpsButton();
        initUndoButton();
        updateHousenumberCount();
    }

    public void setGpsInfoMode(boolean gpsInfoModeEnabled) {
        this.gpsInfoMode = gpsInfoModeEnabled;
        if (gpsInfoMode) {
            accuracyBtn.setBackgroundDrawable(localizer.getDrawable("icon_gps_precision_glow"));
            accuracyText.setTextColor(Color.WHITE);
            setPreferenceMode(false);
            setKeypadMode(false);
        } else {
            accuracyBtn.setBackgroundDrawable(localizer.getDrawable("icon_gps_precision"));
            accuracyText.setTextColor(Color.rgb(249, 190, 39));
        }
    }

    public void setKeypadMode(boolean enabled) {
        if (enabled) {
            keypadBtn.setBackgroundDrawable(localizer.getDrawable("icon_keypad_active"));
            setPreferenceMode(false);
            setGpsInfoMode(false);
        } else {
            keypadBtn.setBackgroundDrawable(localizer.getDrawable("icon_keypad"));
        }
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

        ((FrameLayout) view.findViewById(R.id.menu_home)).setBackgroundDrawable(localizer
                .getDrawable("icon_app_empty"));

        ((Button) view.findViewById(R.id.menu_undo)).setBackgroundDrawable(localizer
                .getDrawable("icon_undo_grey"));

        ((Button) view.findViewById(R.id.menu_freeze_gps)).setBackgroundDrawable(localizer
                .getDrawable("icon_freeze_inactive"));

        ((Button) view.findViewById(R.id.menu_make_photo)).setBackgroundDrawable(localizer
                .getDrawable("icon_camera"));

        ((FrameLayout) view.findViewById(R.id.menu_gps_accuracy)).setBackgroundDrawable(localizer
                .getDrawable("icon_gps_precision"));

        ((Button) view.findViewById(R.id.menu_keypad)).setBackgroundDrawable(localizer
                .getDrawable("icon_keypad_active"));

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((ImageView) view.findViewById(R.id.menu_delimenter)).setImageDrawable(localizer
                    .getDrawable("icon_line_menu"));
        } else {
            ((ImageView) view.findViewById(R.id.menu_delimenter)).setImageDrawable(localizer
                    .getDrawable("line_yellow"));
        }

    }

    private void initFreezeGpsButton() {
        if (mapper.getFreezedLocation() == null) {
            freezeGpsBtn.setBackgroundDrawable(localizer.getDrawable("icon_freeze_inactive"));
        } else {
            freezeGpsBtn.setBackgroundDrawable(localizer.getDrawable("icon_freeze_active"));
        }
    }

    private void initUndoButton() {
        if (mapper.isUndoAvailable()) {
            undoBtn.setBackgroundDrawable(localizer.getDrawable("icon_undo"));
        } else {
            undoBtn.setBackgroundDrawable(localizer.getDrawable("icon_undo_grey"));
        }
        undoBtn.invalidate();
    }

    private void updateHousenumberCount() {
        housenumberCount.setText("" + mapper.getHouseNumberCount());
    }

    private void updateLocation(Location currentLocation) {
        String locationStatus = "";
        if (currentLocation != null) {
            if (KeypadMapperApplication.getInstance().getSettings().getMeasurement()
                    .equalsIgnoreCase("m")) {
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
}
