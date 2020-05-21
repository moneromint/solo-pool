package com.moneromint.solo.utils;

import uk.offtopica.monerocore.BlockHashingBlobCreator;
import uk.offtopica.monerorpc.daemon.BlockTemplate;

import static com.moneromint.solo.utils.ByteArrayUtils.longToByteArray;

public class BlockTemplateUtils {
    // 8 bytes for instance id + 8 bytes for miner id
    public static final int RESERVE_SIZE = 16;

    private static final BlockHashingBlobCreator bhbc = new BlockHashingBlobCreator();

    public static byte[] getHashingBlob(BlockTemplate blockTemplate, Long instanceId, Long minerId) {
        return bhbc.getHashingBlob(withExtra(blockTemplate, instanceId, minerId, new byte[]{}));
    }

    public static byte[] getHashingBlob(BlockTemplate blockTemplate, Long instanceId, Long minerId, byte[] nonce) {
        return bhbc.getHashingBlob(withExtra(blockTemplate, instanceId, minerId, nonce));
    }

    public static byte[] withExtra(BlockTemplate blockTemplate, Long instanceId, Long minerId, byte[] nonce) {
        byte[] template = blockTemplate.getBlockTemplateBlob().clone();
        byte[] instanceIdBytes = longToByteArray(instanceId);

        System.arraycopy(nonce, 0, template, 39, Math.min(nonce.length, 4));
        System.arraycopy(instanceIdBytes, 0, template, blockTemplate.getReservedOffset(),
                instanceIdBytes.length);
        byte[] minerIdBytes = longToByteArray(minerId);
        System.arraycopy(minerIdBytes, 0, template, blockTemplate.getReservedOffset() + instanceIdBytes.length,
                minerIdBytes.length);

        return template;
    }
}
