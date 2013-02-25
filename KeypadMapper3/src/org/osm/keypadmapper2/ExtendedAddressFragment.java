package org.osm.keypadmapper2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.HelpActivity;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.domain.Address;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.library.resources.locale.Localizer;

public class ExtendedAddressFragment extends Fragment implements OnFocusChangeListener,
        OnClickListener {

    private EditText textInputHousenumber;

    private EditText textInputHousename;

    private EditText textInputStreet;

    private EditText textInputPostcode;

    private EditText textInputCity;

    private EditText textInputCountry;

    private AddressInterface addressCallback;

    private Mapper mapper = KeypadMapperApplication.getInstance().getMapper();

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            saveChanges();
            addressCallback.extendedAddressActive();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private Address address;

    private TextWatcher housenumberWatcher = new TextWatcher() {

        @Override
        public void afterTextChanged(Editable s) {
            textInputHousenumber.removeTextChangedListener(housenumberWatcher);
            addressCallback.onHousenumberChanged(s.toString());
            address.setNumber(s.toString());
            mapper.setCurrentAddress(address);
            textInputHousenumber.addTextChangedListener(housenumberWatcher);
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private View helpBtn;

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_UP) {
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            addressCallback = (AddressInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement AddressInterface");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        if (view == helpBtn) {
            Intent help = new Intent(getActivity(), HelpActivity.class);
            startActivity(help);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.extended_address_fragment, container, false);
        textInputHousenumber = (EditText) view.findViewById(R.id.input_housenumber);
        textInputHousenumber.addTextChangedListener(housenumberWatcher);

        textInputHousename = (EditText) view.findViewById(R.id.input_housename);
        textInputHousename.setOnFocusChangeListener(this);
        textInputHousename.addTextChangedListener(textWatcher);

        textInputStreet = (EditText) view.findViewById(R.id.input_street);
        textInputStreet.setOnFocusChangeListener(this);
        textInputStreet.addTextChangedListener(textWatcher);

        textInputPostcode = (EditText) view.findViewById(R.id.input_postcode);
        textInputPostcode.setOnFocusChangeListener(this);
        textInputPostcode.addTextChangedListener(textWatcher);

        textInputCity = (EditText) view.findViewById(R.id.input_city);
        textInputCity.setOnFocusChangeListener(this);
        textInputCity.addTextChangedListener(textWatcher);

        textInputCountry = (EditText) view.findViewById(R.id.input_country);
        textInputCountry.setOnFocusChangeListener(this);
        textInputCountry.addTextChangedListener(textWatcher);

        helpBtn = view.findViewById(R.id.helpBtn);
        helpBtn.setOnClickListener(this);

        updateResources(view);
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                addressCallback.extendedAddressActive();
            }
        });
        return view;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.view.View.OnFocusChangeListener#onFocusChange(android.view.View,
     * boolean)
     */
    @Override
    public void onFocusChange(View view, boolean focused) {
        if (focused) {
            addressCallback.extendedAddressActive();
        }

    }

    @Override
    public void onPause() {
        saveChanges();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatedAddress();
    }

    public void saveChanges() {
        address = mapper.getCurrentAddress();
        address.setNumber(textInputHousenumber.getText().toString());
        address.setHousename(textInputHousename.getText().toString());
        address.setStreet(textInputStreet.getText().toString());
        address.setPostcode(textInputPostcode.getText().toString());
        address.setCity(textInputCity.getText().toString());
        address.setCountryCode(textInputCountry.getText().toString());
        mapper.setCurrentAddress(address);
    }

    public void updatedAddress() {
        address = mapper.getCurrentAddress();
        textInputHousenumber.setText(address.getNumber());
        textInputHousename.setText(address.getHousename());
        textInputStreet.setText(address.getStreet());
        textInputPostcode.setText(address.getPostcode());
        textInputCity.setText(address.getCity());
        textInputCountry.setText(address.getCountryCode());
    }

    public void updateHouseNumber(String number) {
        if (!textInputHousenumber.isFocused()) {
            updatedAddress();
        }
    }

    private void updateResources(View view) {
        Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();
        ((TextView) view.findViewById(R.id.input_desc_housenumber)).setText(localizer
                .getString("Housenumber"));
        ((TextView) view.findViewById(R.id.input_desc_housename)).setText(localizer
                .getString("Housename"));
        ((TextView) view.findViewById(R.id.input_desc_street)).setText(localizer
                .getString("Street"));
        ((TextView) view.findViewById(R.id.input_desc_postcode)).setText(localizer
                .getString("Postcode"));
        ((TextView) view.findViewById(R.id.input_desc_city)).setText(localizer.getString("City"));
        ((TextView) view.findViewById(R.id.input_desc_country)).setText(localizer
                .getString("Countrycode"));

        view.findViewById(R.id.helpBtn).setBackgroundDrawable(localizer.getDrawable("icon_help"));

        textInputHousenumber.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
        textInputHousename.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
        textInputStreet.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
        textInputPostcode.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
        textInputCity.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
        textInputCountry.setBackgroundDrawable(localizer
                .get9PatchDrawable("textfield_multiline_activated_holo_dark"));
    }
}
