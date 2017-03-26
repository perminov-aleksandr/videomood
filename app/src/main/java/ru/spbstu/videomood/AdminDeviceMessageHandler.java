package ru.spbstu.videomood;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import ru.spbstu.videomood.activities.VideoActivity;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;

public class AdminDeviceMessageHandler extends Handler {
    private final WeakReference<VideoActivity> videoActivityRef;

    public AdminDeviceMessageHandler(WeakReference<VideoActivity> videoActivityRef){
        this.videoActivityRef = videoActivityRef;
    }

    @Override
    public void handleMessage(Message msg) {
        VideoActivity videoActivity = videoActivityRef.get();

        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                videoActivity.processAdminDeviceState(msg.arg1);
                break;
            case Constants.MESSAGE_PACKET:
                try {
                    ControlPacket cp = ControlPacket.createFrom((String)msg.obj);
                    if (cp != null)
                        videoActivity.processAdminDevicePacket(cp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
