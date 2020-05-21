package com.moneromint.solo.stratum;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.DataOutput;

public class NdjsonMessageEncoder extends MessageToByteEncoder<Object> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        final DataOutput dataOutput = new ByteBufOutputStream(out);
        MAPPER.writeValue(dataOutput, msg);
        dataOutput.write('\n');
    }
}
