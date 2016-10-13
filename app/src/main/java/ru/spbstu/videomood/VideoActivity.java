package ru.spbstu.videomood;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class VideoActivity extends Activity {

    private static final String TAG = "VideoMood:VideoActivity";
    private MuseMoodSolver moodSolver;

    private final double[][] scoresBuffer = new double[5][4];
    private final double[] meanScores = new double[5];
    private boolean scoresStale = false;

    private final boolean isForeheadTouch = false;
    private final boolean[] isGoodBuffer = new boolean[4];
    private boolean isGoodStale = false;

    private double batteryValue;
    private boolean batteryStale = false;

    private void updateIsGood() {
        TextView good1 = (TextView) findViewById(R.id.good1);
        TextView good2 = (TextView) findViewById(R.id.good2);
        TextView good3 = (TextView) findViewById(R.id.good3);
        TextView good4 = (TextView) findViewById(R.id.good4);
        TextView foreheadTouch = (TextView) findViewById(R.id.forehead);
        good1.setVisibility(isGoodBuffer[Const.FIRST] ? View.VISIBLE : View.INVISIBLE);
        good2.setVisibility(isGoodBuffer[Const.SECOND] ? View.VISIBLE : View.INVISIBLE);
        good3.setVisibility(isGoodBuffer[Const.THIRD] ? View.VISIBLE : View.INVISIBLE);
        good4.setVisibility(isGoodBuffer[Const.FOURTH] ? View.VISIBLE : View.INVISIBLE);
        foreheadTouch.setVisibility(isForeheadTouch ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateBattery() {
        TextView batteryTextView = (TextView) findViewById(R.id.battery);
        batteryTextView.setText(String.format("%s%%", batteryValue));
        batteryStale = false;
    }

    private double getMean(double[] values) {
        double res = 0;
        for (int i = 0; i < values.length; i++)
            res += values[i];
        return res / values.length;
    }

    private void updateScores() {
        TextView alpha = (TextView) findViewById(R.id.alpha);
        TextView beta = (TextView) findViewById(R.id.beta);
        TextView gamma = (TextView) findViewById(R.id.gamma);
        TextView delta = (TextView) findViewById(R.id.delta);
        TextView theta = (TextView) findViewById(R.id.theta);
        alpha.setText(String.format("%1.2f", getMean(scoresBuffer[Const.ALPHA])));
        beta.setText(String.format("%1.2f", getMean(scoresBuffer[Const.BETA])));
        gamma.setText(String.format("%1.2f", getMean(scoresBuffer[Const.GAMMA])));
        delta.setText(String.format("%1.2f", getMean(scoresBuffer[Const.DELTA])));
        theta.setText(String.format("%1.2f", getMean(scoresBuffer[Const.THETA])));
    }

    private void updateMood() {
        TextView moodTextView = (TextView) findViewById(R.id.mood);
        moodTextView.setText(moodSolver.getUser().getCurrentMood().toString());
    }

    public void processMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        ArrayList<Double> packetValues = p.values();
        switch (p.packetType()) {
            case ALPHA_SCORE:
                for (int i = 0; i < packetValues.size(); i++)
                    scoresBuffer[Const.ALPHA][i] = packetValues.get(i);
                break;
            case BETA_SCORE:
                for (int i = 0; i < packetValues.size(); i++)
                    scoresBuffer[Const.BETA][i] = packetValues.get(i);
                break;
            case GAMMA_SCORE:
                for (int i = 0; i < packetValues.size(); i++)
                    scoresBuffer[Const.GAMMA][i] = packetValues.get(i);
                break;
            case DELTA_SCORE:
                for (int i = 0; i < packetValues.size(); i++)
                    scoresBuffer[Const.DELTA][i] = packetValues.get(i);
                break;
            case THETA_SCORE:
                for (int i = 0; i < packetValues.size(); i++)
                    scoresBuffer[Const.THETA][i] = packetValues.get(i);
                break;
            case BATTERY:
                batteryValue = p.getBatteryValue(Battery.CHARGE_PERCENTAGE_REMAINING);
                batteryStale = true;
                break;
            case IS_GOOD:
                for (int i = 0; i < 4; i++)
                    isGoodBuffer[i] = packetValues.get(i) > 0.5;
                isGoodStale = true;
                break;
            default:
                break;
        }
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
            if (scoresStale) {
                updateScores();
                moodSolver.solve(meanScores);
                updateMood();
                scoresStale = false;
            }
            if (isGoodStale) {
                updateIsGood();
                isGoodStale = false;
            }
            if (batteryStale) {
                updateBattery();
                batteryStale = false;
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
        muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_SCORE);
        muse.registerDataListener(dataListener, MuseDataPacketType.BETA_SCORE);
        muse.registerDataListener(dataListener, MuseDataPacketType.GAMMA_SCORE);
        muse.registerDataListener(dataListener, MuseDataPacketType.DELTA_SCORE);
        muse.registerDataListener(dataListener, MuseDataPacketType.THETA_SCORE);
        muse.registerDataListener(dataListener, MuseDataPacketType.IS_GOOD);
        muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
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

