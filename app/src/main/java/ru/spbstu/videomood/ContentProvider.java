package ru.spbstu.videomood;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.webkit.PermissionRequest;

import java.io.File;
import java.security.Permission;
import java.util.ArrayList;

import ru.spbstu.videomood.activities.VideoActivity;
import ru.spbstu.videomood.btservice.VideoItem;

public class ContentProvider {

    private File[] videos;

    public final String VIDEO_FILES_DEFAULT_DIRECTORY = "Video";

    public ContentProvider() throws Exception {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            throw new Exception("External storage not available");

        File videoDirectory = new File(Environment.getExternalStorageDirectory(), VIDEO_FILES_DEFAULT_DIRECTORY);

        videos = videoDirectory.listFiles();

        if (videos == null || videos.length == 0)
            throw new Exception(String.format("video directory \"%s\" is empty", videoDirectory));
    }

    private int index = 0;

    public File getNext() {
        index++;
        if (index >= videos.length)
            index = 0;

        return videos[index];
    }

    public File getPrev() {
        index--;
        if (index <= 0)
            index = videos.length-1;

        return videos[index];
    }

    public ArrayList<VideoItem> getContentList() {
        //MediaStore store = new MediaStore();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        ArrayList<VideoItem> result = new ArrayList<>(videos.length);
        for (File video : videos) {
            retriever.setDataSource(video.getPath());
            String timeStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long time = Long.parseLong(timeStr) / Const.SECOND;
            result.add(new VideoItem(video.getName(), (int) time));
        }
        retriever.release();
        return result;
    }

    public File get(Integer videoIndex) {
        return videos[videoIndex];
    }

    public File get(String path) {
        for (File video : videos) {
            if (video.getName().equals(path))
                return video;
        }
        return null;
    }
}
