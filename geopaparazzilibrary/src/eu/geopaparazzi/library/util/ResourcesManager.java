/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.library.util;

import static eu.geopaparazzi.library.util.LibraryConstants.PREFS_KEY_BASEFOLDER;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.preference.PreferenceManager;
import eu.geopaparazzi.library.R;

/**
 * Singleton that takes care of resources management.
 * 
 * <p>It creates a folder structure with possible database and log file names.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ResourcesManager implements Serializable {
    private static final long serialVersionUID = 1L;

    private Context context;

    private File applicationDir;

    private File debugLogFile;

    private File databaseFile;

    private static ResourcesManager resourcesManager;

    /**
     * The getter for the {@link ResourcesManager} singleton.
     * 
     * <p>This is a singletone but might require to be recreated
     * in every moment of the application. This is due to the fact
     * that when the application looses focus (for example because of
     * an incoming call, and therefore at a random moment, if the memory 
     * is too low, the parent activity could have been killed by 
     * the system in background. In which case we need to recreate it.) 
     * 
     * @param context the context to refer to.
     * @return the {@link ResourcesManager} instance.
     */
    public synchronized static ResourcesManager getInstance( Context context ) {
        if (resourcesManager == null) {
            try {
                resourcesManager = new ResourcesManager(context);
            } catch (IOException e) {
                return null;
            }
        }
        return resourcesManager;
    }

    public static void resetManager() {
        resourcesManager = null;
    }

    public Context getContext() {
        return context;
    }

    private ResourcesManager( Context context ) throws IOException {
        this.context = context;
        ApplicationInfo appInfo = context.getApplicationInfo();
        String applicationLabel = context.getPackageManager().getApplicationLabel(appInfo).toString();
        applicationLabel = applicationLabel.toLowerCase();

        String databaseName = applicationLabel + ".db"; //$NON-NLS-1$
        /*
         * take care to create all the folders needed
         * 
         * The default structure is:
         * 
         * applicationname 
         *    | 
         *    |--- applicationname.db 
         *    `--- debug.log 
         */
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String baseFolder = preferences.getString(PREFS_KEY_BASEFOLDER, ""); //$NON-NLS-1$
        applicationDir = new File(baseFolder);
        if (baseFolder == null || baseFolder.length() == 0 || !applicationDir.getParentFile().exists()
                || !applicationDir.getParentFile().canWrite()) {
            // the folder doesn't exist for some reason, fallback on default
            String state = Environment.getExternalStorageState();
            boolean mExternalStorageAvailable;
            boolean mExternalStorageWriteable;
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mExternalStorageAvailable = mExternalStorageWriteable = true;
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                mExternalStorageAvailable = true;
                mExternalStorageWriteable = false;
            } else {
                mExternalStorageAvailable = mExternalStorageWriteable = false;
            }
            if (mExternalStorageAvailable && mExternalStorageWriteable) {
                File sdcardDir = Environment.getExternalStorageDirectory();// new
                applicationDir = new File(sdcardDir, applicationLabel);
            } else {
                throw new IOException();
            }
        }

        String applicationDirPath = applicationDir.getAbsolutePath();
        if (!applicationDir.exists())
            if (!applicationDir.mkdir())
                Utilities.messageDialog(context,
                        MessageFormat.format(context.getResources().getString(R.string.cantcreate_sdcard), applicationDirPath),
                        null);
        databaseFile = new File(applicationDirPath, databaseName);
        debugLogFile = new File(applicationDirPath, "debug.log"); //$NON-NLS-1$
    }

    /**
     * Get the file to the main application folder.
     * 
     * @return the {@link File} to the app folder.
     */
    public File getApplicationDir() {
        return applicationDir;
    }

    /**
     * Get the file to a default database location for the app.
     * 
     * <p>This path is generated with default values and can be
     * exploited. It doesn't assure that in the location there really is a db.  
     * 
     * @return the {@link File} to the database.
     */
    public File getDatabaseFile() {
        return databaseFile;
    }

    /**
     * Get the path to the log file.
     * 
     * @return the path to the log file. 
     */
    public File getDebugLogFile() {
        return debugLogFile;
    }

}
