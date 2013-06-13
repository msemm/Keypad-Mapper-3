package de.enaikoon.android.keypadmapper3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class ExceptionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        
        String setting = KeypadMapperApplication.getInstance().getSettings().getErrorReporting();
        final String [] constantStrings = getResources().getStringArray(R.array.options_bugreport_keys);
        int checkedItem = -1;
        for (checkedItem = 0; checkedItem < constantStrings.length; checkedItem++) {
            if (setting.equals(constantStrings[checkedItem])) {
                break;
            }
        }
        // ALWAYS
        if (checkedItem == 1) {
            Toast.makeText(this, getString(R.string.bugreport_dialogheader), Toast.LENGTH_LONG)
                    .show();
            CustomExceptionHandler.sendEmail(extras.getString("bugReport"), this);
            finish();
        }   // ASK
            else if (checkedItem == 2) {
            AlertDialog ad =
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.bugreport_dialogheader))
                            .setMessage(getString(R.string.options_bugreport_question))
                            .setPositiveButton(getString(R.string.options_bugreport_send),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            CustomExceptionHandler.sendEmail(
                                                    extras.getString("bugReport"),
                                                    ExceptionActivity.this);
                                            finish();
                                        }
                                    })
                            .setNegativeButton(getString(R.string.options_bugreport_dontsend),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                            return;
                                        }
                                    }).create();

            ad.setCancelable(false);
            ad.show();
        } else {
            // NEVER
            AlertDialog ad =
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.bugreport_dialogheader))
                            .setMessage(getString(R.string.options_bugreport_neversendmessage))
                            .setPositiveButton(getString(R.string.options_bugreport_ok),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                            return;
                                        }
                                    }).create();

            ad.setCancelable(false);
            ad.show();
        }
    }
}
