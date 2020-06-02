package com.moneromint.solo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static com.moneromint.solo.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ShareProcessorTest {
    ShareProcessor shareProcessor;
    BlockTemplateUpdater blockTemplateUpdater;
    MoneroDaemonRpcClient daemon;
    GlobalStats globalStats;

    @BeforeEach
    void setup() {
        blockTemplateUpdater = mock(BlockTemplateUpdater.class);
        daemon = mock(MoneroDaemonRpcClient.class);
        globalStats = mock(GlobalStats.class);
        shareProcessor = new ShareProcessor(blockTemplateUpdater, daemon, globalStats);
    }

    @Test
    void testProcessShareValid() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        // Submit a valid share. Result not good enough to be a block.
        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1,
                job.getId().toString());

        assertEquals(ShareProcessor.ShareStatus.VALID, result);
        assertTrue(job.getResults().contains(RESULT_1));
        verify(miner).addValidShare(job.getDifficulty());
        verify(globalStats).addValidShare(job.getDifficulty());
        verify(daemon, never()).submitBlock(any());
    }

    @Test
    void testProcessShareValidBlock() throws NoSuchFieldException, IllegalAccessException {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        // TODO: Hack to make randomness go away. Will need something equally horrid if Main.INSTANCE_ID becomes random.
        {
            final var id = Miner.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(miner, 0xa10c16e4276b50a6L);
        }

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);
        when(daemon.submitBlock(RESULT_2_BLOB)).thenReturn(CompletableFuture.completedFuture(null));

        // Submit a valid share. Result good enough to be a block.
        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_2, NONCE_2,
                job.getId().toString());
        assertEquals(ShareProcessor.ShareStatus.VALID, result);
        assertTrue(job.getResults().contains(RESULT_2));
        verify(miner).addValidShare(job.getDifficulty());
        verify(globalStats).addValidShare(job.getDifficulty());
        verify(globalStats).addBlock();
        verify(daemon).submitBlock(RESULT_2_BLOB);
    }

    @Test
    void testProcessShareInvalidLowDifficulty() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(10_000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1,
                job.getId().toString());

        assertEquals(ShareProcessor.ShareStatus.LOW_DIFFICULTY, result);
        verify(miner).addInvalidShare();
        verify(globalStats).addInvalidShare();
    }

    @Test
    void testProcessShareInvalidDuplicate() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        shareProcessor.processShare(miner, job, RESULT_1, NONCE_1, job.getId().toString());
        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1,
                job.getId().toString());

        assertEquals(ShareProcessor.ShareStatus.DUPLICATE_RESULT, result);
        verify(miner).addInvalidShare();
        verify(globalStats).addInvalidShare();
    }

    @Test
    void testProcessShareInvalidStale() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(1L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1, "0");

        assertEquals(ShareProcessor.ShareStatus.STALE, result);
        verify(miner).addInvalidShare();
        verify(globalStats).addInvalidShare();
    }

    @Test
    void testProcessShareInvalidResultLength() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job,
                Arrays.copyOfRange(RESULT_1, 0, RESULT_1.length - 1), NONCE_1, job.getId().toString());

        assertEquals(ShareProcessor.ShareStatus.INVALID, result);
        verify(miner).addInvalidShare();
        verify(globalStats).addInvalidShare();
    }

    @Test
    void testProcessShareInvalidNonceLength() {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job,
                RESULT_1, Arrays.copyOfRange(NONCE_1, 0, NONCE_1.length - 1), job.getId().toString());

        assertEquals(ShareProcessor.ShareStatus.INVALID, result);
        verify(miner).addInvalidShare();
        verify(globalStats).addInvalidShare();
    }
}
