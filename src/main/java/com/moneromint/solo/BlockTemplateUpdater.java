package com.moneromint.solo;

import com.moneromint.solo.utils.BlockTemplateUtils;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.MoneroRpcException;
import uk.offtopica.monerorpc.daemon.BlockTemplate;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.util.Arrays;

public class BlockTemplateUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockTemplateUpdater.class);

    private final MoneroDaemonRpcClient daemon;
    private final String wallet;
    private final ChannelGroup activeMiners;
    private BlockTemplate lastBlockTemplate;

    public BlockTemplateUpdater(MoneroDaemonRpcClient daemon, String wallet, ChannelGroup activeMiners) {
        this.daemon = daemon;
        this.wallet = wallet;
        this.activeMiners = activeMiners;
    }

    public void update() {
        LOGGER.trace("Attempting to fetch new block template");

        daemon.getBlockTemplate(wallet, BlockTemplateUtils.RESERVE_SIZE).thenAccept(candidate -> {
            synchronized (this) {
                if (lastBlockTemplate == null || !Arrays.equals(lastBlockTemplate.getPrevHash(),
                        candidate.getPrevHash())) {
                    LOGGER.debug("New block template at height {}", candidate.getHeight());
                    lastBlockTemplate = candidate;
                    final var evt = new NewBlockTemplateEvent(lastBlockTemplate);
                    activeMiners.forEach(c -> c.pipeline().fireUserEventTriggered(evt));
                }
            }
        }).exceptionally(e -> {
            if (e instanceof MoneroRpcException && e.getMessage().contains("Core is busy")) {
                LOGGER.info("Failed to fetch block template; daemon busy");
            } else {
                LOGGER.error("Failed to fetch block template", e);
            }
            return null;
        });
    }

    public BlockTemplate getLastBlockTemplate() {
        return lastBlockTemplate;
    }
}
