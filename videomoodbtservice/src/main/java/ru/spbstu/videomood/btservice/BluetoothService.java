package ru.spbstu.videomood.btservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonStreamParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothService {
    // Debugging
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    public static final String NAME_SECURE = "BluetoothSecure";

    // Unique UUID for this application
    public static final UUID VIDEOMOOD_BTSERVICE_UUID =
            UUID.nameUUIDFromBytes("video_mood_bt_service".getBytes());

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    private boolean isStoppedOutside = false;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothService(Handler handler, BluetoothAdapter btAdapter) {
        mAdapter = btAdapter;
        mState = STATE_NONE;
        mHandler = handler;
    }

    private String stateStr(int state) {
        switch (state) {
            case STATE_NONE:
                return "NONE";
            case STATE_LISTEN:
                return "LISTEN";
            case STATE_CONNECTING:
                return "CONNECTING";
            case STATE_CONNECTED:
                return "CONNECTED";
            default:
                return "";
        }
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState " + stateStr(mState) + " -> " + stateStr(state));
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically startServer AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void startServer() {
        Log.d(TAG, "startServer");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread();
            mSecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connectToServer
     */
    public synchronized void connectToServer(BluetoothDevice device) {
        Log.d(TAG, "connectToServer to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connectToServer with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, ServiceRole role) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connectToServer to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, role);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        /*Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        isStoppedOutside = true;

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param buffer The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] buffer) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(buffer);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connectToServer device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        setState(STATE_NONE);

        // Start the service over to restart listening mode
        BluetoothService.this.startServer();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        /*Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);*/

        setState(STATE_NONE);

        // Start the service over to restart listening mode
        //BluetoothService.this.startServer();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, VIDEOMOOD_BTSERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    connectionFailed();
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(), ServiceRole.SERVER);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");

        }

        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(VIDEOMOOD_BTSERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: create() failed", e);
            }
            mmSocket = tmp;
            if (tmp == null) {
                Log.e(TAG, "ConnectThread attempt to create socket failed: socket is NULL");
                this.cancel();
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                Log.e(TAG, "Unable to connect", e);
                connectionLost();
                return;
            }

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, ServiceRole.CLIENT);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connectToServer socket failed", e);
            }
            connectionLost();
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private final ServiceRole role;

        ConnectedThread(BluetoothSocket socket, ServiceRole role) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
                connectionLost();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            this.role = role;
        }

        /*
        JsonReader reader = new JsonReader(new InputStreamReader(mmInStream));
        while(reader.hasNext()) {
            DataPacket dataPacket = new DataPacket();
        }
        */

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            //byte[] buffer = new byte[1024*1024];
            //String prevMessage = "";

            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.registerTypeAdapter(Packet.class, new PacketJsonAdapter());
            Gson gson = gsonBuilder.create();

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    Reader r = new InputStreamReader(mmInStream, Constants.DEFAULT_CHARSET);
                    JsonStreamParser p = new JsonStreamParser(r);

                    while (p.hasNext()) {
                        JsonElement e = p.next();
                        if (e.isJsonObject()) {
                            Packet packet = gson.fromJson(e, Packet.class);
                            Log.i(TAG, String.format("RECEIVING: %s", e));
                            mHandler.obtainMessage(Constants.MESSAGE_PACKET, -1, -1, packet)
                                    .sendToTarget();
                        }
                    }

                    /*
                    // Read from the InputStream
                    int bytes = mmInStream.read(buffer);
                    byte[] actualBytes = Arrays.copyOf(buffer, bytes);
                    String currentMessage = new String(actualBytes, Constants.DEFAULT_CHARSET);
                    prevMessage = prevMessage.concat(currentMessage);
                    Log.i(TAG, String.format("RECEIVING: %s", prevMessage));

                    if (prevMessage.endsWith("}")) {
                        mHandler.obtainMessage(Constants.MESSAGE_PACKET, -1, -1, prevMessage)
                                .sendToTarget();
                        prevMessage = "";
                    }*/
                } catch (JsonIOException|IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    if (role == ServiceRole.SERVER && !isStoppedOutside)
                        BluetoothService.this.startServer();
                    break;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            try {
                Log.i(TAG, String.format("SENDING: %s", new String(buffer, Constants.DEFAULT_CHARSET)));
                mmOutStream.write(buffer);
            } catch (Exception e) {
                Log.e(TAG, "Exception during write", e);
                connectionLost();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connectToServer socket failed", e);
            }
        }
    }
}

