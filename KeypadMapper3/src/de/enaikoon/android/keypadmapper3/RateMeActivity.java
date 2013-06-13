/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class RateMeActivity extends Activity implements OnClickListener {

    public static void startRateMe(Context context) {
 
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        if (System.currentTimeMillis() - settings.getLastTimeLaunch() > 5 * 1000L) {
            int count = settings.getLaunchCount() + 1;
            settings.setLaunchCount(count);
            if (count == 10 || count == 16 || count == 27) {
                Intent intent = new Intent(context, RateMeActivity.class);
                context.startActivity(intent);
            }
        }
    }

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();

    private Button yesBtn;

    private Button laterBtn;

    private Button dontAskBtn;

    private ImageView icon;

    private ImageView stars;

    private TextView thankYou;

    private TextView keypad;

    private TextView text;

    private View root;

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View view) {
        if (view == yesBtn) {
            settings.setLaunchCount(100);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=de.enaikoon.android.keypadmapper3"));
            startActivity(intent);
            finish();
        } else if (view == laterBtn) {
            int count = settings.getLaunchCount() + 1;
            settings.setLaunchCount(count);
            finish();
        } else if (view == dontAskBtn) {
            settings.setLaunchCount(100);
            finish();
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_me);

        yesBtn = (Button) findViewById(R.id.rateme_yes);
        yesBtn.setOnClickListener(this);
        laterBtn = (Button) findViewById(R.id.rateme_later);
        laterBtn.setOnClickListener(this);
        dontAskBtn = (Button) findViewById(R.id.rateme_dont_ask);
        dontAskBtn.setOnClickListener(this);

        icon = (ImageView) findViewById(R.id.rateme_big_icon);

        stars = (ImageView) findViewById(R.id.rateme_stars);

        thankYou = (TextView) findViewById(R.id.rateme_thankyou);

        keypad = (TextView) findViewById(R.id.rateme_keypad);

        text = (TextView) findViewById(R.id.rateme_text);

        root = findViewById(R.id.root);

        init();
    }

    @Override
    protected void onPause() {
        settings.setLastTimeLaunch(System.currentTimeMillis());
        super.onPause();
    }

    private void init() {
        yesBtn.setText(localizer.getString("rateme_yes"));
        laterBtn.setText(localizer.getString("rateme_later"));
        dontAskBtn.setText(localizer.getString("rateme_dont_ask"));

        yesBtn.setBackgroundDrawable(localizer.getDrawable("yes_button"));
        laterBtn.setBackgroundDrawable(localizer.getDrawable("no_button"));
        dontAskBtn.setBackgroundDrawable(localizer.getDrawable("no_button"));

        root.setBackgroundDrawable(localizer.getDrawable("popup"));

        icon.setImageDrawable(localizer.getDrawable("big_icon"));
        stars.setImageDrawable(localizer.getDrawable("stars"));

        thankYou.setText(localizer.getString("rateme_thankyou"));
        keypad.setText(localizer.getString("rateme_keypad"));
        text.setText(localizer.getString("rateme_text"));

    }

}
