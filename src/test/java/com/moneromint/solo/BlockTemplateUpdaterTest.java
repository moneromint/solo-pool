package com.moneromint.solo;

import com.moneromint.solo.utils.BlockTemplateUtils;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import static com.moneromint.solo.TestData.BLOCK_TEMPLATE_1;
import static com.moneromint.solo.TestData.BLOCK_TEMPLATE_2;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class BlockTemplateUpdaterTest {
    static final String WALLET = "WALLET";

    ChannelGroup activeMiners;
    Channel miner;
    MoneroDaemonRpcClient daemon;
    BlockTemplateUpdater blockTemplateUpdater;

    @BeforeEach
    void setup() {
        activeMiners = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        miner = mock(Channel.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        activeMiners.add(miner);
        daemon = mock(MoneroDaemonRpcClient.class);
        blockTemplateUpdater = new BlockTemplateUpdater(daemon, WALLET, activeMiners);
    }

    @Test
    void testInit() {
        assertNull(blockTemplateUpdater.getLastBlockTemplate());
    }

    @Test
    void testUpdateInit() {
        when(daemon.getBlockTemplate(WALLET, BlockTemplateUtils.RESERVE_SIZE))
                .thenReturn(completedFuture(BLOCK_TEMPLATE_1));

        blockTemplateUpdater.update();

        assertEquals(BLOCK_TEMPLATE_1, blockTemplateUpdater.getLastBlockTemplate());
        verify(miner.pipeline()).fireUserEventTriggered(Mockito.eq(new NewBlockTemplateEvent(BLOCK_TEMPLATE_1)));
    }

    @Test
    void testUpdateDuplicate() {
        when(daemon.getBlockTemplate(WALLET, BlockTemplateUtils.RESERVE_SIZE))
                .thenReturn(completedFuture(BLOCK_TEMPLATE_1));

        blockTemplateUpdater.update();
        blockTemplateUpdater.update();

        assertEquals(BLOCK_TEMPLATE_1, blockTemplateUpdater.getLastBlockTemplate());
        // Should still only be called once.
        verify(miner.pipeline()).fireUserEventTriggered(Mockito.eq(new NewBlockTemplateEvent(BLOCK_TEMPLATE_1)));
    }

    @Test
    void testUpdateNew() {
        when(daemon.getBlockTemplate(WALLET, BlockTemplateUtils.RESERVE_SIZE))
                .thenReturn(completedFuture(BLOCK_TEMPLATE_1), completedFuture(BLOCK_TEMPLATE_2));

        blockTemplateUpdater.update();
        blockTemplateUpdater.update();

        assertEquals(BLOCK_TEMPLATE_2, blockTemplateUpdater.getLastBlockTemplate());

        verify(miner.pipeline()).fireUserEventTriggered(Mockito.eq(new NewBlockTemplateEvent(BLOCK_TEMPLATE_1)));
        verify(miner.pipeline()).fireUserEventTriggered(Mockito.eq(new NewBlockTemplateEvent(BLOCK_TEMPLATE_2)));
    }
}
