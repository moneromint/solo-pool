package com.moneromint.solo.stratum;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.Miner;
import com.moneromint.solo.ShareProcessor;
import com.moneromint.solo.GlobalStats;
import com.moneromint.solo.stratum.message.StratumLoginParams;
import com.moneromint.solo.stratum.message.StratumRequest;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.moneromint.solo.TestData.BLOCK_TEMPLATE_1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StratumServerHandlerTest {
    EmbeddedChannel chan;
    StratumServerHandler handler;
    ChannelGroup activeMiners;
    BlockTemplateUpdater blockTemplateUpdater;
    ShareProcessor shareProcessor;
    MoneroDaemonRpcClient daemon;
    Supplier<Miner> miner;
    Consumer<Miner> setMiner;

    @BeforeEach
    void setup() throws Exception {
        activeMiners = mock(ChannelGroup.class);
        blockTemplateUpdater = mock(BlockTemplateUpdater.class);
        daemon = mock(MoneroDaemonRpcClient.class);
        shareProcessor = spy(new ShareProcessor(blockTemplateUpdater, daemon, mock(GlobalStats.class)));
        handler = new StratumServerHandler(activeMiners, blockTemplateUpdater, shareProcessor);
        chan = new EmbeddedChannel(handler);

        final var minerField = StratumServerHandler.class.getDeclaredField("miner");
        minerField.setAccessible(true);
        miner = () -> {
            try {
                return (Miner) minerField.get(handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
        setMiner = miner -> {
            try {
                minerField.set(handler, miner);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    void testLoginValid() throws Exception {
        // Should be unauthenticated at the start of the connection.
        assertNull(miner.get());

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(BLOCK_TEMPLATE_1);

        // Miner sends login request...
        chan.writeInbound(new StratumRequest<>("0", new StratumLoginParams("x", "x", "XMRig/1.2.3.4")));

        // Miner should be authenticated now...
        assertNotNull(miner.get());

        // Miner should be added to active miners list...
        verify(activeMiners).add(chan);

        Map<String, Object> response = chan.readOutbound();
        assertTrue(response.containsKey("result"));
        // TODO: Validate response!
    }

    @Test
    void testLoginXnp() {
        // When a miner connects with xmr-node-proxy in their user agent...
        chan.writeInbound(new StratumRequest<>("0", new StratumLoginParams("x", "x", "xmr-node-proxy/0.0.3")));

        // They should get an error message...
        Map<String, Object> response = chan.readOutbound();
        assertTrue(response.containsKey("error"));

        // And the connection should be closed.
        assertFalse(chan.isOpen());
    }

    @Test
    void testDisconnect() {
        chan.close();
        verify(activeMiners).remove(chan);
    }
}
