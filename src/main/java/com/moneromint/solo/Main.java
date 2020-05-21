package com.moneromint.solo;

import com.moneromint.solo.stratum.StratumChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    // TODO:
    public static final Long INSTANCE_ID = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        // TODO: Configuration value.
        final int threads = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Using {} threads", threads);

        final EventLoopGroup parentGroup;
        final EventLoopGroup childGroup;

        if (Epoll.isAvailable()) {
            parentGroup = new EpollEventLoopGroup(1);
            childGroup = new EpollEventLoopGroup(threads);
        } else {
            LOGGER.warn("Native transport not available; falling back to NIO");
            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(threads);
        }

        // TODO: GlobalEventExecutor not suitable.
        final var activeMiners = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        final var daemon = new MoneroDaemonRpcClient(URI.create("http://node.xmr.to:38081/json_rpc"));

        // TODO: Configuration value.
        final String wallet =
                "59McWTPGc745SRWrSMoh8oTjoXoQq6sPUgKZ66dQWXuKFQ2q19h9gvhJNZcFTizcnT12r63NFgHiGd6gBCjabzmzHAMoyD6";

        final var blockTemplateUpdater = new BlockTemplateUpdater(daemon, wallet, activeMiners);
        final var updateScheduler = Executors.newScheduledThreadPool(1);
        updateScheduler.scheduleAtFixedRate(blockTemplateUpdater::update, 0, 10, TimeUnit.SECONDS);

        final var bootstrap = new ServerBootstrap();
        bootstrap.group(parentGroup, childGroup);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.channel(EpollServerSocketChannel.class);
        bootstrap.childHandler(new StratumChannelInitializer<>(activeMiners, blockTemplateUpdater, daemon));
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        // TODO: Configuration value.
        final int port = 3333;
        LOGGER.info("Binding on *:{}", port);
        bootstrap.bind(port).channel().closeFuture().sync();
    }
}
