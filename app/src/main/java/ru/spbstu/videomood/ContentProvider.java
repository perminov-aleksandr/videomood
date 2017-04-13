package ru.spbstu.videomood;

import android.annotation.TargetApi;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.spbstu.videomood.btservice.VideoItem;

public class ContentProvider {

    private File[] videos;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ContentProvider() throws Exception {
        File videoDirectory = new File(Environment.getExternalStorageDirectory(), "Video");

        videos = videoDirectory.listFiles();

        if (videos.length <= 0)
            throw new Exception(String.format("video directory \"%s\" is empty", videoDirectory));
    }

    private int index = 0;

    public File getNext() {
        index++;
        if (index >= videos.length)
            index = 0;

        File nextVideo = videos[index];
        return nextVideo;
    }

    public File getPrev() {
        index--;
        if (index <= 0)
            index = videos.length-1;

        File nextVideo = videos[index];
        return nextVideo;
    }

    public ArrayList<VideoItem> getContentList() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        ArrayList<VideoItem> result = new ArrayList<>(videos.length);
        for (int i = 0; i < videos.length; i++) {
            retriever.setDataSource(videos[i].getPath());
            String timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long time = Long.parseLong(timeStr) / 1000;
            result.add(new VideoItem(i, videos[i].getName(), (int) time));
        }
        return result;
    }

    public File get(Integer videoIndex) {
        return videos[videoIndex];
    }
}
