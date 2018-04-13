package ru.spbstu.videomood.btservice;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;

public class ControlPacket extends Packet {
    private Command command;

    private Object[] arguments;

    public ControlPacket(Command command, Object... arguments) {
        this.command = command;
        this.arguments = arguments;
    }

    public Command getCommand() {
        return command;
    }

    public Object[] getArguments() {
        return arguments;
    }

    @Override
    public PacketType type() {
        return PacketType.CONTROL;
    }
}
