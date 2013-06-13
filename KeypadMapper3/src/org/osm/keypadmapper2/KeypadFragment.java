package org.osm.keypadmapper2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.HelpActivity;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.LocationNotAvailableException;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.domain.Address;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.keypadmapper3.domain.UndoAvailabilityListener;
import de.enaikoon.android.keypadmapper3.geocode.ReverseGeocodeController;
import de.enaikoon.android.keypadmapper3.view.HideCursorEditText;
import de.enaikoon.android.keypadmapper3.view.HideCursorEditText.EditTextImeBackListener;
import de.enaikoon.android.keypadmapper3.view.menu.KeypadMapperMenu;
import de.enaikoon.android.library.resources.locale.Localizer;

public class KeypadFragment extends Fragment implements OnClickListener, UndoAvailabilityListener {

    private EditText textHousenumber;

    private TextView textGeoInfo;

    private TextView textlastHouseNumbers1;

    private TextView textlastHouseNumbers2;

    private TextView textlastHouseNumbers3;

    private double distance;

    private View helpBtn;

    private AddressInterface addressCallback;

    private ReverseGeocodeController geocodeController;

    private HideCursorEditText inputNotes;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();;

    private boolean cursorVisible = true;
    
    private View root;
    
    //private KeypadMapperMenu menu;

    private Mapper mapper = KeypadMapperApplication.getInstance().getMapper();

