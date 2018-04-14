package ru.spbstu.videomood.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ru.spbstu.videomood.MuseManager;
import ru.spbstu.videomood.R;

public class SelectMuseActivity extends AppCompatActivity {
    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "VideoMood:SelectMuse";

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    //private MuseManagerAndroid manager;

    /**
     * In the UI, the list of Muses you can connectToServer to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    private static final int REQUEST_ACCESS_LOCATION = 255;
    private static final int REQUEST_ENABLE_BT = 248;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        MuseManager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<SelectMuseActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to receive notifications of what Muse headbands
        // we can connectToServer to.
        MuseManager.getManager().setMuseListener(new MuseL(weakActivity));

        initUI();

        ensureBluetoothEnabled();
    }

    private boolean isBtEnableRequesting = false;
    private boolean isPermissionRequesting = false;

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

        outPersistentState.putInt("isBtEnableRequesting", isBtEnableRequesting ? 1 : 0);
        outPersistentState.putInt("isPermissionRequesting", isPermissionRequesting ? 1 : 0);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        isBtEnableRequesting  = savedInstanceState.getInt("isBtEnableRequesting", 0) != 0;
        isPermissionRequesting = savedInstanceState.getInt("isPermissionRequesting", 0) != 0;
    }

    private void ensureBluetoothEnabled() {
        if (isBtEnableRequesting)
            return;

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            isBtEnableRequesting = true;
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (!isPermissionRequesting)
                ensurePermissions();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MuseManager.startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        MuseManager.stopListening();
    }

    public void goToUserData(View v) {
        MuseManager.stopListening();

        int selectedMuseIndex = musesSpinner.getSelectedItemPosition();
        ArrayList<Muse> availableMuses = MuseManager.getMuses();
        if (availableMuses.size() == 0) {
            Log.i(TAG, "There is nothing to connect to");
            return;
        }
        Muse muse = availableMuses.get(selectedMuseIndex);
        MuseManager.setMuse(muse);

        Log.i(TAG, "selected muse is " + muse.getMacAddress());

        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }

    public void refreshMusesList(View v) {
        MuseManager.stopListening();
        MuseManager.startListening();
    }

    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     * <p>
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {
        if (isPermissionRequesting)
            return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isPermissionRequesting = true;
            ActivityCompat.requestPermissions(SelectMuseActivity.this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_LOCATION);
        } else {
            onPermissionGranted();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    private void museListChanged() {
        spinnerAdapter.clear();

        final List<Muse> list = MuseManager.getMuses();
        boolean isAnyDevices = list.size() > 0;
        musesSpinner.setEnabled(isAnyDevices);
        if (isAnyDevices) {
            for (Muse m : list) {
                spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
            }
            tryAutoSelect();
        }
        else {
            spinnerAdapter.add(getResources().getString(R.string.noDevicesFound));
            Log.i(TAG, "no devices found");
        }
    }

    private void tryAutoSelect() {
        int musesCount = musesSpinner.getAdapter().getCount();
        if (musesCount == 1) {
            musesSpinner.setSelection(0);
            goToUserData(findViewById(R.id.next));
        }
    }

    private Spinner musesSpinner;

    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    private void initUI() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        musesSpinner = findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<SelectMuseActivity> activityRef;

        MuseL(final WeakReference<SelectMuseActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                ensurePermissions();
            } else {
                Toast.makeText(this, R.string.bluetooth_disabled_message, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void onPermissionGranted() {
        isPermissionRequesting = false;
        MuseManager.startListening();
        MuseManager.setPermissionGranted();
    }
}
