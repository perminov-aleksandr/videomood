package ru.spbstu.videomood;

import android.media.MediaMetadataRetriever;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;

import ru.spbstu.videomood.btservice.VideoItem;

public class ContentProvider {

    private File[] videos;

    public ContentProvider() throws Exception {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            throw new Exception("External storage not available");

        File videoDirectory = new File(Environment.getExternalStorageDirectory(), "Video");

        videos = videoDirectory.listFiles();

        if (videos == null || videos.length == 0)
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
            result.add(new VideoItem(videos[i].getName(), (int) time));
        }
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
