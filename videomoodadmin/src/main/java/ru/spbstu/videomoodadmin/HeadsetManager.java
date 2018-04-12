package ru.spbstu.videomoodadmin;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.OnLifecycleEvent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.Constants;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.Packet;
import ru.spbstu.videomood.btservice.PacketType;
import ru.spbstu.videomood.btservice.DataPacket;
import ru.spbstu.videomood.btservice.VideosPacket;

public class HeadsetManager implements LifecycleObserver {
    private static final String TAG = "HeadsetManager";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBtService;
    private BluetoothDevice deviceToConnect;
    private Handler mHandler;
    private String deviceAddress;

    public HeadsetManager(String deviceAddress) {
        this.mHandler = new MessageHandler();
        this.deviceAddress = deviceAddress;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        if (mBtService == null) {
            setupBtService();
            connectDevice(deviceAddress);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        sendMessageHandler.removeCallbacks(sendMessageRunnable);
        if (mBtService != null) {
            mBtService.stop();
        }
    }

    private void setupBtService() {
        mBtService = new BluetoothService(mHandler, BluetoothAdapter.getDefaultAdapter());
    }

    /**
     * Establish connection with other device
     *
     * @param deviceAddress
     */
    private void connectDevice(String deviceAddress) {
        // Get the BluetoothDevice object
        deviceToConnect = mBluetoothAdapter.getRemoteDevice(deviceAddress);

        Log.i(TAG, String.format("Attempt to connectToServer to %s(%s)", deviceToConnect.getName(), deviceAddress));
        // Attempt to connectToServer to the device
        mBtService.connectToServer(deviceToConnect);
    }

    public void sendPacket(ControlPacket controlPacket) {
        if (mBtService != null) {
            byte[] msgBytes = controlPacket.toBytes();
            mBtService.write(msgBytes);
        }
    }

    public void reconnect() {
        mBtService.connectToServer(deviceToConnect);
    }

    private Handler sendMessageHandler = new Handler();

    private Runnable sendMessageRunnable = new Runnable() {
        @Override
        public void run() {
            sendPacket(new ControlPacket(Command.GET));
            sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
        }
    };

    private MutableLiveData<DataPacket> videoStateLiveData = new MutableLiveData<>();

    public LiveData<DataPacket> getVideoState() {
        return videoStateLiveData;
    }

    private MutableLiveData<Integer> headsetStateLiveData = new MutableLiveData<>();

    public LiveData<Integer> getHeadsetState() {
        return headsetStateLiveData;
    }

    private MutableLiveData<VideosPacket> videosLiveData = new MutableLiveData<>();

    public LiveData<VideosPacket> getVideos() {
        return videosLiveData;
    }

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    if (msg.arg1 == BluetoothService.STATE_NONE)
                        onHeadsetDisconnected();
                    else if (msg.arg1 == BluetoothService.STATE_CONNECTED)
                        onHeadsetConnected();
                    headsetStateLiveData.postValue(msg.arg1);
                    break;
                case Constants.MESSAGE_PACKET:
                    Packet packet = Packet.createFrom((String) msg.obj);
                    PacketType type = packet.type();
                    if (type == PacketType.DATA)
                        videoStateLiveData.postValue((DataPacket) packet);
                    else if (type == PacketType.VIDEOS) {
                        videosLiveData.postValue((VideosPacket) packet);
                        sendMessageHandler.removeCallbacks(sendMessageRunnable);
                        sendMessageHandler.postDelayed(sendMessageRunnable, 1000);
                    }
                    break;
            }
        }
    }

    private void onHeadsetConnected() {
        sendPacket(new ControlPacket(Command.LIST));
    }

    private void onHeadsetDisconnected() {
        sendMessageHandler.removeCallbacks(sendMessageRunnable);
    }
}
