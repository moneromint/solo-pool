package com.moneromint.solo;

import com.moneromint.solo.stratum.StratumChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    // TODO:
    public static final Long INSTANCE_ID = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 1) {
            System.err.println("Missing argument for properties path");
            System.exit(1);
        }

        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(args[0])) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO: Configuration value.
        final int threads = Runtime.getRuntime().availableProcessors();
        LOGGER.info("Using {} threads", threads);

        final EventLoopGroup parentGroup;
        final EventLoopGroup childGroup;
        final Class<? extends ServerChannel> channelClass;

        if (Epoll.isAvailable()) {
            parentGroup = new EpollEventLoopGroup(1);
            childGroup = new EpollEventLoopGroup(threads);
            channelClass = EpollServerSocketChannel.class;
        } else {
            LOGGER.warn("Native transport not available; falling back to NIO");
            parentGroup = new NioEventLoopGroup(1);
            childGroup = new NioEventLoopGroup(threads);
            channelClass = NioServerSocketChannel.class;
        }

        // TODO: GlobalEventExecutor not suitable.
        final var activeMiners = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

        final var daemon = new MoneroDaemonRpcClient(URI.create(properties.getProperty("com.moneromint.solo.daemon")));

        final String wallet = properties.getProperty("com.moneromint.solo.wallet");

        final var statsPrinter = new StatsPrinter(activeMiners::size);
        final var blockTemplateUpdater = new BlockTemplateUpdater(daemon, wallet, activeMiners);
        final var updateScheduler = Executors.newScheduledThreadPool(2);
        updateScheduler.scheduleAtFixedRate(blockTemplateUpdater::update, 0, 10, TimeUnit.SECONDS);
        updateScheduler.scheduleAtFixedRate(statsPrinter::print, 60, 60, TimeUnit.SECONDS);

        final var shareProcessor = new ShareProcessor(blockTemplateUpdater, daemon, statsPrinter);

        final var bootstrap = new ServerBootstrap();
        bootstrap.group(parentGroup, childGroup);
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.channel(channelClass);
        bootstrap.childHandler(new StratumChannelInitializer<>(activeMiners, blockTemplateUpdater, shareProcessor));
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);

        final int port = Integer.parseInt(properties.getProperty("com.moneromint.solo.port"));
        LOGGER.info("Binding on *:{}", port);
        bootstrap.bind(port).channel().closeFuture().sync();
    }
}
