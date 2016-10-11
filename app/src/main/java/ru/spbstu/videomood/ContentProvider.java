package ru.spbstu.videomood;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;

import java.io.File;

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
        return ageVideos[index++];
    }
}
