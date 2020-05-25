package com.moneromint.solo.stratum;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.Miner;
import com.moneromint.solo.ShareProcessor;
import com.moneromint.solo.StatsPrinter;
import com.moneromint.solo.stratum.message.StratumLoginParams;
import com.moneromint.solo.stratum.message.StratumRequest;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.offtopica.monerorpc.daemon.BlockTemplate;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.math.BigInteger;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.moneromint.solo.utils.HexUtils.hexStringToByteArray;
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
        shareProcessor = spy(new ShareProcessor(blockTemplateUpdater, daemon, mock(StatsPrinter.class)));
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

        when(blockTemplateUpdater.getLastBlockTemplate()).thenReturn(new BlockTemplate(
                hexStringToByteArray(
                        "0c0cd1cfaff605a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c0000000002c4ef" +
                                "5901ff88ef5901cee7c5cd8570024b1546daf7db876f0b667f71d551b470837e82fe901db7ff1cdcc002" +
                                "95d100483301b615c5edd8011343b78c24027d50fec05cf7a7f88461a7051a6334e16a2126e602100000" +
                                "00000000000000000000000000000000"),
                hexStringToByteArray(
                        "0c0cd1cfaff605a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c00000000b85f07" +
                                "53843ce95299e2fd753c88e13eec4043a33b3264ed3ec887f7b36a9aa201"),
                BigInteger.valueOf(540600L),
                3849795498958L,
                1472392L,
                null,
                hexStringToByteArray("a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c"),
                128,
                hexStringToByteArray("d8ce0beb10924668542d7b08ed99a9d5e71fe829d6853f38e465715c28cc905e"),
                1470464L
        ));

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
