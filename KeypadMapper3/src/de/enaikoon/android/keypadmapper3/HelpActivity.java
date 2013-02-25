/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3;

import java.net.URLEncoder;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * Help screen
 */
public class HelpActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

        WebView webView = (WebView) findViewById(R.id.helpWebView);
        String text1 = localizer.getString("help_text_1");
        String text2 = localizer.getString("help_text_2");
        String text3 = localizer.getString("help_text_3");
        getSupportActionBar().setTitle(localizer.getString("help_title"));

        webView.getSettings().setRenderPriority(RenderPriority.HIGH);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        String top =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                        + "<html><head>"
                        + "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />"
                        + "<head>";
        String data =
                top + "<body  bgcolor=\"#000000\"><font color=\"#ffffff\">" + text1 + text2 + text3
                        + "</font></body></html>";
        webView.loadData(URLEncoder.encode(data).replaceAll("\\+", " "),
                "text/html; charset=utf-8", "UTF-8");
    }

    @Override
    protected void onPause() {
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());
        super.onPause();
    }
}
