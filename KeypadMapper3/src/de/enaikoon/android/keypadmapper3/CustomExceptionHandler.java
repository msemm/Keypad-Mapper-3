package de.enaikoon.android.keypadmapper3;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

public class CustomExceptionHandler implements UncaughtExceptionHandler {

    public static void sendEmail(String stacktrace, Context c) {
        try {
            final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
            emailIntent.setType("text/html");
            emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                    c.getString(R.string.bugreport_subject));
            emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, createReport(stacktrace, c));
            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            emailIntent.putExtra(Intent.EXTRA_EMAIL,
                    c.getResources().getStringArray(R.array.bugreport_persons));
            // production version doesn't create log files so there is no need
            // to attach them

            // if (!Main.PRODUCTION_VERSION)
            // {
            // Uri u = Uri.parse("file://" + compressLogFiles("log.zip"));
            // if (!u.getPath().equals(""))
            // {
            // emailIntent.putExtra(Intent.EXTRA_STREAM, u);
            // }
            // }

            c.startActivity(emailIntent);
        } catch (Exception e) {
            // AppMain.writeExceptionToLog(e);
        }
    }

    /**
     * @return string with all SharedPreferences parameters
     */
    protected static String logConfigParams(Boolean htmlFormat) {
        String params = "***PARAMS***" + (htmlFormat ? "<br>" : "\n");
        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        Map<String, ?> map = prefs.getAll();

        if (map.size() > 0) {
            Iterator<String> keyI = map.keySet().iterator();

            do {
                String keyName = keyI.next();
                try {
                    params +=
                            keyName + " => " + map.get(keyName).toString()
                                    + (htmlFormat ? "<br>" : "\n");
                } catch (Exception ex) {
                    // FileLog.writeExceptionToLog(ex);
                }
            } while (keyI.hasNext());
        }

        return params;
    }

    protected static String logConfigParams2(Boolean paramBoolean) {
        StringBuilder paramsInfoBuilder = new StringBuilder("***PARAMS***");
        if (paramBoolean.booleanValue())
            paramsInfoBuilder.append("<br>");

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        Map<String, ?> prefMap = prefs.getAll();

        Set<String> keys = prefMap.keySet();

        for (String key : keys) {
            paramsInfoBuilder.append(key);
            paramsInfoBuilder.append(" => ");
            paramsInfoBuilder.append(String.valueOf(prefMap.get(key)));
            paramsInfoBuilder.append("<br>");
            paramsInfoBuilder.append("\n");
        }
        return paramsInfoBuilder.toString();
    }

    private static Spanned createReport(String stacktrace, Context c) {
        String report = "";
        report += "<b>" + c.getString(R.string.bugreport_subject) + "</b><br>";
        report += "Report created: " + new Date().toGMTString() + "<br><br>";
        report +=
                "<b>Application: " + c.getString(R.string.app_name) + " v" + getAppVersion(c)
                        + "</b><br><br>";
        report += "Device: " + Build.DEVICE + "<br>";
        report += "Manufacturer: " + Build.MANUFACTURER + "<br>";
        report += "Model: " + Build.MODEL + "<br>";
        report += "Product name: " + Build.PRODUCT + "<br>";
        report += "Android version: " + Build.VERSION.RELEASE + "<br><br>";
        report += logConfigParams(true) + "<br><br>";
        report += "Stack trace: <br>";
        report += stacktrace;

        return Html.fromHtml(report);
    }

    private static String getAppVersion(Context c) {
        String app_ver = "";

        try {
            app_ver = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // AppMain.writeExceptionToLog(e);
        }

        return app_ver;
    }

    private UncaughtExceptionHandler defaultUEH;

    public static Context context;

    public CustomExceptionHandler(Context context) {
        this.context = context;
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    // public static File compressLogFiles(String zipFileName)
    // {
    // try
    // {
    // String zipPath = AppMain.getApplicationPath() + zipFileName;
    //
    // File f = new File(zipPath);
    // f.delete();
    //
    // BufferedInputStream origin = null;
    // FileOutputStream dest = new FileOutputStream(zipPath);
    // ZipOutputStream out = new ZipOutputStream(new
    // BufferedOutputStream(dest));
    // byte data[] = new byte[2048];
    //
    // //iterate through the log files (e.g. routes1.log, routes2.log)
    // for (int i=1;i<3;i++)
    // {
    // String fileName = AppMain.getApplicationPath() +
    // AppMain.logFileNamePrefix + i + ".log";
    //
    // if ((new File(fileName)).exists())
    // {
    // FileInputStream fi = new FileInputStream(fileName);
    // origin = new BufferedInputStream(fi, 2048);
    // ZipEntry entry = new
    // ZipEntry(fileName.substring(fileName.lastIndexOf("/") + 1));
    // out.putNextEntry(entry);
    // int count;
    // while ((count = origin.read(data, 0, 2048)) != -1)
    // {
    // out.write(data, 0, count);
    // }
    // origin.close();
    // }
    // }
    //
    // out.close();
    //
    // return new File(zipPath);
    // } catch (Exception e)
    // {
    // e.printStackTrace();
    // return null;
    // }
    // }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        final String stacktrace = result.toString();
        printWriter.close();

        try {
            Log.e("routes CustomExceptionHandler", stacktrace);

            // if (AppMain.prefs == null) {
            // AppMain.prefs =
            // PreferenceManager.getDefaultSharedPreferences(context);
            // }

            // AppMain.writeUnhandledExceptionToLog(e);
        } catch (Exception ex) {
            Log.e("UNHANDLED ERRORS ERROR", "UNHANDLED ERRORS ERROR", ex);
        }

        Intent i1 = new Intent(context, ExceptionActivity.class);
        i1.putExtra("bugReport", stacktrace);
        i1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(i1);

        System.exit(0);
    }

}
