package com.moneromint.solo;

import com.moneromint.solo.utils.BlockTemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

public class ShareProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShareProcessor.class);

    private final BlockTemplateUpdater blockTemplateUpdater;
    private final MoneroDaemonRpcClient daemon;

    public ShareProcessor(BlockTemplateUpdater blockTemplateUpdater, MoneroDaemonRpcClient daemon) {
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.daemon = daemon;
    }

    public ShareStatus processShare(Miner miner, Job job, byte[] result, byte[] nonce) {
        if (result.length != 32 || nonce.length != 4) {
            return ShareStatus.INVALID;
        }

        // TODO: Validate result hash.

        final Difficulty shareDifficulty = Difficulty.ofShare(result);

        // If the share difficulty is greater than or equal to the network difficulty...
        if (shareDifficulty.getDifficulty().compareTo(miner.getJob().getBlockTemplate().getDifficulty()) >= 0) {
            LOGGER.info("Found block at height {}", miner.getJob().getHeight());
            final var completedBlob = BlockTemplateUtils.withExtra(miner.getJob().getBlockTemplate(),
                    Main.INSTANCE_ID, miner.getId(), nonce);
            daemon.submitBlock(completedBlob)
                    .thenRun(() -> {
                        LOGGER.trace("Successfully submitted block!");
                        blockTemplateUpdater.update();
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Failed to submit block", e);
                        return null;
                    });
        }

        if (!job.getResults().add(result)) {
            return ShareStatus.DUPLICATE_RESULT;
        }

        // If the share difficulty is less than the job difficulty...
        if (shareDifficulty.compareTo(miner.getJob().getDifficulty()) < 0) {
            return ShareStatus.LOW_DIFFICULTY;
        }

        // TODO: Increment valid share counter.

        return ShareStatus.VALID;
    }

    public enum ShareStatus {
        /**
         * The provided result hash does not match.
         */
        BAD_HASH,

        /**
         * This result hash has already been sent to the pool.
         */
        DUPLICATE_RESULT,

        /**
         * Failed to process the share for some reason, e.g. bad nonce or result length.
         */
        INVALID,

        /**
         * The provided share's difficulty was lower than the job difficulty.
         */
        LOW_DIFFICULTY,

        /**
         * The share was valid.
         */
        VALID,
    }
}
