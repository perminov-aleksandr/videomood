package ru.spbstu.videomood;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.lang.ref.WeakReference;

import ru.spbstu.videomood.activities.VideoActivity;
import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
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
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        videoActivity.setAdminDeviceStatus(R.string.state_connected);
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        videoActivity.setAdminDeviceStatus(R.string.state_connecting);
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        videoActivity.setAdminDeviceStatus(R.string.state_unknown);
                        break;
                }
                break;
            case Constants.MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                ControlPacket cp = new Gson().fromJson(readMessage, ControlPacket.class);
                videoActivity.processAdminDevicePacket(cp);
                break;
        }
    }
}
