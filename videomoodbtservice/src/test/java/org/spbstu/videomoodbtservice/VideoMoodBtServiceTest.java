package org.spbstu.videomoodbtservice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;

import ru.spbstu.videomood.btservice.BluetoothService;
import ru.spbstu.videomood.btservice.Constants;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VideoMoodBtServiceTest {

    private BluetoothService clientBtService;
    private BluetoothService serverBtService;

    private final String TEST_MESSAGE = "{ msg: TEST_MESSAGE }";

    @Before
    public void initServices() throws IOException {
        //todo: mock handler. Mock method obtainMessage.
        Handler serverHandler = mock(Handler.class);
        Message emptyMessage = new Message();
        when(serverHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, BluetoothService.STATE_LISTEN, -1)).thenReturn(emptyMessage);
        BluetoothAdapter btAdapter = mock(BluetoothAdapter.class);
        BluetoothServerSocket serverSocket = mock(BluetoothServerSocket.class);
        when(btAdapter.listenUsingRfcommWithServiceRecord(BluetoothService.NAME_SECURE, BluetoothService.VIDEOMOOD_BTSERVICE_UUID))
                .thenReturn(serverSocket);

        BluetoothSocket socket = mock(BluetoothSocket.class);
        when(serverSocket.accept()).thenReturn(socket);
        InputStream inStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inStream);
        byte[] buffer = new byte[256];
        when(inStream.read(buffer)).then(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] argument = invocation.getArgument(0);
                byte[] answer = TEST_MESSAGE.getBytes(Constants.DEFAULT_CHARSET);
                for (int i = 0; i < answer.length; i++)
                    argument[i] = answer[i];
                return answer.length;
            }
        });
        serverBtService = new BluetoothService(serverHandler, btAdapter);
    }

    @Test
    public void startServerTest(){

    }

    @Test
    public void connectTest() throws IOException {
        //todo: mock BluetoothDevice. Mock method createRfcommSocketToServiceRecord
        //todo: mock BluetoothServerSocket. Mock method accept, close
        //todo: mock BluetoothSocket. Mock method getInputStream, getOutputStream
        BluetoothDevice btDevice = mock(BluetoothDevice.class);
        BluetoothSocket socket = mock(BluetoothSocket.class);
        when(btDevice.createRfcommSocketToServiceRecord(BluetoothService.VIDEOMOOD_BTSERVICE_UUID)).thenReturn(socket);

        serverBtService.connectToServer(btDevice);
    }

    @Test
    public void readTest() {
        //todo: mock InputStream. Mock method read, expect Handler to receive message
    }

    @Test
    public void writeTest(){
        //todo: mock OutputStream. Mock method write
        //serverBtService.write();
    }
}