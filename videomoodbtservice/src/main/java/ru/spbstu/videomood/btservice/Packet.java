package ru.spbstu.videomood.btservice;

public abstract class Packet {
    public abstract PacketType type();

    public abstract byte[] toBytes();
}
