package com.moneromint.solo;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifficultyTest {
    static Stream<Arguments> difficulties() {
        return Stream.of(
                Arguments.of(100L, BigInteger.valueOf(677154562L), "285c8f02"),
                Arguments.of(1000L, BigInteger.valueOf(931741952L), "37894100"),
                Arguments.of(5000L, BigInteger.valueOf(1897598208L), "711b0d00"),
                Arguments.of(1000000L, BigInteger.valueOf(3322937344L), "c6100000")
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("difficulties")
    void testTargetHex(long difficulty, BigInteger target, String hex) {
        Difficulty d = new Difficulty(difficulty);
        assertEquals(hex, d.getHex());
        assertEquals(target, d.getTarget());
    }
}
