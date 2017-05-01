package org.spbstu.videomoodbtservice;

import android.test.mock.MockContext;

import org.junit.Before;
import org.junit.Test;

import ru.spbstu.videomood.btservice.BluetoothService;

import static org.junit.Assert.*;

public class VideoMoodBtServiceTest {

    private BluetoothService btService;

    @Before
    public void initService() {
        //todo: mock handler. Mock method obtainMessage.
        //btService = new BluetoothService();
    }

    @Test
    public void connectTest() {
        //todo: mock BluetoothDevice. Mock method createRfcommSocketToServiceRecord
        //todo: mock BluetoothServerSocket. Mock method accept, close
        //todo: mock BluetoothSocket. Mock method getInputStream, getOutputStream
        //btService.connect();
        btService.start();
    }

    @Test
    public void readTest() {
        //todo: mock InputStream. Mock method read, expect Handler to receive message
    }

    @Test
    public void writeTest(){
        //todo: mock OutputStream. Mock method write
        //btService.write();
    }
}