package cc.shaoyi.sl651.common.utils;

/**
 * @author shaoyi
 * @date 2022/5/12 15:20
 */
public class Crc16Util {

    /**
     * 一个字节包含位的数量 8
     */
    private static final int BITS_OF_BYTE = 8;

    /**
     * 多项式
     */
    private static final int POLYNOMIAL = 0xA001;

    /**
     * 初始值
     */
    private static final int INITIAL_VALUE = 0xFFFF;

    /**
     * CRC16 编码
     *
     * @param bytes
     * @return 编码结果
     */
    public static String crc16(int[] bytes, boolean isRevertHighLow) {
        int res = INITIAL_VALUE;
        for (int data : bytes) {
            res = res ^ data;
            for (int i = 0; i < BITS_OF_BYTE; i++) {
                res = (res & 0x0001) == 1 ? (res >> 1) ^ POLYNOMIAL : res >> 1;
            }
        }
        return isRevertHighLow ? Integer.toHexString(revert(res)) : Integer.toHexString(res);
    }

    public static String crc16(byte[] bytes, boolean isRevertHighLow) {
        int res = INITIAL_VALUE;
        for (byte bt : bytes) {
            int data = (int)bt & 0x000000ff;
            res = res ^ data;
            for (int i = 0; i < BITS_OF_BYTE; i++) {
                res = (res & 0x0001) == 1 ? (res >> 1) ^ POLYNOMIAL : res >> 1;
            }
        }
        return isRevertHighLow ? Integer.toHexString(revert(res)) : Integer.toHexString(res);

    }

    /**
     * 翻转16位的高八位和低八位字节
     *
     * @param src
     * @return 翻转结果
     */
    private static int revert(int src) {
        int lowByte = (src & 0xFF00) >> 8;
        int highByte = (src & 0x00FF) << 8;
        return lowByte | highByte;
    }

}
