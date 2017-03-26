package ru.spbstu.videomoodadmin.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

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
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.R;

public class SeanceActivity extends Activity {

    private Seance seance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seance);

        Intent prevIntent = getIntent();
        int seanceId = prevIntent.getIntExtra(AdminConst.EXTRA_SEANCE_ID, -1);
        String seanceDataStr = prevIntent.getStringExtra(AdminConst.EXTRA_SEANCE_DATA);
        String seanceDateFrom = prevIntent.getStringExtra(AdminConst.EXTRA_SEANCE_DATEFROM);
        String seanceDateTo = prevIntent.getStringExtra(AdminConst.EXTRA_SEANCE_DATETO);

        seance = new Seance();
        seance.setId(seanceId);
        seance.setDateFrom(seanceDateFrom);
        seance.setDateTo(seanceDateTo);
        seance.setData(seanceDataStr);

        try {
            TextView dateTv = (TextView) findViewById(R.id.seance_card_date);
            Date dateFrom = Seance.dateFormat.parse(seance.getDateFrom());
            dateTv.setText(new SimpleDateFormat("dd.MM.yyyy").format(dateFrom));

            TextView from = (TextView) findViewById(R.id.seance_card_from);
            DateFormat timeFormat = new SimpleDateFormat("hh:mm");
            from.setText(timeFormat.format(dateFrom));

            TextView to = (TextView) findViewById(R.id.seance_card_to);
            Date dateTo = Seance.dateFormat.parse(seance.getDateTo());
            to.setText(timeFormat.format(dateTo));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        initChart();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishActivity(RESULT_OK);
    }

    public void initChart() {
        BarChart seanceBarChart = (BarChart) findViewById(R.id.seance_dataBarChart);

        List<BarEntry> alphaEntries = new ArrayList<>();
        List<BarEntry> betaEntries = new ArrayList<>();
        int time = 0;
        for (SeanceDataEntry seanceDataEntry : seance.getData()) {
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
    }
}
