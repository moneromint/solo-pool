package com.moneromint.solo.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.GlobalStats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BlockTemplateUpdater blockTemplateUpdater;
    private final GlobalStats globalStats;

    public HttpServerHandler(BlockTemplateUpdater blockTemplateUpdater, GlobalStats globalStats) {
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.globalStats = globalStats;
    }

    private HttpResponse handleFetchTemplate(HttpRequest request) {
        if (request.method() == HttpMethod.GET) {
            blockTemplateUpdater.update();
        }
        return new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
    }

    private HttpResponse handleMetrics(HttpRequest request) {
        final ByteBuf buf;

        if (request.method() == HttpMethod.GET) {
            final String body =
                    // Write blocks found.
                    "# HELP stratum_blocks_found Total number of blocks found by the pool.\n" +
                            "# TYPE stratum_blocks_found counter\n" +
                            "stratum_blocks_found " +
                            globalStats.getBlocksFound() +
                            "\n\n" +

                            // Write connection count.
                            "# HELP stratum_connections_count The total number of connections to the stratum server" +
                            ".\n" +
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
                            // instead of the pool working out the hashrate and sending it.
                            "# HELP stratum_hashes_total The total number of hashes submitted to the server.\n" +
                            "# TYPE stratum_hashes_total counter\n" +
                            "stratum_hashes_total " +
                            globalStats.getTotalHashes().toString(10) +
                            '\n';

            buf = Unpooled.wrappedBuffer(body.getBytes(CharsetUtil.UTF_8));
        } else {
            buf = Unpooled.buffer(0);
        }

        final var response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, buf);

        response.headers().set("Content-Type", "text/plain; version=0.0.4");

        return response;
    }

    private HttpResponse handleStatsJson(HttpRequest request) throws IOException {
        final ByteBuf buf;

        if (request.method() == HttpMethod.GET) {
            final var content = Map.of(
                    "connections", globalStats.getConnectionCount(),
                    "hashrate", globalStats.estimateHashrate().longValue(),
                    "validShares", globalStats.getValidShares(),
                    "invalidShares", globalStats.getInvalidShares(),
                    "totalHashes", globalStats.getTotalHashes().toString()
            );

            buf = Unpooled.buffer();
            final DataOutput dataOutput = new ByteBufOutputStream(buf);
            MAPPER.writeValue(dataOutput, content);
        } else {
            buf = Unpooled.buffer(0);
        }

        final var response = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, buf);
        response.headers().set("Content-Type", "application/json");
        response.headers().set("Access-Control-Allow-Origin", "*"); // Allow access from any domain

        return response;
    }

    private HttpResponse handleRequest(HttpRequest request) throws IOException {
        if (request.method() != HttpMethod.GET && request.method() != HttpMethod.HEAD) {
            return new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.METHOD_NOT_ALLOWED);
        }

        return switch (request.uri()) {
            case "/-/fetchtemplate" -> handleFetchTemplate(request);
            case "/metrics" -> handleMetrics(request);
            case "/stats.json" -> handleStatsJson(request);
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
