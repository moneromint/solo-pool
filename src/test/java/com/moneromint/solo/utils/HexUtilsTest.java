package com.moneromint.solo.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class HexUtilsTest {
    static Stream<Arguments> valid() {
        return Stream.of(
                Arguments.of("", new byte[]{}),
                Arguments.of("00", new byte[]{0x00}),
                Arguments.of("01", new byte[]{0x01}),
                Arguments.of("ff", new byte[]{(byte) 0xFF}),
                Arguments.of("deadbeef", new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}),
                Arguments.of("DEADBEEF", new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF})
        );
    }

    static Stream<Arguments> invalid() {
        return Stream.of(
                // invalid length
                Arguments.of("0"),
                Arguments.of("a"),
                Arguments.of("caf"),
                Arguments.of("deadbee"),
                // invalid character
                Arguments.of("deadbeex"),
                Arguments.of("f$")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("valid")
    void testByteArrayToHexString(String hex, byte[] bytes) {
        final String actual = HexUtils.byteArrayToHexString(bytes);
        assertEquals(hex.toLowerCase(), actual.toLowerCase());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("valid")
    void testHexStringToByteArrayValid(String hex, byte[] bytes) throws HexUtils.InvalidHexStringException {
        final byte[] actual = HexUtils.hexStringToByteArray(hex);
        assertArrayEquals(bytes, actual);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalid")
    void testHexStringToByteArrayInvalid(String hex) {
        assertThrows(HexUtils.InvalidHexStringException.class, () -> HexUtils.hexStringToByteArray(hex));
    }
}
