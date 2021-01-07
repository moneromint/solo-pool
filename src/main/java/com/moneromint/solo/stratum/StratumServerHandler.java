package com.moneromint.solo.stratum;

import com.moneromint.solo.BlockTemplateUpdater;
import com.moneromint.solo.Miner;
import com.moneromint.solo.NewBlockTemplateEvent;
import com.moneromint.solo.ShareProcessor;
import com.moneromint.solo.stratum.message.StratumLoginParams;
import com.moneromint.solo.stratum.message.StratumRequest;
import com.moneromint.solo.stratum.message.StratumSubmitParams;
import com.moneromint.solo.utils.HexUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.util.Map;

public class StratumServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumServerHandler.class);

    private final ChannelGroup activeMiners;
    private final BlockTemplateUpdater blockTemplateUpdater;
    private final ShareProcessor shareProcessor;
    private final String algo;

    private Miner miner;

    public StratumServerHandler(ChannelGroup activeMiners, BlockTemplateUpdater blockTemplateUpdater,
                                ShareProcessor shareProcessor, String algo) {
        this.activeMiners = activeMiners;
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.shareProcessor = shareProcessor;
        this.algo = algo;
    }

    private void login(ChannelHandlerContext ctx, StratumRequest<StratumLoginParams> request) {
        if (miner != null) {
            sendError(ctx, request.getId(), "Already authenticated");
            return;
        }

        LOGGER.trace("login login={} pass={} agent={}",
                request.getParams().getLogin(),
                request.getParams().getPass(),
                request.getParams().getAgent());

        if (request.getParams().getAgent() != null
                && request.getParams().getAgent().contains("xmr-node-proxy")) {
            sendError(ctx, request.getId(), "xmr-node-proxy is not supported by this pool");
            ctx.close();
            return;
        }

        miner = Miner.create(request.getParams().getLogin(), request.getParams().getPass());
        final var job = miner.createAndSetJob(blockTemplateUpdater.getLastBlockTemplate());

        sendResponse(ctx, request.getId(), Map.of(
                "result", "OK",
                "id", miner.getId().toString(),
                "job", Map.of(
                        "blob", HexUtils.byteArrayToHexString(job.getBlob()),
                        "job_id", job.getId().toString(),
                        "target", job.getDifficulty().getHex(),
                        "id", miner.getId().toString(),
                        "seed_hash", HexUtils.byteArrayToHexString(job.getSeedHash()),
                        "height", job.getHeight(),
                        "algo", algo
                )));

        activeMiners.add(ctx.channel());
    }

    private void submit(ChannelHandlerContext ctx, StratumRequest<StratumSubmitParams> request) {
        if (miner == null) {
            sendError(ctx, request.getId(), "Unauthenticated");
            return;
        }

        final String jobId = request.getParams().getJobId();
        final byte[] result;
        final byte[] nonce;
        try {
            result = HexUtils.hexStringToByteArray(request.getParams().getResult());
            nonce = HexUtils.hexStringToByteArray(request.getParams().getNonce());
        } catch (HexUtils.InvalidHexStringException e) {
            sendError(ctx, request.getId(), "Malformed share");
            return;
        }

        var status = shareProcessor.processShare(miner, miner.getJob(), result, nonce, jobId);

        if (status != ShareProcessor.ShareStatus.VALID) {
            sendError(ctx, request.getId(), status.toString());
            return;
        }

        sendResponse(ctx, request.getId(), Map.of(
                "result", "OK"
        ));
    }

    private void newJob(ChannelHandlerContext ctx, BlockTemplate blockTemplate) {
        final var job = miner.createAndSetJob(blockTemplateUpdater.getLastBlockTemplate());

        ctx.writeAndFlush(Map.of(
                "method", "job",
                "params", Map.of(
                        "blob", HexUtils.byteArrayToHexString(job.getBlob()),
                        "job_id", job.getId().toString(),
                        "target", job.getDifficulty().getHex(),
                        "id", miner.getId().toString(),
                        "seed_hash", HexUtils.byteArrayToHexString(job.getSeedHash()),
                        "height", job.getHeight(),
                        "algo", algo
                )
        ));
    }

    private void sendResponse(ChannelHandlerContext ctx, Object id, Map<String, Object> result) {
        ctx.writeAndFlush(Map.of(
                "id", id,
                "result", result
        ));
    }

    private void sendError(ChannelHandlerContext ctx, Object id, String message) {
        ctx.writeAndFlush(Map.of(
                "id", id,
                "error", Map.of(
                        "code", -1,
                        "message", message
                )
        ));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.trace("New connection from {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.trace("Connection closed {}", ctx.channel().remoteAddress());
        activeMiners.remove(ctx.channel());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof StratumRequest)) {
            LOGGER.warn("Unknown message type. Expected StratumRequest, got {}", msg.getClass().getSimpleName());
            return;
        }

        switch (((StratumRequest<?>) msg).getMethod()) {
            case LOGIN -> login(ctx, (StratumRequest<StratumLoginParams>) msg);
            case SUBMIT -> submit(ctx, (StratumRequest<StratumSubmitParams>) msg);
            default -> throw new IllegalArgumentException("Unexpected value: " + ((StratumRequest<?>) msg).getMethod());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            LOGGER.debug("Disconnecting {} for being idle", ctx.channel().remoteAddress());
            ctx.close();
        } else if (evt instanceof NewBlockTemplateEvent) {
            newJob(ctx, ((NewBlockTemplateEvent) evt).getBlockTemplate());
        } else {
            LOGGER.warn("Unknown event {}", evt.getClass().getSimpleName());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof TooLongFrameException) {
            ctx.close();
            LOGGER.warn("Flood detected from {}", ctx.channel().remoteAddress());
            return;
        }

        LOGGER.error("Unhandled exception", cause);
        ctx.close();
    }
}