    private Address address;

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP
                && !KeypadMapperApplication.getInstance().getSettings()
                        .isLayoutOptimizationEnabled()) {
            float xTouch = ev.getX();
            float yTouch = ev.getY();
            int[] location = new int[2];
            helpBtn.getLocationOnScreen(location);
            DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
            int padding = (int) ((20 /* dp */* displayMetrics.density) + 0.5);
            if (xTouch >= location[0] - padding
                    && xTouch <= location[0] + padding + helpBtn.getWidth()
                    && yTouch >= location[1] - padding
                    && yTouch <= location[1] + padding + helpBtn.getHeight()) {
                onClick(helpBtn);
                return true;
            }
        }
        return false;
    }

    public void enableHousenumberView() {
        inputNotes.setCursorVisible(false);
        cursorVisible = false;
        InputMethodManager imm =
                (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputNotes.getWindowToken(), 0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            addressCallback = (AddressInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement AddressInterface");
        }
    }

    @Override
    public void onClick(View v) {
        enableHousenumberView();
        address = mapper.getCurrentAddress();
        addressCallback.extendedAddressInactive();
        String housenumber = (String) textHousenumber.getText().toString();
        textHousenumber.clearFocus();
        
        try {
            switch (v.getId()) {
            case R.id.helpBtn:
                Intent help = new Intent(getActivity(), HelpActivity.class);
                startActivity(help);
                break;
            case R.id.button_C:
                // clear
                keyboardVibrate();
                clearInfo();
                break;
            case R.id.button_DEL: {
                keyboardVibrate();
                // delete the last char
                if (housenumber.length() > 0) {
                    address.setNumber(housenumber.substring(0, housenumber.length()-1));
                    mapper.setCurrentAddress(address);
                }
                break;
            }
            case R.id.button_L:
                if (!KeypadMapperApplication.getInstance().getSettings().isRecording()) {
                    Toast.makeText(getActivity(), localizer.getString("notRecording"),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (mapper.getCurrentLocation() == null && mapper.getFreezedLocation() == null) {
                    throw new LocationNotAvailableException("Location is not available");
                }
                
                // must update address if user has used soft-keyboard
                if (housenumber.length() != 0) {
                    address.setNumber(localizer.getString("buttonLeft") + ": " + housenumber);
                }

                mapper.setCurrentAddress(address);
                // place address to the left
                mapper.saveCurrentAddress(0, distance);
                
                clearInfo();
                vibrate();
                break;
            case R.id.button_F:
                if (!KeypadMapperApplication.getInstance().getSettings().isRecording()) {
                    Toast.makeText(getActivity(), localizer.getString("notRecording"),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (mapper.getCurrentLocation() == null && mapper.getFreezedLocation() == null) {
                    throw new LocationNotAvailableException("Location is not available");
                }
                
                // must update address if user has used soft-keyboard
                if (housenumber.length() != 0) {
                    address.setNumber(localizer.getString("buttonFront") + ": " + housenumber);
                }
                mapper.setCurrentAddress(address);
                // place address forwards
                mapper.saveCurrentAddress(distance, 0);
                
                //menu.updateShareIcon();
                
                clearInfo();
                vibrate();
                break;
            case R.id.button_R:
                if (!KeypadMapperApplication.getInstance().getSettings().isRecording()) {
                    Toast.makeText(getActivity(), localizer.getString("notRecording"),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                             
                if (mapper.getCurrentLocation() == null && mapper.getFreezedLocation() == null) {
                    throw new LocationNotAvailableException("Location is not available");
                }

                // must update address if user has used soft-keyboard
                if (housenumber.length() != 0) {
                    address.setNumber(localizer.getString("buttonRight") + ": " + housenumber);
                }
                mapper.setCurrentAddress(address);
                
                
                // place address to the right
                mapper.saveCurrentAddress(0, -distance);
                //menu.updateShareIcon();
                
                clearInfo();
                vibrate();
                break;
            default:
                // all other buttons are used to add characters
                keyboardVibrate();
                housenumber += ((Button) v).getText();

                address.setNumber(housenumber);
                mapper.setCurrentAddress(address);
            }
        } catch (LocationNotAvailableException exception) {
            Toast.makeText(getActivity(), localizer.getString("locationNotAvailable"),
                    Toast.LENGTH_SHORT).show();
        }
        addressCallback.onHousenumberChanged(address.getNumber());
        updateLastHouseNumbers();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            cursorVisible = savedInstanceState.getBoolean("cursor");
            // height = savedInstanceState.getInt("btn_height");
        }
        Log.d("Keypad", "onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            view = inflater.inflate(R.layout.keypad_fragment_fadeout, container, false);
        } else {
            view = inflater.inflate(R.layout.keypad_fragment, container, false);
        }
        Log.d("Keypad", "onCreateView");
        address = mapper.getCurrentAddress();

        root = view;
        textHousenumber = (EditText) view.findViewById(R.id.text_housenumber);
        textHousenumber.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, 0);
                textHousenumber.setCursorVisible(true);
                return true;
            }
        });
        textHousenumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0, null);
            }
        });
        textHousenumber.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0, null);
                    
                    textHousenumber.setCursorVisible(false);
                } else {
                    textHousenumber.setCursorVisible(true);
                }
            }
        });
        textHousenumber.clearFocus();
        
        textlastHouseNumbers1 = (TextView) view.findViewById(R.id.text_last_housenumbers_1);
        textlastHouseNumbers2 = (TextView) view.findViewById(R.id.text_last_housenumbers_2);
        textlastHouseNumbers3 = (TextView) view.findViewById(R.id.text_last_housenumbers_3);
        textGeoInfo = (TextView) view.findViewById(R.id.text_geoinfo);

        helpBtn = view.findViewById(R.id.helpBtn);
        helpBtn.setOnClickListener(this);

        inputNotes = (HideCursorEditText) view.findViewById(R.id.input_text_name);
        inputNotes.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                address = mapper.getCurrentAddress();
                address.setNotes(s.toString());
                mapper.setCurrentAddress(address);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        inputNotes.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                inputNotes.setCursorVisible(true);
                cursorVisible = true;

            }
        });

        inputNotes.setOnFocusChangeListener(new OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    inputNotes.setCursorVisible(true);
                    cursorVisible = true;
                }
            }
        });
        inputNotes.setOnEditTextImeBackListener(new EditTextImeBackListener() {

            @Override
            public void onImeBack(HideCursorEditText editText, String text) {
                inputNotes.setCursorVisible(false);
                cursorVisible = false;
            }
        });
        inputNotes.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    inputNotes.setCursorVisible(false);
                    cursorVisible = false;
                }
                return false;
            }
        });
        inputNotes.setCursorVisible(cursorVisible);

        geocodeController = new ReverseGeocodeController(getActivity(), textGeoInfo);

        setupButtons((ViewGroup) view.findViewById(R.id.fragment_keypad));
        updateTextOnLayout(view);

        view.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                addressCallback.extendedAddressInactive();
                enableHousenumberView();
                return false;
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        mapper.removeUndoListener(this);
        geocodeController.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        distance = KeypadMapperApplication.getInstance().getSettings().getHouseNumberDistance();
        
        geocodeController.onResume();
        address = mapper.getCurrentAddress();
        textHousenumber.setText(address.getNumber());
        inputNotes.setText(address.getNotes());
        updateLastHouseNumbers();
        mapper.addUndoListener(this);
        textHousenumber.clearFocus();

        View middleButton = root.findViewById(R.id.keysLandMiddle);
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            root.findViewById(R.id.geoinfo_container).setVisibility(View.GONE);
            inputNotes.setVisibility(View.GONE);
            root.findViewById(R.id.keysRow1).setVisibility(View.GONE);
            if (middleButton != null) {
                middleButton.setVisibility(View.GONE);
                root.findViewById(R.id.delimiter1).setVisibility(View.GONE);
            }
        } else {
            root.findViewById(R.id.geoinfo_container).setVisibility(View.VISIBLE);
            inputNotes.setVisibility(View.VISIBLE);
            root.findViewById(R.id.keysRow1).setVisibility(View.VISIBLE);
            if (middleButton != null) {
                middleButton.setVisibility(View.VISIBLE);
                root.findViewById(R.id.delimiter1).setVisibility(View.VISIBLE);
            }
        }
        
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(inputNotes.getApplicationWindowToken(), 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("cursor", cursorVisible);
        // outState.putInt("btn_height", height);
        super.onSaveInstanceState(outState);
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.enaikoon.android.keypadmapper3.domain.UndoAvailabilityListener#
     * undoStateChanged(boolean)
     */
    @Override
    public void undoStateChanged(boolean undoAvailable) {
        updateLastHouseNumbers();
    }

    public void updateHousenumber(String number) {
        address = mapper.getCurrentAddress();
        this.textHousenumber.setText(number);
    }

    private void clearInfo() {
        textHousenumber.setText("");
        inputNotes.setText("");
        address.setNotes("");
        address.setNumber("");
        address.setHousename("");
        mapper.setCurrentAddress(address);
        // update extended view
        addressCallback.onAddressUpdated();
    }

    private int getScreenMaxHeight() {
        // initialize the DisplayMetrics object
        DisplayMetrics deviceDisplayMetrics = new DisplayMetrics();

        // populate the DisplayMetrics object with the display characteristics
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(deviceDisplayMetrics);

        // get the width and height
        int screenWidth = deviceDisplayMetrics.widthPixels;
        int screenHeight = deviceDisplayMetrics.heightPixels;

        int max = Math.max(screenWidth, screenHeight);
        return max;
    }

    private void initLettersRows(final View view) {
        int max = getScreenMaxHeight();

        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        int dpMax = (int) ((max / displayMetrics.density) + 0.5);

        if (dpMax > 480) {
            LinearLayout lastNumbersContainer =
                    (LinearLayout) view.findViewById(R.id.housenumber_container);

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) lastNumbersContainer.getLayoutParams();
            // Changes the height and width to the specified *pixels*
            params.height = params.height * 3 / 2;
            lastNumbersContainer.setLayoutParams(params);
            textlastHouseNumbers3.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams number2Params =
                    (FrameLayout.LayoutParams) textlastHouseNumbers2.getLayoutParams();
            number2Params.gravity = Gravity.CENTER_VERTICAL;

        } else {
            textlastHouseNumbers3.setVisibility(View.GONE);
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                || dpMax >= 580) {
            // do nothing
        } else if (dpMax > 480) {
            // remove one line
            //view.findViewById(R.id.keysRow3).setVisibility(View.GONE);
            ((Button) view.findViewById(R.id.buttonJ)).setText(localizer.getString("buttonSep1"));
            ((Button) view.findViewById(R.id.buttonK)).setText(localizer.getString("buttonSep2"));
            ((Button) view.findViewById(R.id.buttonL)).setText(localizer.getString("buttonSep3"));
        } else {
            // remove two lines
            view.findViewById(R.id.keysRow2).setVisibility(View.GONE);
            view.findViewById(R.id.keysRow3).setVisibility(View.GONE);
            ((Button) view.findViewById(R.id.buttonD)).setText(localizer.getString("buttonSep1"));
            ((Button) view.findViewById(R.id.buttonE)).setText(localizer.getString("buttonSep2"));
            ((Button) view.findViewById(R.id.buttonF)).setText(localizer.getString("buttonSep3"));
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                View lfrRow = view.findViewById(R.id.lfrRow);
                LayoutParams params = (LayoutParams) lfrRow.getLayoutParams();
                if (KeypadMapperApplication.getInstance().getSettings()
                        .isLayoutOptimizationEnabled()) {
                    params.weight = 1.55f;
                } else {
                    params.weight = 1.4f;
                }
                lfrRow.setLayoutParams(params);
            }
        }
    }

    /**
     * Performs additional setup steps for the buttons. Used for properties
     * which cannot be set as layout properties.
     * 
     * @param viewGroup
     */
    private void setupButtons(ViewGroup viewGroup) {
        // Set OnClickListener. The method specified via android:onClick must be
        // implemented in the main activity, so this workaround is needed.
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (ViewGroup.class.isInstance(viewGroup.getChildAt(i))) {
                setupButtons((ViewGroup) viewGroup.getChildAt(i));
            } else if (Button.class.isInstance(viewGroup.getChildAt(i))
                    | ImageButton.class.isInstance(viewGroup.getChildAt(i))) {
                viewGroup.getChildAt(i).setOnClickListener(this);
            }
        }
    }

    private void updateLastHouseNumbers() {
        textlastHouseNumbers1.setText("");
        textlastHouseNumbers2.setText("");
        textlastHouseNumbers3.setText("");
        String[] numbers = mapper.getLast3HouseNumbers();
        if (numbers.length == 1) {
            textlastHouseNumbers1.setText(numbers[0]);
            textlastHouseNumbers2.setText("");
            textlastHouseNumbers3.setText("");
        } else if (numbers.length == 2) {
            textlastHouseNumbers1.setText(numbers[0]);
            textlastHouseNumbers2.setText(numbers[1]);
            textlastHouseNumbers3.setText("");
        } else if (numbers.length == 3) {
            textlastHouseNumbers1.setText(numbers[0]);
            textlastHouseNumbers2.setText(numbers[1]);
            textlastHouseNumbers3.setText(numbers[2]);
        }
    }

    private void updateTextOnLayout(View view) {

        textHousenumber.setBackgroundDrawable(localizer.getDrawable("house_number_background"));

        inputNotes.setHint(localizer.getString("keypad_info_hint"));
        inputNotes.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));

        view.findViewById(R.id.helpBtn).setBackgroundDrawable(localizer.getDrawable("icon_help"));

        View delimiter1 = view.findViewById(R.id.delimiter1);
        if (delimiter1 != null) {
            delimiter1.setBackgroundDrawable(localizer.getDrawable("icon_line_menu"));
        }
        View delimiter2 = view.findViewById(R.id.delimiter2);
        if (delimiter2 != null) {
            delimiter2.setBackgroundDrawable(localizer.getDrawable("icon_line_menu"));
        }

        
        ((Button) view.findViewById(R.id.button1)).setText(localizer.getString("button1"));

        ((Button) view.findViewById(R.id.button1)).setText(localizer.getString("button1"));
        ((Button) view.findViewById(R.id.button2)).setText(localizer.getString("button2"));
        ((Button) view.findViewById(R.id.button3)).setText(localizer.getString("button3"));
        ((Button) view.findViewById(R.id.button4)).setText(localizer.getString("button4"));
        ((Button) view.findViewById(R.id.button5)).setText(localizer.getString("button5"));
        ((Button) view.findViewById(R.id.button6)).setText(localizer.getString("button6"));
        ((Button) view.findViewById(R.id.button7)).setText(localizer.getString("button7"));
        ((Button) view.findViewById(R.id.button8)).setText(localizer.getString("button8"));
        ((Button) view.findViewById(R.id.button9)).setText(localizer.getString("button9"));
        ((Button) view.findViewById(R.id.button0)).setText(localizer.getString("button0"));
        

        ((Button) view.findViewById(R.id.buttonA)).setText(localizer.getString("buttonA"));
        ((Button) view.findViewById(R.id.buttonB)).setText(localizer.getString("buttonB"));
        ((Button) view.findViewById(R.id.buttonC)).setText(localizer.getString("buttonC"));
        ((Button) view.findViewById(R.id.buttonD)).setText(localizer.getString("buttonD"));
        ((Button) view.findViewById(R.id.buttonE)).setText(localizer.getString("buttonE"));
        ((Button) view.findViewById(R.id.buttonF)).setText(localizer.getString("buttonF"));

        ((Button) view.findViewById(R.id.buttonG)).setText(localizer.getString("buttonG"));
        ((Button) view.findViewById(R.id.buttonH)).setText(localizer.getString("buttonH"));
        ((Button) view.findViewById(R.id.buttonI)).setText(localizer.getString("buttonI"));

        ((Button) view.findViewById(R.id.buttonJ)).setText(localizer.getString("buttonJ"));
        ((Button) view.findViewById(R.id.buttonK)).setText(localizer.getString("buttonK"));
        ((Button) view.findViewById(R.id.buttonL)).setText(localizer.getString("buttonL"));
        
        ((Button) view.findViewById(R.id.buttonSep1)).setText(localizer.getString("buttonSep1"));
        ((Button) view.findViewById(R.id.buttonSep2)).setText(localizer.getString("buttonSep2"));
        ((Button) view.findViewById(R.id.buttonSep3)).setText(localizer.getString("buttonSep3"));

        initLettersRows(view);
    }
    
    private void vibrate() {
        int vibTime = KeypadMapperApplication.getInstance().getSettings().getVibrationTime();
        if (vibTime != 0) {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(vibTime);
        }
    }
    
    private void keyboardVibrate() {
        int vibTime = KeypadMapperApplication.getInstance().getSettings().getKeyboardVibrationTime();
        if (vibTime != 0) {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(vibTime);
        }
    }
}
