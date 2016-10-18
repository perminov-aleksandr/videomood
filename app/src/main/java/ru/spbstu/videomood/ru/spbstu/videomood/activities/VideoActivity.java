package ru.spbstu.videomood.ru.spbstu.videomood.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.choosemuse.libmuse.Battery;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataPacket;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ru.spbstu.videomood.ConnectionListener;
import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.ContentProvider;
import ru.spbstu.videomood.DataListener;
import ru.spbstu.videomood.MuseManager;
import ru.spbstu.videomood.MuseMoodSolver;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.Range;
import ru.spbstu.videomood.User;
import ru.spbstu.videomood.Utils;

public class VideoActivity extends Activity {

    private static final String TAG = "VideoMood:VideoActivity";
    private MuseMoodSolver moodSolver;

    private final double[][] relativeBuffer = new double[5][4];
    private final double[] meanScores = new double[5];
    private boolean relativeStale = false;

    private TextView foreheadTouch;
    private boolean isForeheadTouch = false;

    private TextView[] isGoodIndicators;
    private final boolean[] isGoodBuffer = new boolean[4];
    private boolean isGoodStale = false;

    private TextView batteryTextView;
    private double batteryValue;
    private boolean batteryStale = false;

    private void updateIsGood() {
        for (int i = 0; i < isGoodBuffer.length; i++)
            isGoodIndicators[i].setVisibility(isGoodBuffer[i] ? View.VISIBLE : View.INVISIBLE);

        foreheadTouch.setVisibility(isForeheadTouch ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateBattery() {
        batteryTextView.setText(String.format("%s%%", batteryValue));
        batteryStale = false;
    }

    private TextView alphaBar;
    private TextView betaBar;

    private void updateScores() {
        //todo: refactor this. TOO EXPLICIT DEPENDENCY FROM relativeBuffer and other
        BarValues barValues = new BarValues().calculate();
        long alphaPercent = barValues.getAlphaPercent();
        long betaPercent = barValues.getBetaPercent();

        alphaBar.setText(String.format("%d", alphaPercent));
        betaBar.setText(String.format("%d", betaPercent));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) alphaBar.getLayoutParams();
        params.weight = (float)alphaPercent;
        alphaBar.setLayoutParams(params);

        params = (LinearLayout.LayoutParams) betaBar.getLayoutParams();
        params.weight = (float)betaPercent;
        betaBar.setLayoutParams(params);
    }

    private void updateMood() {
        //TextView moodTextView = (TextView) findViewById(R.id.mood);
        //moodTextView.setText(moodSolver.getUser().getCurrentMood().toString());
    }

    public void processMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        ArrayList<Double> packetValues = p.values();
        switch (p.packetType()) {
            case ALPHA_RELATIVE:
                fillRelativeBufferWith(Const.Rhythms.ALPHA, packetValues);
                relativeStale = true;
                break;
            case BETA_RELATIVE:
                fillRelativeBufferWith(Const.Rhythms.BETA, packetValues);
                relativeStale = true;
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

    private void fillRelativeBufferWith(final int rangeIndex, final ArrayList<Double> packetValues) {
        for (int i = 0; i < packetValues.size(); i++) {
            Double v = packetValues.get(i);
            relativeBuffer[rangeIndex][i] = v;
            //Log.i(TAG, String.format("ALPHA: %2.6f", v) );
        }
    }

    public void processMuseArtifactPacket(final MuseArtifactPacket p, Muse muse) {
        isForeheadTouch = p.getHeadbandOn();
        isGoodStale = true;
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
            if (relativeStale) {
                updateScores();
                //moodSolver.solve(meanScores);
                //updateMood();
                relativeStale = false;
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
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    private Muse muse;

    private ContentProvider contentProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WeakReference<VideoActivity> weakActivity = new WeakReference<>(this);
        // Register a listener to handle connection state changes
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
        dataListener = new DataListener(weakActivity);

        moodSolver = new MuseMoodSolver();

        initUI();

        try {
            Range<Integer> ageRange = User.getAgeRange();
            int ageRangeIndex;
            for (ageRangeIndex = 0; ageRangeIndex < Const.ageRanges.length; ageRangeIndex++)
                if (Const.ageRanges[ageRangeIndex] == ageRange)
                    break;
            contentProvider = new ContentProvider(ageRangeIndex);
            videoView.setVideoURI(Uri.fromFile(contentProvider.getNext()));
        } catch (Exception e) {
            e.printStackTrace();
            displayErrorDialog();
            return;
        }

        MuseManager.setContext(this);
        // Cache the Muse that the user has selected.
        muse = MuseManager.getMuse();
        MuseManager.registerMuseListeners(connectionListener, dataListener);
        //start receiving muse packets
        muse.runAsynchronously();
        // Start our asynchronous updates of the UI.
        handler.post(tickUi);
    }

    private void initUI() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video);
        initTextViews();
        initVideoView();
    }

    private void initTextViews(){
        alphaBar = (TextView) findViewById(R.id.alpha);
        betaBar = (TextView) findViewById(R.id.beta);
        batteryTextView = (TextView) findViewById(R.id.battery);
        museState = (TextView) findViewById(R.id.museState);

        isGoodIndicators = new TextView[4];
        isGoodIndicators[Const.Electrodes.FIRST] = (TextView) findViewById(R.id.good1);
        isGoodIndicators[Const.Electrodes.SECOND] = (TextView) findViewById(R.id.good2);
        isGoodIndicators[Const.Electrodes.THIRD] = (TextView) findViewById(R.id.good3);
        isGoodIndicators[Const.Electrodes.FOURTH] = (TextView) findViewById(R.id.good4);

        foreheadTouch = (TextView) findViewById(R.id.forehead);
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

    private TextView museState;

    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
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
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoView.setVideoURI(Uri.fromFile(contentProvider.getNext()));
                videoView.start();
            }
        });
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

    private class BarValues {
        private long alphaPercent;
        private long betaPercent;

        public long getAlphaPercent() {
            return alphaPercent;
        }

        public long getBetaPercent() {
            return betaPercent;
        }

        /***
         * Calculate weighted values each of channel in relativeBuffer for ALPHA and BETA ranges.
         * @return new BarValues instance filled according to calculated values
         */
        public BarValues calculate() {
            double alphaMean = Utils.mean(relativeBuffer[Const.Rhythms.ALPHA]);
            double betaMean = Utils.mean(relativeBuffer[Const.Rhythms.BETA]);

            double t = 100.0 / (alphaMean + betaMean);

            double alphaWeighted = alphaMean * t;
            double betaWeighted = betaMean * t;

            alphaPercent = Math.round(alphaWeighted);
            betaPercent = Math.round(betaWeighted);
            return this;
        }
    }
}

