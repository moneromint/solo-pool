package com.moneromint.solo.stratum.message;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.DataInput;
import java.util.List;

public class StratumMessageDecoder extends ByteToMessageDecoder {
    private static final ObjectMapper MAPPER;

    static {
        final var module = new SimpleModule();
        module.addDeserializer(StratumMessage.class, new StratumMessageDeserializer());
        MAPPER = new ObjectMapper();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(module);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        final DataInput dataInput = new ByteBufInputStream(in);
        out.add(MAPPER.readValue(dataInput, StratumMessage.class));
    }
}
