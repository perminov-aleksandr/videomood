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

    @Nullable
    public static ControlPacket createFrom(byte[] bytes) {
        try {
            String json = new String(bytes, Constants.DEFAULT_CHARSET);
            return createFrom(json);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ControlPacket createFrom(String json) {
        return gson.fromJson(json, ControlPacket.class);
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
