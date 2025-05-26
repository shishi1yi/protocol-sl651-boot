package cc.shaoyi.sl651.common.utils;

/**
 * 水文协议M1、M2、M4模式报文工具类
 * @author shaoyi
 * @date 2022/7/19 16:52
 */
public class FrameM124Util {

	/**
	 * 获取报文正文流水号
	 * @param bodyFrame
	 * @return
	 */
	public static Long getBodySerialNo(char[] bodyFrame) {
        return Long.parseLong(HexStringUtil.int2HexStr(bodyFrame, 0, 1), 16);
    }

	/**
	 * 获取报文正文发报时间
	 * @param bodyFrame
	 * @return
	 */
    public static String getBodySendTime(char[] bodyFrame) {
        // 6字节，YYMMDDHHmmSS格式，年份只有两位，需要在前面补两位
        return "20" + HexStringUtil.int2HexStr(bodyFrame, 2, 7);
    }

	/**
	 * 获取报文正文遥测站地址
	 * @param bodyFrame
	 * @return
	 */
    public static String getBodyDetectAddress(char[] bodyFrame) {
        // 跳过2字节标识符，5字节
        return HexStringUtil.int2HexStr(bodyFrame, 10, 14);
    }

	/**
	 * 获取报文正文遥测站分类码
	 * @param bodyFrame
	 * @return
	 */
    public static String getBodyDetectAddressTypeCode(char[] bodyFrame) {
        // 1字节
        return HexStringUtil.int2HexStr(bodyFrame, 15, 15);
    }

	/**
	 * 获取报文正文观测时间
	 * @param bodyFrame
	 * @return
	 */
    public static String getBodyObserveTime(char[] bodyFrame) {
        // 跳过2字节标识符
        // 5字节，YYMMDDHHmm格式，没有ss秒。年份只有两位，需要在前面补两位
        return "20" + HexStringUtil.int2HexStr(bodyFrame, 18, 22);
    }

    public static char[] getBodyElementByRegularReport(char[] bodyFrame) {
        char[] bodyElementFrame = new char[bodyFrame.length - 23];
        System.arraycopy(bodyFrame, 23, bodyElementFrame, 0, bodyFrame.length - 23);
        return bodyElementFrame;
    }

}
