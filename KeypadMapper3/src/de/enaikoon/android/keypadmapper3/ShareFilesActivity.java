/**************************************************************************
 * Copyright
 *
 * $Id: ShareFilesActivity.java 149 2013-02-01 08:56:06Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/ShareFilesActivity.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.settings.KeypadMapperSettings;
import de.enaikoon.android.keypadmapper3.writers.GpxWriter;
import de.enaikoon.android.keypadmapper3.writers.OsmWriter;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class ShareFilesActivity extends ListActivity {

    class ApplicationAdapter extends ArrayAdapter<ResolveInfo> {
        private PackageManager pm = null;

        ApplicationAdapter(PackageManager pm, List<ResolveInfo> apps) {
            super(ShareFilesActivity.this, R.layout.share_files_row, apps);
            this.pm = pm;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = newView(parent);
            }

            bindView(position, convertView);

            return (convertView);
        }

        private void bindView(int position, View row) {
            TextView label = (TextView) row.findViewById(R.id.label);

            label.setText(getItem(position).loadLabel(pm));

            ImageView icon = (ImageView) row.findViewById(R.id.icon);

            icon.setImageDrawable(getItem(position).loadIcon(pm));
        }

        private View newView(ViewGroup parent) {
            return (getLayoutInflater().inflate(R.layout.share_files_row, parent, false));
        }
    }

    public static boolean createZip(File zipFileName, File[] selected) {
        boolean success = false;
        try {
            byte[] buffer = new byte[1024];
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
            out.setLevel(Deflater.DEFAULT_COMPRESSION);
            for (int i = 0; i < selected.length; i++) {
                // skip empty files
                if (selected[i].getAbsolutePath().endsWith(".gpx") && selected[i].length() <= GpxWriter.getEmptyFileSize()) {
                    // skip it
                    continue;
                } else if (selected[i].getAbsolutePath().endsWith(".osm") && selected[i].length() <= OsmWriter.getEmptyFileSize()) {
                    // skip it
                    continue;
                }
                
                FileInputStream in = new FileInputStream(selected[i]);
                out.putNextEntry(new ZipEntry(selected[i].getName()));
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
            success = true;
        } catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return success;
    }

    private ApplicationAdapter adapter = null;

    private List<ResolveInfo> sendMultipleActions;

    private List<ResolveInfo> sendActions;

    private List<ResolveInfo> mailClientActions;

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (KeypadMapperApplication.getInstance().getSettings().isLayoutOptimizationEnabled()) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_files_activity);

        setTitle(localizer.getString("prefsShare"));

        PackageManager pm = getPackageManager();
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE, null);
        shareIntent.setType("application/zip");

        sendMultipleActions = pm.queryIntentActivities(shareIntent, 0);

        shareIntent = new Intent(android.content.Intent.ACTION_SEND, null);
        shareIntent.setType("application/zip");

        sendActions = pm.queryIntentActivities(shareIntent, 0);

        String recepientEmail = "";
        shareIntent = new Intent(Intent.ACTION_SENDTO);
        shareIntent.setData(Uri.parse("mailto:" + recepientEmail));
        mailClientActions = pm.queryIntentActivities(shareIntent, 0);

        List<ResolveInfo> launchables = new ArrayList<ResolveInfo>();

        launchables.addAll(sendMultipleActions);

        for (ResolveInfo info : sendActions) {
            if (!isInSendMultipleList(info)) {
                launchables.add(info);
            }
        }

        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(pm));

        adapter = new ApplicationAdapter(pm, launchables);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ResolveInfo launchable = adapter.getItem(position);
        ActivityInfo activity = launchable.activityInfo;
        ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name);

        if (isInSendMultipleList(launchable)) {
            if (isMailClient(launchable)) {
                shareFilesViaEmail(name);
            } else {
                shareFiles(name, null);
                finish();
            }
        } else {
            shareFilesInZip(name);
            finish();
        }
    }

    @Override
    protected void onPause() {
        KeypadMapperSettings settings = KeypadMapperApplication.getInstance().getSettings();
        settings.setLastTimeLaunch(System.currentTimeMillis());
        super.onPause();
    }

    private boolean isInSendMultipleList(ResolveInfo inInfo) {
        for (ResolveInfo info : sendMultipleActions) {
            if (info.activityInfo.name.equals(inInfo.activityInfo.name)
                    && info.activityInfo.applicationInfo.packageName
                            .equals(inInfo.activityInfo.applicationInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMailClient(ResolveInfo inInfo) {
        for (ResolveInfo info : mailClientActions) {
            if (info.activityInfo.name.equals(inInfo.activityInfo.name)
                    && info.activityInfo.applicationInfo.packageName
                            .equals(inInfo.activityInfo.applicationInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private void shareFiles(ComponentName componentName, String email) {
        // need to "send multiple" to get more than one attachment
        final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
        if (email != null) {
            shareIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { email });
        }
        shareIntent.setType("application/zip");
        // has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        // convert from paths to Android friendly Parcelable Uri's
        File kpmFolder = KeypadMapperApplication.getInstance().getKeypadMapperDirectory();
        File[] filePaths = kpmFolder.listFiles(KeypadMapperApplication.getInstance().getFileFilter());
        for (File file : filePaths) {
            if (file.getAbsolutePath().endsWith(".gpx") && file.length() <= GpxWriter.getEmptyFileSize()) {
                // skip it
                continue;
            } else if (file.getAbsolutePath().endsWith(".osm") && file.length() <= OsmWriter.getEmptyFileSize()) {
                // skip it
                continue;
            }
            
            Uri u = Uri.fromFile(file);
            uris.add(u);
        }
        KeypadMapperApplication.getInstance().getMapper().reset();
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.setComponent(componentName);
        startActivity(shareIntent);
    }

    private void shareFilesInZip(ComponentName componentName) {
        // need to "send multiple" to get more than one attachment
        final Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        // has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<Uri>();
        // convert from paths to Android friendly Parcelable Uri's
        File kpmFolder = KeypadMapperApplication.getInstance().getKeypadMapperDirectory();
        File zipFolder = new File(kpmFolder.getAbsolutePath() + "/zip");
        if (!zipFolder.exists()) {
            zipFolder.mkdir();
        }
        File[] filePaths = kpmFolder.listFiles(KeypadMapperApplication.getInstance().getFileFilter());
        
        File zipFile =
                new File(zipFolder.getAbsolutePath() + "/" + System.currentTimeMillis() + ".zip");

        boolean zipped = createZip(zipFile, filePaths);
        KeypadMapperApplication.getInstance().getMapper().reset();
        if (zipped) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));

            shareIntent.setComponent(componentName);
            startActivity(shareIntent);
        } else {
            Toast.makeText(this, localizer.getString("zip_fail"), Toast.LENGTH_LONG).show();
        }
    }

    private void shareFilesViaEmail(final ComponentName componentName) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(localizer.getString("prefsShareEmailTitle"));
        // alert.setMessage("Message");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setHint(localizer.getString("prefsShareEmailHint"));
        input.setText(KeypadMapperApplication.getInstance().getSettings().getLastSharedEmail());
        alert.setView(input);

        alert.setPositiveButton(localizer.getString("prefsShareEmailShare"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String email = input.getText().toString();
                        KeypadMapperApplication.getInstance().getSettings()
                                .setLastSharedEmail(email);
                        shareFiles(componentName, email);
                        finish();
                    }
                });

        alert.setNegativeButton(localizer.getString("prefsShareEmailCancel"),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                });

        alert.show();
    }
}
