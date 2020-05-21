package com.moneromint.solo.stratum;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.stratum.message.StratumMessageDecoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

public class StratumChannelInitializer<T extends SocketChannel> extends ChannelInitializer<T> {
    // Needs to be quite long because XMRig sends a bunch of garbage about supported algos on login.
    private static final int LINE_MAX = 512;

    private final ChannelGroup activeMiners;
    private final BlockTemplateUpdater blockTemplateUpdater;
    private final MoneroDaemonRpcClient daemon;

    public StratumChannelInitializer(ChannelGroup activeMiners, BlockTemplateUpdater blockTemplateUpdater,
                                     MoneroDaemonRpcClient daemon) {
        this.activeMiners = activeMiners;
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.daemon = daemon;
    }

    @Override
    protected void initChannel(T ch) throws Exception {
        final var p = ch.pipeline();

        p.addLast(new LineBasedFrameDecoder(LINE_MAX, true, true));
        p.addLast(new StratumMessageDecoder());

        p.addLast(new NdjsonMessageEncoder());

        p.addLast(new IdleStateHandler(0, 0, 300));
        p.addLast(new StratumServerHandler(activeMiners, blockTemplateUpdater, daemon));
    }
}
