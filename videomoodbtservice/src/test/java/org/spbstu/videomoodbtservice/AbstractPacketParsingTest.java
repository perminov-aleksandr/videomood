package org.spbstu.videomoodbtservice;

import org.junit.Assert;
import org.junit.Test;

import ru.spbstu.videomood.btservice.Command;
import ru.spbstu.videomood.btservice.ControlPacket;
import ru.spbstu.videomood.btservice.Packet;
import ru.spbstu.videomood.btservice.PacketType;

import static org.junit.Assert.*;

public class AbstractPacketParsingTest {

    private ControlPacket controlPacket = new ControlPacket(Command.LIST);
    private String controlPacketSerialized = "{\"type\":\"CONTROL\",\"properties\":{\"command\":\"LIST\",\"arguments\":[]}}";

    @Test
    public void Serialize() {
        String result = controlPacket.toJson();
        assertEquals(controlPacketSerialized, result);
    }

    @Test
    public void Deserialize() {
        Packet packet = Packet.createFrom(controlPacketSerialized);
        assertEquals(PacketType.CONTROL, packet.type());

        ControlPacket controlPacket = (ControlPacket) packet;
        assertEquals(Command.LIST, controlPacket.getCommand());
    }

    @Test
    public void SerializeDeserialize() {
        String serializedPacket = controlPacket.toJson();
        Packet packet = Packet.createFrom(serializedPacket);
        assertTrue(packet instanceof ControlPacket);
    }
}
