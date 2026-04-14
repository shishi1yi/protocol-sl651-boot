package cc.shaoyi.sl651.common.utils;

import cn.hutool.core.util.ArrayUtil;

/**
 * 水文协议M1、M2、M4模式报文工具类
 */
public class FrameM124Util {

	/**
	 * 获取报文正文流水号
	 */
	public static Long getBodySerialNo(char[] bodyFrame) {
        return Long.parseLong(HexStringUtil.int2HexStr(bodyFrame, 0, 1), 16);
    }

	/**
	 * 获取报文正文发报时间
	 */
	public static String getBodySendTime(char[] bodyFrame) {
		// 增加长度安全防御：最大需要访问下标 7，所以数组长度至少要 >= 8
		if (ArrayUtil.isEmpty(bodyFrame) || bodyFrame.length < 8) {
			// 报文不完整时返回空字符串，避免越界报错
			return "";
		}

		// 6字节，YYMMDDHHmmSS格式，年份只有两位，需要在前面补两位
		return "20" + HexStringUtil.int2HexStr(bodyFrame, 2, 7);
	}

	/**
	 * 获取报文正文遥测站地址
	 */
    public static String getBodyDetectAddress(char[] bodyFrame) {
        // 跳过2字节标识符，5字节
        return HexStringUtil.int2HexStr(bodyFrame, 10, 14);
    }

	/**
	 * 获取报文正文遥测站分类码
	 */
    public static String getBodyDetectAddressTypeCode(char[] bodyFrame) {
        // 1字节
        return HexStringUtil.int2HexStr(bodyFrame, 15, 15);
    }

	/**
	 * 获取报文正文观测时间
	 */
    public static String getBodyObserveTime(char[] bodyFrame) {
		// 增加长度安全防御：最大需要访问下标 22，所以数组长度至少要 >= 23
		if (ArrayUtil.isEmpty(bodyFrame) || bodyFrame.length < 23) {
			// 如果报文长度不够，返回空字符串或 null，避免程序崩溃
			return "";
		}

		// 跳过2字节标识符
		// 5字节，YYMMDDHHmm格式，没有ss秒。年份只有两位，需要在前面补两位
		return "20" + HexStringUtil.int2HexStr(bodyFrame, 18, 22);
    }

	/**
	 * 获取正文要素内容，跳过23字节数（流水号+发报时间+遥测地址+遥测站分类码+观测时间），这是截止到观测时间之后
	 */
    public static char[] getBodyElementByRegularReport(char[] bodyFrame) {
        char[] bodyElementFrame = new char[bodyFrame.length - 23];
        System.arraycopy(bodyFrame, 23, bodyElementFrame, 0, bodyFrame.length - 23);
        return bodyElementFrame;
    }

	/**
	 * 获取正文要素内容，跳过16个字节数（流水号+发报时间+遥测地址+遥测站分类码），从遥测站类型码后开始
	 */
	public static char[] getBodyElementReport(char[] bodyFrame) {
		char[] bodyElementFrame = new char[bodyFrame.length - 16];
		System.arraycopy(bodyFrame, 16, bodyElementFrame, 0, bodyFrame.length - 16);
		return bodyElementFrame;
	}

	/**
	 * 截取报文正文要素内容
	 * @param bodyFrame 完整的正文字符数组
	 * @param offset    需要跳过的前置字节数量（偏移量）
	 * @return 截取后的要素内容数组
	 */
	public static char[] getBodyElementReport(char[] bodyFrame, int offset) {
		char[] bodyElementFrame = new char[bodyFrame.length - offset];
		// 从 bodyFrame 的 offset 位置开始，复制剩余的所有内容
		System.arraycopy(bodyFrame, offset, bodyElementFrame, 0, bodyFrame.length - offset);
		return bodyElementFrame;
	}

}
