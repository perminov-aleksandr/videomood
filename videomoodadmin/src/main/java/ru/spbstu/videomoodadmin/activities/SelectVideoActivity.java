package ru.spbstu.videomoodadmin.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomood.database.Video;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.VideoAdapter;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class SelectVideoActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private VideoAdapter videosArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video);

        videosArrayAdapter = new VideoAdapter(this, R.layout.device_name);
        List<Video> dbVideos = getDbVideos();
        Collections.sort(dbVideos, new Comparator<Video>() {
            @Override
            public int compare(Video o1, Video o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        videosArrayAdapter.addAll(dbVideos);

        ListView videosListView = (ListView) findViewById(R.id.videoList);
        videosListView.setAdapter(videosArrayAdapter);
        videosListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                Video video = (Video) parent.getItemAtPosition(position);
                intent.putExtra(MainActivity.EXTRA_SELECTED_VIDEO, video.getPath());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Nullable
    private List<Video> getDbVideos() {
        try {
            Dao<Video, Integer> videoDao = getHelper().getDao(Video.class);
            return videoDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
