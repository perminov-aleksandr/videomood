package ru.spbstu.videomoodadmin.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomood.database.SeanceVideo;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.SeanceVideoAdapter;
import ru.spbstu.videomoodadmin.Timeline;

public class SeanceActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private static final String TAG = "SeanceActivity";
    private EditText actionEditor;
    private TextView actionTv;
    private EditText commentEditor;
    private TextView commentTv;

    private ListView videosListView;

    private Button editButton;

    private Seance seance;

    private Dao<Seance, Integer> seanceDao;
    private Timeline timeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seance);

        seance = findSeance();
        if (seance == null) {
            Toast.makeText(SeanceActivity.this, R.string.seanceNotFoundError, Toast.LENGTH_LONG);
            finish();
            return;
        }

        try {
            TextView dateTv = (TextView) findViewById(R.id.seance_card_date);
            Date dateFrom = Seance.dateFormat.parse(seance.getDateFrom());
            dateTv.setText(new SimpleDateFormat("dd.MM.yyyy").format(dateFrom));

            TextView from = (TextView) findViewById(R.id.seance_card_from);
            DateFormat timeFormat = new SimpleDateFormat("hh:mm");
            from.setText(timeFormat.format(dateFrom));

            TextView to = (TextView) findViewById(R.id.seance_card_to);
            String dateToStr = seance.getDateTo();
            if (dateToStr != null) {
                Date dateTo = Seance.dateFormat.parse(dateToStr);
                to.setText(timeFormat.format(dateTo));
            }

            actionEditor = (EditText) findViewById(R.id.seance_card_action_edit);
            actionTv = (TextView) findViewById(R.id.seance_card_action);
            actionTv.setText(seance.getAction());

            commentEditor = (EditText) findViewById(R.id.seance_card_comment_edit);
            commentTv = (TextView) findViewById(R.id.seance_card_comment);
            commentTv.setText(seance.getComment());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        videosListView = (ListView) findViewById(R.id.seance_card_videos);
        SeanceVideoAdapter videosAdapter = new SeanceVideoAdapter(SeanceActivity.this, R.layout.seance_video_item);
        ForeignCollection<SeanceVideo> seanceVideos = seance.getSeanceVideos();
        for (SeanceVideo seanceVideo : seanceVideos)
            videosAdapter.add(seanceVideo);
        videosListView.setAdapter(videosAdapter);

        ArrayList<Timeline.TimeLineEvent> timelineEvents = new ArrayList<>(seanceVideos.size());
        for (SeanceVideo seanceVideo : seanceVideos) {
            timelineEvents.add(new Timeline.TimeLineEvent(seanceVideo.video.getName(), seanceVideo.getTimestamp()));
        }

        timeline = (Timeline) findViewById(R.id.seance_timeline);
        timeline.setTimelineEvents(timelineEvents);

        editButton = (Button) findViewById(R.id.seance_card_editBtn);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit();
            }
        });

        Button removeButton = (Button) findViewById(R.id.seance_card_deleteBtn);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove();
            }
        });

        initChart();
    }

    private void remove() {
        try {
            seanceDao.delete(seance);
        } catch (SQLException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        finish();
    }

    @Nullable
    private Seance findSeance() {
        Intent prevIntent = getIntent();
        int seanceId = prevIntent.getIntExtra(AdminConst.EXTRA_SEANCE_ID, -1);

        try {
            seanceDao = getHelper().getDao(Seance.class);
            return seanceDao.queryForId(seanceId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*Intent intent = new Intent(SeanceActivity.this, UsersActivity.class);
        startActivity(intent);*/
        finish();
    }

    public void initChart() {
        List<SeanceDataEntry> seanceData = new ArrayList<>();
        ForeignCollection<SeanceVideo> seanceVideos = seance.getSeanceVideos();
        for (SeanceVideo seanceVideo : seanceVideos) {
            seanceData.addAll(seanceVideo.getData());
        }

        if (seanceData.isEmpty())
            return;

        List<BarEntry> alphaEntries = new ArrayList<>();
        List<BarEntry> betaEntries = new ArrayList<>();
        int time = 0;
        for (SeanceDataEntry seanceDataEntry : seanceData) {
            BarEntry barEntry = new BarEntry(time++, seanceDataEntry.betaValue);
            if (seanceDataEntry.isPanic)
                betaEntries.add(barEntry);
            else
                alphaEntries.add(barEntry);
        }

        BarDataSet alphaSet = new BarDataSet(alphaEntries, getString(R.string.calm));
        alphaSet.setColor(getResources().getColor(R.color.calmColor));

        BarDataSet betaSet = new BarDataSet(betaEntries, getString(R.string.warning));
        betaSet.setColor(getResources().getColor(R.color.warningColor));

        BarData data = new BarData();
        data.addDataSet(alphaSet);
        data.addDataSet(betaSet);

        BarChart seanceBarChart = (BarChart) findViewById(R.id.seance_dataBarChart);
        seanceBarChart.setData(data);

        data.setDrawValues(false);

        XAxis xAx = seanceBarChart.getXAxis(); //no axis
        YAxis yAx = seanceBarChart.getAxisLeft();
        YAxis yRightAx = seanceBarChart.getAxisRight();
        xAx.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                int minutes = (int) (value / 60.0);
                int seconds = (int) (value % 60);
                return String.format("%d:%02d", minutes, seconds);
            }
        });
        //xAx.setEnabled(false);
        seanceBarChart.setFitBars(true);
        seanceBarChart.setVisibleXRangeMinimum(30);
        seanceBarChart.setVisibleXRangeMaximum(120);

        //yAx.setEnabled(false);
        //yRightAx.setEnabled(false);

        seanceBarChart.setDrawGridBackground(false); //no grid
        seanceBarChart.setDescription(null); //no description
        seanceBarChart.setBorderWidth(0f);

        seanceBarChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                timeline.setScaleFactor(scaleX);
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                //timeline.scrollBy((int) dX, 0);
            }
        });
    }

    private boolean isEditMode = false;

    private void edit() {
        if (isEditMode)
            saveChanges();
        isEditMode = !isEditMode;
        toggleEditBoxes();
    }

    private void saveChanges() {
        seance.setAction(actionEditor.getText().toString());
        seance.setComment(commentEditor.getText().toString());

        try {
            seanceDao.update(seance);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void toggleEditBoxes() {
        editButton.setText(isEditMode ? R.string.save : R.string.edit);

        if (isEditMode) {
            actionTv.setVisibility(View.GONE);
            actionEditor.setText(seance.getAction());
            actionEditor.setVisibility(View.VISIBLE);

            commentTv.setVisibility(View.GONE);
            commentEditor.setText(seance.getComment());
            commentEditor.setVisibility(View.VISIBLE);
        }
        else {
            actionEditor.setVisibility(View.GONE);
            actionTv.setText(seance.getAction());
            actionTv.setVisibility(View.VISIBLE);

            commentEditor.setVisibility(View.GONE);
            commentTv.setText(seance.getComment());
            commentTv.setVisibility(View.VISIBLE);
        }
    }
}
