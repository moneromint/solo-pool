package com.moneromint.solo.stratum.message;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StratumMessageDecoderTest {
    EmbeddedChannel chan;

    static Stream<Arguments> messages() {
        return Stream.of(
                Arguments.of(
                        "{\"id\":\"0\",\"method\":\"login\",\"params\":{\"login\":\"x\",\"pass\":\"x\"}}",
                        new StratumRequest<>("0", new StratumLoginParams("x", "x"))
                )
        );
    }

    @BeforeEach
    void setup() {
        chan = new EmbeddedChannel(new StratumMessageDecoder());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("messages")
    void testDecode(String message, Object expected) {
        chan.writeInbound(Unpooled.wrappedBuffer(message.getBytes(CharsetUtil.UTF_8)));
        final Object actual = chan.readInbound();
        assertEquals(expected, actual);
    }
}
