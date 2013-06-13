/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.photo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.keypadmapper3.domain.Mapper;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class CameraHelper {

    public static final int REQUEST_CAMERA_PHOTO = 2;

    // private final static Logger log = Logger.getLogger(CameraHelper.class);

    public static boolean onActivityResult(Activity activity, int requestCode, int resultCode,
            Intent data) {
        Mapper mapper = KeypadMapperApplication.getInstance().getMapper();
        Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();
        if (requestCode == REQUEST_CAMERA_PHOTO) {
            File imageFile = KeypadMapperApplication.getInstance().getLastPhotoFile();
            if (resultCode == Activity.RESULT_OK && imageFile != null && imageFile.exists()) {
                Location location = mapper.getPhotoLocation();
                if (location == null) {
                    Toast.makeText(activity, localizer.getString("no_location"), Toast.LENGTH_SHORT)
                            .show();
                    return false;
                }
                // log.info("Saving photo with location: " + location);
                GpsLocationWriter.update(imageFile, location);
                Toast.makeText(activity, imageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return false;
    }

    public static void startPhotoIntent(Activity activity) {
        Mapper mapper = KeypadMapperApplication.getInstance().getMapper();
        Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();
        Location location = mapper.getCurrentLocation();
        // log.info("Making photo with location: " + location);
        
        if (!KeypadMapperApplication.getInstance().getSettings().isRecording()) {
            Toast.makeText(activity, localizer.getString("error_not_recording"), Toast.LENGTH_LONG).show();
            return;
        }

        mapper.setPhotoLocation(location);

        if (location == null) {
            Toast.makeText(activity, localizer.getString("no_location"), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        File kpmFolder = KeypadMapperApplication.getInstance().getKeypadMapperDirectory();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HH_mm_ss").format(new Date());
        File lastPhotoFile =
                new File(kpmFolder.getAbsoluteFile() + "/" + mapper.getBasename() + "_" + timeStamp
                        + ".jpg");
        KeypadMapperApplication.getInstance().setLastPhotoFile(lastPhotoFile);
        Uri mImageUri = Uri.fromFile(lastPhotoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        // start camera intent
        activity.startActivityForResult(intent, REQUEST_CAMERA_PHOTO);
    }
}
