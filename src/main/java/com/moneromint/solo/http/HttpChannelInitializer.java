package com.moneromint.solo.http;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.GlobalStats;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.codec.http.HttpServerKeepAliveHandler;

public class HttpChannelInitializer<T extends SocketChannel> extends ChannelInitializer<T> {
    private final BlockTemplateUpdater blockTemplateUpdater;
    private final GlobalStats globalStats;

    public HttpChannelInitializer(BlockTemplateUpdater blockTemplateUpdater, GlobalStats globalStats) {
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.globalStats = globalStats;
    }

    @Override
    protected void initChannel(T ch) throws Exception {
        final var p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new HttpServerExpectContinueHandler());
        p.addLast(new HttpServerKeepAliveHandler());
        p.addLast(new HttpServerHandler(blockTemplateUpdater, globalStats));
    }
}
