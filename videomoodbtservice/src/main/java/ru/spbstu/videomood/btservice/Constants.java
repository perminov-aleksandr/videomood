package ru.spbstu.videomood.btservice;

/**
 * Defines several constants used between {@link BluetoothService} and the UI.
 */
public interface Constants {

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_PACKET = 2;
    public static final String DEFAULT_CHARSET = "UTF-8";
}
