package ru.spbstu.videomood.btservice;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;

public abstract class Packet {
    public abstract PacketType type();

    public static Packet createFrom(String json) {
        return gson.fromJson(json, Packet.class);
    }

    @Nullable
    public static Packet createFrom(byte[] bytes) {
        try {
            return createFrom(new String(bytes, Constants.DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    static final Gson gson;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder().setLenient();
        gsonBuilder.registerTypeAdapter(Packet.class, new PacketAdapter());
        gson = gsonBuilder.create();
    }

    public String toJson() {
        return gson.toJson(this, Packet.class);
    }

    public byte[] toBytes() {
        try {
            return this.toJson().getBytes(Constants.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    };
}
