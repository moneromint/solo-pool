package com.moneromint.solo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import static com.moneromint.solo.TestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ShareProcessorTest {
    ShareProcessor shareProcessor;
    BlockTemplateUpdater blockTemplateUpdater;
    MoneroDaemonRpcClient daemon;
    StatsPrinter statsPrinter;

    @BeforeEach
    void setup() {
        blockTemplateUpdater = mock(BlockTemplateUpdater.class);
        daemon = mock(MoneroDaemonRpcClient.class);
        statsPrinter = mock(StatsPrinter.class);
        shareProcessor = new ShareProcessor(blockTemplateUpdater, daemon, statsPrinter);
    }

    @Test
    void testProcessShareValid() throws Exception {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(5000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        // Submit a valid share. Result not good enough to be a block.
        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1);

        assertEquals(ShareProcessor.ShareStatus.VALID, result);
        assertTrue(job.getResults().contains(RESULT_1));
        verify(miner).addValidShare(job.getDifficulty());
        verify(statsPrinter).addValidShare(job.getDifficulty());
        verify(daemon, never()).submitBlock(any());
    }

    @Test
    void testProcessShareInvalidLowDifficulty() throws Exception {
        var bt = BLOCK_TEMPLATE_1;
        var job = spy(new Job(0L, bt.getBlockTemplateBlob(), new Difficulty(10_000L), bt.getSeedHash(),
                bt.getHeight(), bt));
        var miner = spy(Miner.create("x", "x"));

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        ShareProcessor.ShareStatus result = shareProcessor.processShare(miner, job, RESULT_1, NONCE_1);

        assertEquals(ShareProcessor.ShareStatus.LOW_DIFFICULTY, result);
        verify(miner).addInvalidShare();
        verify(statsPrinter).addInvalidShare();
    }
}
