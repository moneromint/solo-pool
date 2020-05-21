package com.moneromint.solo.stratum;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NdjsonMessageEncoderTest {
    EmbeddedChannel chan;

    static Stream<Arguments> messages() {
        return Stream.of(
                Arguments.of("{\"foo\":\"bar\"}\n", Map.of("foo", "bar"))
        );
    }

    @BeforeEach
    void setup() {
        chan = new EmbeddedChannel(new NdjsonMessageEncoder());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("messages")
    void testEncode(String expected, Object object) {
        chan.writeOutbound(object);
        final String encoded = ((ByteBuf) chan.readOutbound()).toString(CharsetUtil.UTF_8);
        // TODO: JSONAssert?
        assertEquals(expected, encoded);
    }
}
