package ru.spbstu.videomoodadmin.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
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
        seance.setDateFrom(seanceDateTo);
        seance.setData(seanceDataStr);

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
    }
}
