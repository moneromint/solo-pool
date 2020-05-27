package com.moneromint.solo.http;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.GlobalStats;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);

    private final BlockTemplateUpdater blockTemplateUpdater;
    private final GlobalStats globalStats;

    public HttpServerHandler(BlockTemplateUpdater blockTemplateUpdater, GlobalStats globalStats) {
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.globalStats = globalStats;
    }

    private HttpResponse handleFetchTemplate(HttpRequest request) {
        blockTemplateUpdater.update();
        return new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
    }

    private HttpResponse handleMetrics(HttpRequest request) {
        String response =
                // Write connection count.
                "# HELP stratum_connections_count The total number of connections to the stratum server.\n" +
                        "# TYPE stratum_connections_count gauge\n" +
                        "stratum_connections_count " +
                        globalStats.getConnectionCount() +
                        "\n\n" +

                        // Write share count.
                        "# HELP stratum_shares_total The total number of shares submitted to the server.\n" +
                        "# TYPE stratum_shares_total counter\n" +
                        "stratum_shares_total{valid=\"true\"} " +
                        globalStats.getValidShares() +
                        '\n' +
                        "stratum_shares_total{valid=\"false\"} " +
                        globalStats.getInvalidShares() +
                        "\n\n" +

                        // Write total hashes.
                        // It is better to send the number of hashes and letting the database work out the hashrate,
                        // instead
                        // of the pool working out the hashrate and sending it.
                        "# HELP stratum_hashes_total The total number of hashes submitted to the server.\n" +
                        "# TYPE stratum_hashes_total counter\n" +
                        "stratum_hashes_total " +
                        globalStats.getTotalHashes().toString(10) +
                        '\n';
        final var buf = Unpooled.wrappedBuffer(response.getBytes(CharsetUtil.UTF_8));

        return new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, buf);
    }

    private HttpResponse handleRequest(HttpRequest request) {
        if (request.method() != HttpMethod.GET) {
            return new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        return switch (request.uri()) {
            case "/-/fetchtemplate" -> handleFetchTemplate(request);
            case "/metrics" -> handleMetrics(request);
            default -> new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_FOUND);
        };
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpRequest)) {
            return;
        }

        var request = (HttpRequest) msg;

        HttpResponse response = handleRequest(request);

        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warn("Unhandled exception", cause);
        ctx.close();
    }
}
