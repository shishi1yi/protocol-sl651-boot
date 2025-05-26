package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.common.utils.HexStringUtil;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Shao Yi
 * @description 16进制帧解码器
 * @date 2022年08月03日 14:00
 */
@Slf4j
public class HexFrameDecoder extends ByteToMessageDecoder {

	private final FrameHeaderDecoder frameHeaderDecoder;

	private final FrameBodyDecoder frameBodyDecoder;

	public HexFrameDecoder(FrameHeaderDecoder frameHeaderDecoder, FrameBodyDecoder frameBodyDecoder) {
		this.frameHeaderDecoder = frameHeaderDecoder;
		this.frameBodyDecoder = frameBodyDecoder;
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		HexFrameWrapper hexFrameWrapper = this.decodeFrame(in, ctx.channel());
		out.add(hexFrameWrapper);
	}



	private HexFrameWrapper decodeFrame(ByteBuf byteBuf, Channel channel) {
		HexFrameWrapper wrapper = new HexFrameWrapper();
		HexFrameMessage frameMessage = new HexFrameMessage();
		wrapper.setMessage(frameMessage);
		String frameHexStr = ByteBufUtil.hexDump(byteBuf);
		try {
			wrapper.setRemoteAddress(channel.remoteAddress().toString())
				.setLocalAddress(channel.localAddress().toString());
			log.info("会话通道:{}, sl651-2014, hex帧消息报文 {}", channel, frameHexStr);
			wrapper.setOriginalFrame(frameHexStr);
			char[] frame = HexStringUtil.hexStr2CharArray(frameHexStr);
			if (FrameUtil.verifyCRC16Code(frameHexStr)) {
				log.info("CRC16校验成功");
				// 解析帧头
				HexFrameHeaderMessage headerMessage = frameHeaderDecoder.decodeHexHeader(frame);
				frameMessage.setHeader(headerMessage);
				int bodyLength = headerMessage.getBodyLength().intValue();
				if (headerMessage.isM3Mode()) {
					frameMessage.setBody(frameBodyDecoder.decodeM3Body(FrameUtil.getM3Body(frame, bodyLength)));
				} else {
					frameMessage.setBody(frameBodyDecoder.decodeM124Body(FrameUtil.getM124Body(frame, bodyLength),
						FrameCommandCodeEnum.getFrameFuncEnum(headerMessage.getCommandCode())));
				}
				frameMessage.setBodyEndFrameMark(FrameUtil.getBodyEndSymbol(frame));
				frameMessage.setCrcCode(FrameUtil.getCRC16Code(frame));
				wrapper.setSuccess(true);
				if (!"2f".equalsIgnoreCase(wrapper.getMessage().getHeader().getCommandCode())) {
					LogUtil.logJsonMessage("sl651报文解析成功",wrapper.getMessage().getHeader().getDetectAddress(), wrapper);
				}
				log.info("sl651-2014解析成功，解析结果 {}", JSONUtil.toJsonStr(wrapper));
			} else {
				wrapper.setSuccess(false);
				String gatewayCode = StrUtil.sub(
					StrUtil.subAfter(frameHexStr, "7e7e", false), // 从关键词后开始
					2,  // 往后偏移2个长度
					12  // 偏移2后再取10个字符（2 + 10 = 12）
				);
				LogUtil.logJsonMessage("sl651报文CRC16校验失败",gatewayCode, wrapper);
				log.info("CRC16校验失败, 解析结果 {}", JSONUtil.toJsonStr(wrapper));
			}
		} catch (Exception e) {
			log.error("会话通道:{}, sl651-2014 hex帧消息编码错误, hex帧消息报文 {}", channel, ByteBufUtil.hexDump(byteBuf), e);
			wrapper.setSuccess(false);
			String gatewayCode = StrUtil.sub(
				StrUtil.subAfter(frameHexStr, "7e7e", false), // 从关键词后开始
				2,  // 往后偏移2个长度
				12  // 偏移2后再取10个字符（2 + 10 = 12）
			);
			LogUtil.logJsonMessage("sl651报文hex帧消息编码错误",gatewayCode, wrapper);
		} finally {
			byteBuf.skipBytes(byteBuf.readableBytes());
		}
		return wrapper;
	}

}
