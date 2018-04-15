package ru.spbstu.videomood.btservice;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

class PacketJsonAdapter implements JsonSerializer<Packet>, JsonDeserializer<Packet> {

    @Override
    public Packet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        JsonElement element = jsonObject.get("properties");

        switch (type) {
            case "VIDEOS":
                return context.deserialize(element, VideosPacket.class);
            case "DATA":
                return context.deserialize(element, DataPacket.class);
            case "CONTROL":
                return context.deserialize(element, ControlPacket.class);
            default:
                throw new JsonParseException("Unknown type");
        }
    }

    @Override
    public JsonElement serialize(Packet src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        PacketType type = src.type();
        switch (type) {
            case VIDEOS:
                result.add("type", new JsonPrimitive("VIDEOS"));
                break;
            case DATA:
                result.add("type", new JsonPrimitive("DATA"));
                break;
            case CONTROL:
                result.add("type", new JsonPrimitive("CONTROL"));
                break;
        }
        result.add("properties", context.serialize(src, src.getClass()));
        return result;
    }
}
