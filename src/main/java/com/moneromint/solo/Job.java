package com.moneromint.solo;

import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Job {
    private final Set<byte[]> results;
    private Long id;
    private byte[] blob;
    private Difficulty difficulty;
    private byte[] seedHash;
    private Long height;
    private BlockTemplate blockTemplate;

    public Job(Long id, byte[] blob, Difficulty difficulty, byte[] seedHash, Long height, BlockTemplate blockTemplate) {
        results = new HashSet<>();
        this.id = id;
        this.blob = blob;
        this.difficulty = difficulty;
        this.seedHash = seedHash;
        this.height = height;
        this.blockTemplate = blockTemplate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getBlob() {
        return blob;
    }

    public void setBlob(byte[] blob) {
        this.blob = blob;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public byte[] getSeedHash() {
        return seedHash;
    }

    public void setSeedHash(byte[] seedHash) {
        this.seedHash = seedHash;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public BlockTemplate getBlockTemplate() {
        return blockTemplate;
    }

    public void setBlockTemplate(BlockTemplate blockTemplate) {
        this.blockTemplate = blockTemplate;
    }

    public Set<byte[]> getResults() {
        return results;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(id, job.id) &&
                Arrays.equals(blob, job.blob) &&
                Objects.equals(difficulty, job.difficulty) &&
                Arrays.equals(seedHash, job.seedHash) &&
                Objects.equals(height, job.height) &&
                Objects.equals(blockTemplate, job.blockTemplate) &&
                Objects.equals(results, job.results);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, difficulty, height, blockTemplate);
        result = 31 * result + Arrays.hashCode(blob);
        result = 31 * result + Arrays.hashCode(seedHash);
        return result;
    }
}
