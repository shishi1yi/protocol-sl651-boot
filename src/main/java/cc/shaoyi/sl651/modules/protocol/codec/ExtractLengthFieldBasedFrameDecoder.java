package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.utils.LogUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.Locale;

/**
 * @author Shao Yi
 * @description 提取长度字段帧检测器
 * @date 2022年08月02日 14:38
 */
@Slf4j
public class ExtractLengthFieldBasedFrameDecoder extends LengthFieldBasedFrameDecoder {

	private int endFieldLength;

	public ExtractLengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip, int endFieldLength) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
		this.endFieldLength = endFieldLength;
	}


	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		String hexStr = ByteBufUtil.hexDump(in).toLowerCase(Locale.ROOT);
		log.info(">>> 会话通道:{},sl651[解码前,提取长度帧检测器-接收的报文]：{}", ctx.channel(), hexStr);
		try {
			String commandCode = StrUtil.sub(
				StrUtil.subAfter(hexStr, "7e7e", false), // 从关键词后开始
				16,  // 往后偏移16个长度
				18  // 偏移16后再取2个字符（16 + 2 = 18）
			);
			if (!"2f".equalsIgnoreCase(commandCode)) {
				LogUtil.logDLFrameMessage("sl651接收的报文", hexStr);
			}
		} catch (Exception e) {
			log.error("logback打印错误", e);
		}
		return super.decode(ctx, in);
	}

	/**
	 * 获取未调节的帧长度
	 * @param buf
	 * @param offset
	 * @param length
	 * @param order
	 * @return
	 */
	@Override
	protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
		String hexStr = ByteBufUtil.hexDump(buf).toLowerCase(Locale.ROOT);
		log.info(">>> sl651[提取长度帧检测器-接收的报文]：{}", hexStr);
		long unadjustedFrameLength = super.getUnadjustedFrameLength(buf, offset, length, order);
		// 转成成二进制
		String binStr = Integer.toBinaryString((int) unadjustedFrameLength);
		// 取后十二位 二进制
		String binStrLength = StrUtil.subSufByLength(binStr, 12);
		long bodyLength = Integer.valueOf(binStrLength, 2) + endFieldLength;
		log.info(">>> sl651正文长度：{}, 收到的报文：{}", bodyLength, hexStr);
		return bodyLength;
	}
}
