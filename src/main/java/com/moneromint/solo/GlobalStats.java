package com.moneromint.solo;

import com.moneromint.solo.utils.CircularBuffer;
import com.moneromint.solo.utils.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class GlobalStats {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalStats.class);

    private final Supplier<Integer> connectionCount;
    private final CircularBuffer<ImmutablePair<Instant, BigInteger>> recentShares;
    private long blocksFound;
    private long validShares;
    private long invalidShares;
    private BigInteger totalHashes;

    public GlobalStats(Supplier<Integer> connectionCount) {
        this.connectionCount = connectionCount;
        recentShares = new CircularBuffer<>(60);
        totalHashes = BigInteger.ZERO;
    }

    public BigInteger estimateHashrate() {
        if (validShares <= 1) {
            return BigInteger.ZERO;
        }

        var oldest = Instant.MAX;
        BigInteger count = BigInteger.ZERO;

        for (int i = 0; i < recentShares.getSize(); i++) {
            final var share = recentShares.get(i);

            if (share.getLhs().compareTo(oldest) < 0) {
                oldest = share.getLhs();
            }

            count = count.add(share.getRhs());
        }

        long timespent = Duration.between(oldest, Instant.now()).getSeconds();

        return count.divide(BigInteger.valueOf(timespent));
    }

    public void addBlock() {
        blocksFound++;
    }

    public void addValidShare(Difficulty difficulty) {
        recentShares.add(new ImmutablePair<>(Instant.now(), difficulty.getDifficulty()));
        validShares++;
        totalHashes = totalHashes.add(difficulty.getDifficulty());
    }

    public void addInvalidShare() {
        invalidShares++;
    }

    public long getBlocksFound() {
        return blocksFound;
    }

    public int getConnectionCount() {
        return connectionCount.get();
    }

    public long getValidShares() {
        return validShares;
    }

    public long getInvalidShares() {
        return invalidShares;
    }

    public BigInteger getTotalHashes() {
        return totalHashes;
    }

    public void print() {
        LOGGER.info("{} connections | {} H/s | {} valid and {} invalid shares",
                connectionCount.get(),
                estimateHashrate(),
                validShares,
                invalidShares);
    }
}
