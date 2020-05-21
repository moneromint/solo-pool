package com.moneromint.solo.stratum;

import com.moneromint.solo.*;
import com.moneromint.solo.stratum.message.StratumLoginParams;
import com.moneromint.solo.stratum.message.StratumRequest;
import com.moneromint.solo.stratum.message.StratumSubmitParams;
import com.moneromint.solo.utils.BlockTemplateUtils;
import com.moneromint.solo.utils.HexUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.offtopica.monerorpc.daemon.BlockTemplate;
import uk.offtopica.monerorpc.daemon.MoneroDaemonRpcClient;

import java.util.Map;

public class StratumServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(StratumServerHandler.class);

    private final ChannelGroup activeMiners;
    private final BlockTemplateUpdater blockTemplateUpdater;
    private final MoneroDaemonRpcClient daemon;

    private Miner miner;

    public StratumServerHandler(ChannelGroup activeMiners, BlockTemplateUpdater blockTemplateUpdater,
                                MoneroDaemonRpcClient daemon) {
        this.activeMiners = activeMiners;
        this.blockTemplateUpdater = blockTemplateUpdater;
        this.daemon = daemon;
    }

    private void login(ChannelHandlerContext ctx, StratumRequest<StratumLoginParams> request) {
        LOGGER.trace("login login={} pass={}", request.getParams().getLogin(), request.getParams().getPass());

        miner = Miner.create(request.getParams().getLogin(), request.getParams().getPass());
        final var job = miner.createAndSetJob(blockTemplateUpdater.getLastBlockTemplate());

        // TODO:
        ctx.writeAndFlush(Map.of(
                "id", request.getId(),
                "result", Map.of(
                        "result", "OK",
                        "id", miner.getId().toString(),
                        "job", Map.of(
                                "blob", HexUtils.byteArrayToHexString(job.getBlob()),
                                "job_id", job.getId().toString(),
                                "target", job.getDifficulty().getHex(),
                                "id", miner.getId().toString(),
                                "seed_hash", HexUtils.byteArrayToHexString(job.getSeedHash()),
                                "height", job.getHeight(),
                                "algo", "rx/0"
                        )
                )
        ));

        activeMiners.add(ctx.channel());
    }

    private void submit(ChannelHandlerContext ctx, StratumRequest<StratumSubmitParams> request) {
        // TODO: Check that miner is authenticated.
        // TODO: Extract this logic to a share processor class.
        // TODO: Validate nonce.
        // TODO: Validate result hash.

        final Difficulty shareDifficulty;
        final byte[] nonce;
        try {
            shareDifficulty = Difficulty.ofShare(HexUtils.hexStringToByteArray(request.getParams().getResult()));
            nonce = HexUtils.hexStringToByteArray(request.getParams().getNonce());
        } catch (HexUtils.InvalidHexStringException e) {
            ctx.writeAndFlush(Map.of(
                    "id", request.getId(),
                    "error", Map.of(
                            "code", -1,
                            "message", "Malformed share"
                    )
            ));
            return;
        }

        // If the share difficulty is greater than or equal to the network difficulty...
        if (shareDifficulty.getDifficulty().compareTo(miner.getJob().getBlockTemplate().getDifficulty()) >= 0) {
            LOGGER.info("Found block at height {}", miner.getJob().getHeight());
            final var completedBlob = BlockTemplateUtils.withExtra(miner.getJob().getBlockTemplate(),
                    Main.INSTANCE_ID, miner.getId(), nonce);
            daemon.submitBlock(completedBlob)
                    .thenRun(() -> {
                        LOGGER.trace("Successfully submitted block!");
                        blockTemplateUpdater.update();
                    })
                    .exceptionally(e -> {
                        LOGGER.error("Failed to submit block", e);
                        return null;
                    });
        }

        // If the share difficulty is less than the job difficulty...
        if (shareDifficulty.compareTo(miner.getJob().getDifficulty()) < 0) {
            LOGGER.trace("Invalid share {}", ctx.channel().remoteAddress());
            // TODO: Increment invalid share counter.
            ctx.writeAndFlush(Map.of(
                    "id", request.getId(),
                    "error", Map.of(
                            "code", -1,
                            "message", "Low difficulty share"
                    )
            ));
            return;
        }

        // TODO: Increment valid share counter.

        ctx.writeAndFlush(Map.of(
                "id", request.getId(),
                "result", Map.of(
                        "result", "OK"
                )
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
                        "algo", "rx/0"
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
