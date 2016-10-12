package ru.spbstu.videomood;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.view.Window;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class VideoActivity extends Activity {

    private static final String TAG = "VideoMood:VideoActivity";
    private MuseMoodSolver moodSolver;

    private final double[] eegBuffer = new double[6];
    private boolean eegStale = false;

    private void updateEeg() {
        TextView alpha = (TextView) findViewById(R.id.alpha);
        TextView beta = (TextView) findViewById(R.id.beta);
        TextView gamma = (TextView) findViewById(R.id.gamma);
        TextView delta = (TextView) findViewById(R.id.delta);
        alpha.setText(String.format("%6.2f", eegBuffer[0]));
        beta.setText(String.format("%6.2f", eegBuffer[1]));
        gamma.setText(String.format("%6.2f", eegBuffer[2]));
        delta.setText(String.format("%6.2f", eegBuffer[3]));
    }

    private void updateMood() {
        TextView moodTextView = (TextView) findViewById(R.id.mood);
        moodTextView.setText(moodSolver.getUser().getCurrentMood().toString());
    }

    public void processMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert (eegBuffer.length >= n);
                getEegChannelValues(eegBuffer, p);
                eegStale = true;
                break;
            case ACCELEROMETER:
            case ALPHA_RELATIVE:
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     * <p>
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */
    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * The runnable that is used to update the UI at 60Hz.
     * <p>
     * We update the UI from this Runnable instead of in packet handlers
     * because packets come in at high frequency -- 220Hz or more for raw EEG
     * -- and it only makes sense to update the UI at about 60fps. The update
     * functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Runnable tickUi = new Runnable() {
        @Override
        public void run() {
            if (eegStale) {
                updateEeg();
                moodSolver.solve(eegBuffer);
                updateMood();
            }
            handler.postDelayed(tickUi, 1000 / 60);
        }
    };

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    private ContentProvider contentProvider;

    private int selectedMuseIndex;
    private int ageRangeIndex;
    private int moodIndex;

    private void fillIndexes(Intent intent) {
        //todo: check for null, process it - go to start activity
        ageRangeIndex = intent.getIntExtra(Const.ageRangeIndexStr, -1);
        assert(ageRangeIndex != -1);
        moodIndex = intent.getIntExtra(Const.moodStr, -1);
        assert(moodIndex != -1);
        selectedMuseIndex = intent.getIntExtra(Const.selectedMuseIndexStr, -1);
        assert(selectedMuseIndex != -1);
        //Log.i(TAG, "received selected muse index from UserActivity is " + selectedMuseIndex);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        WeakReference<VideoActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to handle connection state changes
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);

        fillIndexes(getIntent());
        initMoodSolver();

        initUI();

        try {
            contentProvider = new ContentProvider(ageRangeIndex);
            videoView.setVideoURI(Uri.fromFile(contentProvider.getNext()));
        } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog();
            return;
        }

        List<Muse> availableMuses = manager.getMuses();

        if (availableMuses.size() == 0)  {
            Log.w(TAG, "There is nothing to connect to");
            return;
        }

        // Cache the Muse that the user has selected.
        muse = availableMuses.get(selectedMuseIndex);
        registerMuseListeners(muse);
        //start receiving muse packets
        muse.runAsynchronously();
        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
    }

    private void initUI() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video);
        initVideoView();
        museState = (TextView) findViewById(R.id.museState);
    }

    private void displayErrorDialog() {
        final Activity activity = this;
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("No suitable video files found. Application will quit")
                .setPositiveButton("Okay", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        activity.finishAffinity();
                    }
                })
                .show();
    }

    private void initMoodSolver() {
        Range<Integer> ageRange = Const.ageRanges[ageRangeIndex];
        User user = new User(ageRange);

        user.setCurrentMood(Const.moods[moodIndex]);

        moodSolver = new MuseMoodSolver(user);
    }

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     * <p>
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     * <p>
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    // Unregister all prior listeners and register our data listener to
    // receive the MuseDataPacketTypes we are interested in.  If you do
    // not register a listener for a particular data type, you will not
    // receive data packets of that type.
    private void registerMuseListeners(Muse muse) {
        muse.unregisterAllListeners();
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
        /*muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
        muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
        muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
        muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);*/
    }

    private TextView museState;

    private void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        final ConnectionState current = p.getCurrentConnectionState();

        //todo: handle connection and disconnection
        if (current == ConnectionState.CONNECTED) {
            videoView.start();
        }

        museState.setText(current.toString());

        if (current == ConnectionState.DISCONNECTED) {
            this.muse = null;
            videoView.pause();
        }
    }

    private VideoView videoView;

    private void initVideoView() {
        videoView = (VideoView) findViewById(R.id.videoView);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.requestFocus();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                videoView.setVideoURI(Uri.fromFile(contentProvider.getNext()));
                videoView.start();
                }
            }
        );
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (muse != null)
            muse.enableDataTransmission(false);
    }

    @Override
    protected void onResume() {
        super.onPause();

        if (muse != null)
            muse.enableDataTransmission(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (muse != null)
            muse.disconnect(true);
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<VideoActivity> activityRef;

        ConnectionListener(final WeakReference<VideoActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }
}

