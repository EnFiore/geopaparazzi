// MainActivityFragment.java
// Contains the Flag Quiz logic
package eu.hydrologis.geopaparazzi.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.gps.GpsServiceUtilities;
import eu.geopaparazzi.library.sensors.OrientationSensor;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.activities.AboutActivity;
import eu.hydrologis.geopaparazzi.activities.SettingsActivity;
import eu.hydrologis.geopaparazzi.providers.ProviderTestActivity;

public class GeopaparazziActivityFragment extends Fragment implements View.OnLongClickListener, View.OnClickListener {

    private ImageButton mNotesButton;
    private ImageButton mMetadataButton;
    private ImageButton mMapviewButton;
    private ImageButton mGpslogButton;
    private ImageButton mImportButton;

    private MenuItem mGpsMenuItem;

    private OrientationSensor mOrientationSensor;
    private BroadcastReceiver mGpsServiceBroadcastReceiver;

    private static boolean sCheckedGps = false;
    private GpsServiceStatus mLastGpsServiceStatus;
    private int[] mLastGpsStatusExtras;
    private GpsLoggingStatus mLastGpsLoggingStatus;
    private GestureDetectorCompat mGestureDetector;
    private double[] lastGpsPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.fragment_geopaparazzi, container, false);

        // this fragment adds to the menu
        setHasOptionsMenu(true);

        // start gps service
        GpsServiceUtilities.startGpsService(getActivity());

        return v; // return the fragment's view for display
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNotesButton = (ImageButton) view.findViewById(R.id.dashboardButtonNotes);
        mNotesButton.setOnLongClickListener(this);

        mMetadataButton = (ImageButton) view.findViewById(R.id.dashboardButtonMetadata);
        mMetadataButton.setOnClickListener(this);

        mMapviewButton = (ImageButton) view.findViewById(R.id.dashboardButtonMapview);
        mMapviewButton.setOnClickListener(this);

        mGpslogButton = (ImageButton) view.findViewById(R.id.dashboardButtonGpslog);
        mGpslogButton.setOnClickListener(this);

        mImportButton = (ImageButton) view.findViewById(R.id.dashboardButtonImport);
        mImportButton.setOnClickListener(this);


        mGestureDetector = new GestureDetectorCompat(getActivity(), new PanicGestureListener());
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        };
        view.setOnTouchListener(touchListener);
        mNotesButton.setOnTouchListener(touchListener);
        mMetadataButton.setOnTouchListener(touchListener);
        mMapviewButton.setOnTouchListener(touchListener);
        mGpslogButton.setOnTouchListener(touchListener);
        mImportButton.setOnTouchListener(touchListener);

    }

    @Override
    public void onResume() {
        super.onResume();

        GpsServiceUtilities.triggerBroadcast(getActivity());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (mOrientationSensor == null) {
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            mOrientationSensor = new OrientationSensor(sensorManager, null);
        }
        mOrientationSensor.register(getActivity(), SensorManager.SENSOR_DELAY_NORMAL);

        if (mGpsServiceBroadcastReceiver == null) {
            mGpsServiceBroadcastReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    onGpsServiceUpdate(intent);
                    checkFirstTimeGps(context);
                }
            };
        }
        GpsServiceUtilities.registerForBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);

    }

    // remove SourceUrlsFragmentListener when Fragment detached
    @Override
    public void onDetach() {
        super.onDetach();

        mOrientationSensor.unregister();
        GpsServiceUtilities.unregisterFromBroadcasts(getActivity(), mGpsServiceBroadcastReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_main, menu);

        mGpsMenuItem = menu.getItem(3);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tilesource: {

            }
            case R.id.action_new: {

            }
            case R.id.action_load: {

            }
            case R.id.action_gps: {
                GpsInfoDialogFragment gpsInfoDialogFragment = new GpsInfoDialogFragment();
                gpsInfoDialogFragment.show(getFragmentManager(), "gpsinfo dialog");
                return true;
            }
            case R.id.action_gpsstatus: {
                // open gps status app
                AppsUtilities.checkAndOpenGpsStatus(getActivity());
                return true;
            }
            case R.id.action_settings: {
                Intent preferencesIntent = new Intent(this.getActivity(), SettingsActivity.class);
                startActivity(preferencesIntent);
                return true;
            }
            case R.id.action_about: {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_exit: {
                getActivity().finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onLongClick(View v) {
        String tooltip = "blah";
        if (v == mNotesButton) {
            tooltip = "Available providers:";
            for (PackageInfo pack : getActivity().getPackageManager().getInstalledPackages(PackageManager.GET_PROVIDERS)) {
                ProviderInfo[] providers = pack.providers;
                if (providers != null) {
                    for (ProviderInfo provider : providers) {
                        Log.d("Example", "provider: " + provider.authority);
                        tooltip = tooltip + "\n" + provider.authority;
                    }
                }
            }


        }

        Snackbar.make(v, tooltip, Snackbar.LENGTH_LONG).show();

        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mMetadataButton) {
            LineWidthDialogFragment widthDialog =
                    new LineWidthDialogFragment();
            widthDialog.show(getFragmentManager(), "line width dialog");
        } else if (v == mMapviewButton) {
            ColorDialogFragment colorDialog = new ColorDialogFragment();
            colorDialog.show(getFragmentManager(), "color dialog");
        } else if (v == mGpslogButton) {
            mGpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
        } else if (v == mImportButton) {
            Intent providerIntent = new Intent(getActivity(), ProviderTestActivity.class);
            startActivity(providerIntent);
        }

    }


    private void onGpsServiceUpdate(Intent intent) {
        mLastGpsServiceStatus = GpsServiceUtilities.getGpsServiceStatus(intent);
        mLastGpsLoggingStatus = GpsServiceUtilities.getGpsLoggingStatus(intent);
        mLastGpsStatusExtras = GpsServiceUtilities.getGpsStatusExtras(intent);
        lastGpsPosition = GpsServiceUtilities.getPosition(intent);
//        lastGpsPositionExtras = GpsServiceUtilities.getPositionExtras(intent);
//        lastPositiontime = GpsServiceUtilities.getPositionTime(intent);


        boolean doLog = GPLog.LOG_HEAVY;
        if (doLog && mLastGpsStatusExtras != null) {
            int satCount = mLastGpsStatusExtras[1];
            int satForFixCount = mLastGpsStatusExtras[2];
            GPLog.addLogEntry(this, "satellites: " + satCount + " of which for fix: " + satForFixCount);
        }

        if (mGpsMenuItem != null)
            if (mLastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be on");
                if (mLastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON) {
                    if (doLog)
                        GPLog.addLogEntry(this, "GPS seems to be also logging");
                    mGpsMenuItem.setIcon(R.drawable.actionbar_gps_logging);
                } else {
                    if (mLastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS has fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_fix_nologging);
                    } else {
                        if (doLog) {
                            GPLog.addLogEntry(this, "GPS doesn't have a fix");
                        }
                        mGpsMenuItem.setIcon(R.drawable.actionbar_gps_nofix);
                    }
                }
            } else {
                if (doLog)
                    GPLog.addLogEntry(this, "GPS seems to be off");
                mGpsMenuItem.setIcon(R.drawable.actionbar_gps_off);
            }
    }

    private void checkFirstTimeGps(Context context) {
        if (!sCheckedGps) {
            sCheckedGps = true;
            if (mLastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                String prompt = getResources().getString(R.string.prompt_gpsenable);
                Utilities.yesNoMessageDialog(context, prompt, new Runnable() {
                    public void run() {
                        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(gpsOptionsIntent);
                    }
                }, null);
            }
        }
    }


    private class PanicGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            if (lastGpsPosition == null) {
                return true;
            }

            PanicDialogFragment panicDialogFragment = new PanicDialogFragment();
            Bundle bundle = new Bundle();

            double lon = lastGpsPosition[0];
            double lat = lastGpsPosition[1];
            bundle.putDouble(LibraryConstants.LATITUDE, lat);
            bundle.putDouble(LibraryConstants.LONGITUDE, lon);

            float deltaY = event1.getY() - event2.getY();
            if (deltaY < 0) {
                // open panic button view on down fling
                bundle.putBoolean(PanicDialogFragment.KEY_ISPANIC, true);
            } else {
                // status update button view on down fling
                bundle.putBoolean(PanicDialogFragment.KEY_ISPANIC, false);
            }
            panicDialogFragment.setArguments(bundle);
            panicDialogFragment.show(getFragmentManager(), "panic dialog");
            return true;
        }
    }


}
