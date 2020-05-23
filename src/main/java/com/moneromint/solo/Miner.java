package com.moneromint.solo;

import com.moneromint.solo.utils.BlockTemplateUtils;
import com.moneromint.solo.utils.CircularBuffer;
import com.moneromint.solo.utils.ImmutablePair;
import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class Miner {
    // TODO: Make this configurable.
    private static final Difficulty MINIMUM_DIFFICULTY = new Difficulty(200L);
    private static final Difficulty STARTING_DIFFICULTY = new Difficulty(5000L);
    private static final BigInteger SHARE_TARGET_TIME = BigInteger.valueOf(15L);
    private static final int VARDIFF_WINDOW = 10;

    private final Long id;
    private final String username;
    private final String password;
    private final Instant connectedAt;
    private final CircularBuffer<ImmutablePair<Instant, BigInteger>> recentShares;
    private Job job;
    private BigInteger validHashes;
    private long validShares;
    private long invalidShares;

    public Miner(Long id, String username, String password, Instant connectedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.connectedAt = connectedAt;
        recentShares = new CircularBuffer<>(VARDIFF_WINDOW);
        validHashes = BigInteger.ZERO;
    }

    public static Miner create(String username, String password) {
        return new Miner(ThreadLocalRandom.current().nextLong(), username, password, Instant.now());
    }

    private Difficulty getNextJobDifficulty() {
        if (validShares < 6) {
            return STARTING_DIFFICULTY;
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

        // count hashes performed in timespent seconds
        //  => hashrate = count / timespent

        var difficulty = new Difficulty(count.divide(BigInteger.valueOf(timespent)).multiply(SHARE_TARGET_TIME));

        if (difficulty.compareTo(MINIMUM_DIFFICULTY) < 0) {
            return MINIMUM_DIFFICULTY;
        }
        return difficulty;
    }

    public Job createAndSetJob(BlockTemplate blockTemplate) {
        job = new Job(
                ThreadLocalRandom.current().nextLong(),
                BlockTemplateUtils.getHashingBlob(blockTemplate, Main.INSTANCE_ID, id),
                getNextJobDifficulty(),
                blockTemplate.getSeedHash(),
                blockTemplate.getHeight(),
                blockTemplate
        );
        return job;
    }

    public void addValidShare(Difficulty difficulty) {
        recentShares.add(new ImmutablePair<>(Instant.now(), difficulty.getDifficulty()));
        validHashes = validHashes.add(difficulty.getDifficulty());
        validShares++;
    }

    public void addInvalidShare() {
        invalidShares++;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }
}
