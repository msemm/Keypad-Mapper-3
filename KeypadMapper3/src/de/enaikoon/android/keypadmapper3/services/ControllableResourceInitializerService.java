/**************************************************************************
 * Copyright
 *
 * $Id: ControllableResourceInitializerService.java 126 2013-01-22 09:22:34Z jvilya $
 * $HeadURL: https://brainymobility.unfuddle.com/svn/brainymobility_enaikoon/trunk/keypadmapper3/src/de/enaikoon/android/keypadmapper3/services/ControllableResourceInitializerService.java $
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.services;

import android.content.Context;
import android.content.Intent;
import de.enaikoon.android.keypadmapper3.utils.ConnectivityUtils;
import de.enaikoon.android.library.resources.locale.ResourcesInitializerService;

/**
 * 
 */
public class ControllableResourceInitializerService extends ResourcesInitializerService {

    public static void startResourceLoading(Context context, String languagesCodeResourceName,
            String languagesNameResourceName, String languagesUrlResourceName) {
        if (languagesCodeResourceName == null || languagesNameResourceName == null
                || languagesUrlResourceName == null) {
            throw new IllegalArgumentException("Input arguments could not be null");
        }
        Intent localeInitializerIntent =
                new Intent(context, ControllableResourceInitializerService.class);
        localeInitializerIntent.putExtra("lang_codes", languagesCodeResourceName);
        localeInitializerIntent.putExtra("lang_names", languagesNameResourceName);
        localeInitializerIntent.putExtra("lang_urls", languagesUrlResourceName);
        context.startService(localeInitializerIntent);
    }

    @Override
    protected boolean isDownloadAllowed() {
        return ConnectivityUtils.isDownloadAllowed(this);
    }

}
