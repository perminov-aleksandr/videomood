package ru.spbstu.videomood;

import android.annotation.TargetApi;
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

    private final File ageVideoDir;

    private File[] ageVideos;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ContentProvider(int ageRangeIndex) throws Exception {
        File videoStorage = new File(Environment.getExternalStorageDirectory(), "Video");
        File[] ageDirectories = videoStorage.listFiles();

        if (ageDirectories.length <= 0)
            throw new Exception("Age video directories not found");

        ageVideoDir = ageDirectories[ageRangeIndex];
        ageVideos = ageVideoDir.listFiles();

        if (ageVideos.length <= 0)
            throw new Exception(String.format("video directory \"%s\" for age \"%s\" is empty", ageVideoDir.getAbsolutePath(), Const.ageRanges[ageRangeIndex].toString()));
    }

    private int index = 0;

    public File getNext() {
        index++;
        if (index >= ageVideos.length)
            index = 0;

        File nextVideo = ageVideos[index];
        return nextVideo;
    }

    public File getPrev() {
        index--;
        if (index <= 0)
            index = ageVideos.length-1;

        File nextVideo = ageVideos[index];
        return nextVideo;
    }

    public ArrayList<VideoItem> getContentList() {
        //File videoStorage = new File(Environment.getExternalStorageDirectory(), "Video");
        File[] ageDirectories = ageVideoDir.listFiles();

        ArrayList<VideoItem> result = new ArrayList<>(ageDirectories.length);
        for (int i = 0; i < ageDirectories.length; i++) {
            result.add(new VideoItem(i, ageDirectories[i].getName(), ""));
        }
        return result;
    }

    public File get(Integer videoIndex) {
        File[] ageDirectories = ageVideoDir.listFiles();

        return ageDirectories[videoIndex];
    }
}
