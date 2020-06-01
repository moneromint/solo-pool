package com.moneromint.solo;

import com.moneromint.solo.utils.HexUtils;
import uk.offtopica.monerorpc.daemon.BlockTemplate;

import java.math.BigInteger;

import static com.moneromint.solo.utils.HexUtils.hexStringToByteArray;

public class TestData {
    /**
     * A nice testnet block template.
     */
    public static final BlockTemplate BLOCK_TEMPLATE_1;

    /**
     * A submission for BLOCK_TEMPLATE_1. Difficulty 9278.
     */
    public static final byte[] RESULT_1;

    /**
     * Nonce to go with RESULT_1.
     */
    public static final byte[] NONCE_1;

    /**
     * A testnet block template that comes after BLOCK_TEMPLATE_2.
     */
    public static final BlockTemplate BLOCK_TEMPLATE_2;

    static {
        try {
            BLOCK_TEMPLATE_1 = new BlockTemplate(
                    hexStringToByteArray(
                            "0c0cd1cfaff605a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c0000000002c4ef" +
                                    "5901ff88ef5901cee7c5cd8570024b1546daf7db876f0b667f71d551b470837e82fe901db7ff1cdcc002" +
                                    "95d100483301b615c5edd8011343b78c24027d50fec05cf7a7f88461a7051a6334e16a2126e602100000" +
                                    "00000000000000000000000000000000"),
                    hexStringToByteArray(
                            "0c0cd1cfaff605a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c00000000b85f07" +
                                    "53843ce95299e2fd753c88e13eec4043a33b3264ed3ec887f7b36a9aa201"),
                    BigInteger.valueOf(540600L),
                    3849795498958L,
                    1472392L,
                    null,
                    hexStringToByteArray("a1ff51bc50c7abc5080e4486d030f481d3a0dcf92bb18a4f7631263c6d46793c"),
                    128,
                    hexStringToByteArray("d8ce0beb10924668542d7b08ed99a9d5e71fe829d6853f38e465715c28cc905e"),
                    1470464L
            );
            RESULT_1 = hexStringToByteArray("a5c845cbf40ab5cd8793a134a6d81d99cec82dbf0d83f96edde2293e1b100700");
            NONCE_1 = hexStringToByteArray("f0030000");

            BLOCK_TEMPLATE_2 = new BlockTemplate(
                    hexStringToByteArray(
                            "0c0cc7b2d3f605513e1a73ea30b746f0aaadc1443c12487a2a7f4b0fcaf830d75845434af262f00000000002c38e" +
                                    "5a01ff878e5a019dbe82c0996f027e3679ba508cad4d8de8345d55f35e3c08b4180bb591d6536963a02a" +
                                    "101153c63301ffc87f9a77dede4391a77663a002fcd34a4e8d78b64fbb96ffe76b3b825e409d02100000" +
                                    "00000000000000000000000000000000"),
                    hexStringToByteArray(
                            "0c0cc7b2d3f605513e1a73ea30b746f0aaadc1443c12487a2a7f4b0fcaf830d75845434af262f000000000601473" +
                                    "d2b34569f3b4d1dd55323eb1cfc2b9642490dc9e58c7a8bb474cb09b6101"),
                    BigInteger.valueOf(96775L),
                    3820776103709L,
                    1476359L,
                    null,
                    hexStringToByteArray("513e1a73ea30b746f0aaadc1443c12487a2a7f4b0fcaf830d75845434af262f0"),
                    128,
                    hexStringToByteArray("0303917418a151c5f92429b89c3dd5f6a02c9072659d3b530c1ec2ec9aa55825"),
                    1474560L
            );
        } catch (HexUtils.InvalidHexStringException e) {
            throw new RuntimeException(e);
        }
    }
}
