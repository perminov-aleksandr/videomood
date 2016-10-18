package ru.spbstu.videomood;

import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;

import java.lang.ref.WeakReference;

import ru.spbstu.videomood.ru.spbstu.videomood.activities.VideoActivity;

public class ConnectionListener extends MuseConnectionListener {
    final WeakReference<VideoActivity> activityRef;

    public ConnectionListener(final WeakReference<VideoActivity> activityRef) {
        this.activityRef = activityRef;
    }

    @Override
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        activityRef.get().receiveMuseConnectionPacket(p, muse);
    }
}