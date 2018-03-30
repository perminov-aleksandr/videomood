package org.spbstu.videomoodbtservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import ru.spbstu.videomood.btservice.VideoActivityState;

public class IncompletePacketsParcingTest {
    private String completePacketJsonExample = "{\"alphaPct\":0,\"betaPct\":0,\"currentPosition\":0,\"duration\":0,\"headsetBatteryPercent\":57,\"isPanic\":false,\"museSensorsState\":[false,false,false,false],\"museState\":false,\"videoName\":\"Svinka.Peppa.(S.01.E.16.iz.52).Ikota.2005.XviD.SATRip_x264.mp4\",\"videoState\":false}";

    private String incompletePacketJsonExample = "{\"alphaPct\":0,\"betaPct\":0,\"currentPosition\":0,\"duration\":0,\"headsetBatteryPercent\":57,\"isPanic\":false,\"museSensorsState\":[false,false,false,false],\"mus";

    private String overCompletePacketJsonExample = String.format("%s%s", completePacketJsonExample, incompletePacketJsonExample);

    private byte[] incompletePacketJsonExampleBytes;
    private byte[] overcompletePacketJsonExampleBytes;

    private final String charsetName = "UTF-8";

    @Before
    public void init() throws UnsupportedEncodingException {
        incompletePacketJsonExampleBytes = incompletePacketJsonExample.getBytes( charsetName );
        overcompletePacketJsonExampleBytes = overCompletePacketJsonExample.getBytes( charsetName );
    }

    @Test
    public void parseTest() throws IOException {

        InputStream input = new ByteArrayInputStream(overcompletePacketJsonExampleBytes);
        JsonReader reader = new JsonReader(new InputStreamReader(input, charsetName));

        Gson gson = new GsonBuilder().create();

        // Read file in stream mode
        while (reader.hasNext()) {
            // Read data into object model
            try {
                VideoActivityState packet = gson.fromJson(reader, VideoActivityState.class);
                //System.console().writer().print(packet);
            } catch (JsonSyntaxException ex) {

            }

            break;
        }
        reader.close();
    }
}
