package com.moneromint.solo;

import com.moneromint.solo.utils.BlockTemplateUtils;
import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class Miner {
    private final Long id;
    private final String username;
    private final String password;
    private final Instant connectedAt;
    private Job job;

    public Miner(Long id, String username, String password, Instant connectedAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.connectedAt = connectedAt;
    }

    public static Miner create(String username, String password) {
        return new Miner(ThreadLocalRandom.current().nextLong(), username, password, Instant.now());
    }

    public Job createAndSetJob(BlockTemplate blockTemplate) {
        job = new Job(
                ThreadLocalRandom.current().nextLong(),
                BlockTemplateUtils.getHashingBlob(blockTemplate, Main.INSTANCE_ID, id),
                new Difficulty(5000L),
                blockTemplate.getSeedHash(),
                blockTemplate.getHeight(),
                blockTemplate
        );
        return job;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }
}
