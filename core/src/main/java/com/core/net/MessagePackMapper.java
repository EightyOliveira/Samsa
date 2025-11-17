package com.core.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;


public final class MessagePackMapper {
    private static final ObjectMapper MAPPER = new ObjectMapper(new MessagePackFactory());

    private MessagePackMapper() {
    }

    public static <T> T fromBytes(byte[] data, Class<T> clazz) throws IOException {
        return MAPPER.readValue(data, clazz);
    }

    public static byte[] toBytes(Object obj) throws IOException {
        return MAPPER.writeValueAsBytes(obj);
    }
}
