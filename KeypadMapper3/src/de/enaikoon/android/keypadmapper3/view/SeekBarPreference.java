package de.enaikoon.android.keypadmapper3.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.R;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;

// based on http://robobunny.com/wp/2011/08/13/android-seekbar-preference/

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
    
    private final String LOGTAG = "KeypadMapper";
    
    private int defaultValue;
    private int mMaxValue;
    private int mMinValue;
    private int mInterval;
    private int mCurrentValue;
    private SeekBar mSeekBar;
    
    private TextView mStatusText;
    private TextView summaryText;
    private TextView titleText;
    private LinearLayout container;
    
    public static volatile int paddingLeft = 0;
    public static volatile int paddingRight = 0;
    
    private boolean isMetricValue;
    
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs, R.style.SettingsTheme);

        mSeekBar = new SeekBar(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, R.style.SettingsTheme);
        mSeekBar = new SeekBar(context, attrs);
    }
   
    /** MUST BE CALLED! */
    public void initPreference(int defVal, int minVal, int maxVal, int interval, int currentVal, boolean isMetric) {
        mSeekBar.setOnSeekBarChangeListener(null);
        defaultValue = defVal;
        mMinValue = minVal;
        mMaxValue = maxVal;
        mInterval = interval;
        mCurrentValue = currentVal;
        mSeekBar.setMax(mMaxValue - mMinValue);
        mSeekBar.setProgress(mCurrentValue - mMinValue);
        mSeekBar.setOnSeekBarChangeListener(this);
        isMetricValue = isMetric;

        updateStatusText();
    }
    
    @Override
    protected View onCreateView(ViewGroup parent){
        LinearLayout layout =  null;
        try {
            /*
            Log.d("Keypad", "child count: " + parent.getChildCount());
            if (parent != null && parent.getChildCount() > 0) {
                View firstChild = parent.getChildAt(0);

                Log.d("Keypad", "child type: " + firstChild.toString());
                if (firstChild instanceof LinearLayout) {
                    LinearLayout fcLayout = (LinearLayout) firstChild;
                    for(int index=0; index < fcLayout.getChildCount(); index++) {
                        View childx = fcLayout.getChildAt(index);
                        Log.d("Keypad", "childx " + childx);
                        Log.d("Keypad", "left = " + childx.getLeft() + " pleft: " + childx.getPaddingLeft());
                        if (childx instanceof RelativeLayout) {
                            if (childx.getLeft() > 0) {
                                paddingLeft = childx.getLeft();
                                paddingRight = childx.getPaddingRight();
                            }
                            if (childx.getPaddingLeft() > 0) {
                                paddingLeft = childx.getPaddingLeft();
                                paddingRight = childx.getPaddingRight();
                            }
                        }
                    }
                }
            }*/
            
            // get child
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            
            layout = (LinearLayout) mInflater.inflate(R.layout.seek_bar_preference, parent, false);
            //layout.setPadding(paddingLeft, 0, paddingRight, 0);
            
            summaryText = (TextView) layout.findViewById(android.R.id.summary);
            titleText = (TextView) layout.findViewById(android.R.id.title);
            container = (LinearLayout) layout.findViewById(R.id.seekBarPrefBarContainer);
            mStatusText = (TextView) layout.findViewById(R.id.seekBarPrefValue);
        } catch(Exception e) {
            Log.e(LOGTAG, "Error creating seek bar preference", e);
        }

        return layout;
        
    }
    
    @Override
    public void onBindView(View view) {
        super.onBindView(view);

        try {
            // move our seekbar to the new view we've been given
            ViewParent oldContainer = mSeekBar.getParent();
            ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);

            if (oldContainer != newContainer) {
                // remove the seekbar from the old view
                if (oldContainer != null) {
                    ((ViewGroup) oldContainer).removeView(mSeekBar);
                }
                // remove the existing seekbar (there may not be one) and add ours
                newContainer.removeAllViews();
                newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT,
                                               ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        } catch(Exception ex) {
            Log.e(LOGTAG, "Error binding view: " + ex.toString());
        }

        updateView(view);
    }
    
    private void updateStatusText() {
        if (mStatusText != null) {
            if (isMetricValue) {
                String text = String.valueOf(mCurrentValue) + " ";
                if (KeypadMapperApplication.getInstance().getSettings().getMeasurement().equals(KeypadMapperSettings.UNIT_METER)) {
                    text += KeypadMapperApplication.getInstance().getLocalizer().getString("km_per_hour");
                } else {
                    text += KeypadMapperApplication.getInstance().getLocalizer().getString("miles_per_hour");
                }
                mStatusText.setText(text);
            } else {
                mStatusText.setText(String.valueOf(mCurrentValue));
            }
        }
    }
    /**
     * Update a SeekBarPreference view with our current state
     * @param view
     */
    protected void updateView(View view) {
        try {
            LinearLayout layout = (LinearLayout)view;

            updateStatusText();
            mStatusText.setMinimumWidth(30);
            mSeekBar.setProgress(mCurrentValue - mMinValue);
            /*
            // adapt text colors
            if (Build.VERSION.SDK_INT <= 12) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.white));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.white));
             } else {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
            }
            
            /* IF left padding for Seekbar preferences doesn't work, other methods to fix it are:
             * - hardcode left padding for seekbar layout like below (doesn't work for all devices and isn't generic),
             * - spend a week to rewrite the whole PreferenceActivity as a completely custom thing which can fully
             * be controlled.
             * 
            if (view.getResources().getBoolean(R.bool.is_720dp_tablet) && Build.VERSION.SDK_INT >= 14) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                // paddings
                layout.setPadding(125, 0, 10, 0);
                mStatusText.setPadding(0, 0, 45, 0);
            } else if (view.getResources().getBoolean(R.bool.is_tablet) && (Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15)) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                // paddings
                if (view.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    layout.setPadding(65, 0, 10, 0);
                } else {
                    layout.setPadding(10, 0, 10, 0);
                }
                mStatusText.setPadding(0, 0, 45, 0);
            } else if (view.getResources().getBoolean(R.bool.is_tablet) && Build.VERSION.SDK_INT <= 12) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.white));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.white));
                // paddings
                layout.setPadding(15, 0, 45, 0);
                //layout.setPadding(paddingLeft, 0, paddingRight, 0);
                mStatusText.setPadding(0, 0, 10, 0);
            } else if (view.getResources().getBoolean(R.bool.is_tablet) && Build.VERSION.SDK_INT >= 16) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                // paddings
                //layout.setPadding(15, 0, 45, 0);
                layout.setPadding(paddingLeft, 0, paddingRight, 0);
                mStatusText.setPadding(0, 0, 10, 0);
            } else if (Build.VERSION.SDK_INT <= 12) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.white));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.white));
                // paddings
                
                mStatusText.setPadding(0, 0, 10, 7);
                layout.setPadding(15, 0, 5, 0);
            } else if (Build.VERSION.SDK_INT == 14 || Build.VERSION.SDK_INT == 15) {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                // paddings
                layout.setPadding(7, 0, 5, 0);
                mStatusText.setPadding(0, 0, 45, 7);
            } else {
                summaryText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                mStatusText.setTextColor(layout.getResources().getColor(R.color.ENAiKOON_light_gray));
                // paddings
                
                layout.setPadding(paddingLeft, 0, paddingRight, 0);
                mStatusText.setPadding(0, 0, 90, 7);
            }*/
        }
        catch(Exception e) {
            Log.e(LOGTAG, "Error updating seek bar preference", e);
        }
    }
    
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = progress + mMinValue;
        
        if(newValue > mMaxValue)
            newValue = mMaxValue;
        else if(newValue < mMinValue)
            newValue = mMinValue;
        else if(newValue % mInterval != 0)
            newValue = Math.round(((float)newValue)/mInterval)*mInterval;  
        
        // change rejected, revert to the previous value
        if(!callChangeListener(newValue)){
            seekBar.setProgress(mCurrentValue - mMinValue); 
            return; 
        }

        // change accepted, store it
        mCurrentValue = newValue;
        updateStatusText();
        persistInt(newValue);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        notifyChanged();
    }

    @Override 
    protected Object onGetDefaultValue(TypedArray ta, int index){
        return defaultValue;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if(restoreValue) {
            mCurrentValue = getPersistedInt(mCurrentValue);
        } else {
            int temp = 0;
            
            try {
                temp = (Integer)defaultValue;
            } catch(Exception ex) {
                Log.e(LOGTAG, "Invalid default value: " + defaultValue.toString());
            }
            
            persistInt(temp);
            mCurrentValue = temp;
        }
    }
}

