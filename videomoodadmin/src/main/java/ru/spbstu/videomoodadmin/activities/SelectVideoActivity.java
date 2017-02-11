package ru.spbstu.videomoodadmin.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;

import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomoodadmin.R;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class SelectVideoActivity extends AppCompatActivity {

    private ArrayAdapter<String> videosArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video);

        videosArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        //videosArrayAdapter.addAll(MainActivity.videoItems);

        ListView pairedListView = (ListView) findViewById(R.id.videoList);
        pairedListView.setAdapter(videosArrayAdapter);
        pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra(MainActivity.EXTRA_SELECTED_VIDEO, id);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
