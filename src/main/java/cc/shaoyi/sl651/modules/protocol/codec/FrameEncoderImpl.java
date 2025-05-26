package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.constant.CommonConstant;
import cc.shaoyi.sl651.common.utils.Crc16Util;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author Shao Yi
 * @description 帧消息编码器
 * @date 2022年08月03日 19:02
 */
@Slf4j
@Component
public class FrameEncoderImpl implements FrameEncoder {


	/**
	 * @description 帧消息编码
	 * @param frameWrapper
	 * @return
	 */
	@Override
	public ByteBuf encodeReply(HexFrameWrapper frameWrapper) {
		if (frameWrapper.getMessage().getHeader().isM3Mode()) {
			return encodeM3Reply(frameWrapper);
		} else {
			return encodeM24Reply(frameWrapper);
		}
	}

	/**
	 * @description 编码M3报文
	 * @param frameWrapper
	 * @return
	 */
	private ByteBuf encodeM3Reply(HexFrameWrapper frameWrapper) {
		// TODO
		return null;
	}


	/**
	 * @description 编码M24报文
	 * @param frameWrapper
	 * @return
	 */
	private ByteBuf encodeM24Reply(HexFrameWrapper frameWrapper) {
		try {
			HexFrameHeaderMessage headerMessage = frameWrapper.getMessage().getHeader();
			StringBuilder stringBuilder = new StringBuilder();
			// 正文
			String replyBody = Opt.of(headerMessage)
				.filter(_headerMessage -> !StrUtil.equalsIgnoreCase(_headerMessage.getCommandCode(), CommonConstant.HEARTBEAT_COMMAND_CODE))
				.map(_headerMessage -> frameWrapper.getMessage().getBody())
				.map(body -> StrUtil.padPre(Integer.toHexString(body.getSerialNo().intValue()), 4, '0') + DateUtil.format(DateUtil.date(), "yyMMddHHmmss"))
				.orElseGet(() -> "");
			// 报文下行标志和报文长度
			String length = Opt.ofBlankAble(replyBody)
				.map(_replyBody -> CommonConstant.DOWN_TAG + StrUtil.padPre(Integer.toBinaryString(NumberUtil.ceilDiv(_replyBody.length(), 2) ), 12, '0'))
				.map(binaryStr -> StrUtil.padPre(Integer.toHexString(Integer.parseInt(binaryStr, 2)), 4, '0'))
				.orElseGet(() -> CommonConstant.HEADER_RSP_DOWN_SYMBOL_AND_ZERO_LEN_HEX);
			// 没正文的不用返回
			if (StrUtil.isNotBlank(replyBody)) {
				stringBuilder.append(CommonConstant.HEADER_START_HEX) // 帧起始符
					.append(headerMessage.getDetectAddress()) // 遥测站地址
					.append(headerMessage.getHubAddress()) // 中心站地址
					.append(headerMessage.getPassword()) // 密码
					.append(headerMessage.getCommandCode())
					.append(length) // 报文下行标志和报文长度
					.append(CommonConstant.HEADER_RSP_BODY_END_STX_HEX) // 报文体起始符
					.append(replyBody) // 正文
					.append(CommonConstant.HEADER_RSP_BODY_END_EOT_HEX); // 报文体结束符
				stringBuilder.append(Crc16Util.crc16(Hex.decodeHex(stringBuilder.toString()), false).toUpperCase()); // CRC校验码
				log.info("编码M24报文帧消息：{}", stringBuilder);
				String hexStr = stringBuilder.toString();
				LogUtil.logULFrameMessage("sl651回应的报文", hexStr);
				ByteBuf byteBuf = Unpooled.buffer();
				byteBuf.writeBytes(HexUtil.decodeHex(hexStr));
				return Unpooled.copiedBuffer(byteBuf);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}


	@Override
	public ByteBuf encodeAsk(HexFrameHeaderMessage headerMessage) {
		if (Objects.nonNull(headerMessage)) {
			try {
				// 流水号，2个字节 1-65535
				String serialNo = StrUtil.toString(RandomUtil.randomInt(1, 65536));
				// 正文
				String askBody = StrUtil.padPre(new BigInteger(serialNo, 10).toString(16), 4, '0') + DateUtil.format(DateUtil.date(), "yyMMddHHmmss");
				// 报文长度(bit位数)
				String bitLengthStr = StrUtil.toString(NumberUtil.ceilDiv(askBody.length(), 2));
				// 报文下行标志和报文长度
				String length = StrUtil.padPre(
					new BigInteger(
						CommonConstant.DOWN_TAG +
						StrUtil.padPre(new BigInteger(bitLengthStr, 10).toString(2), 12, "0"), 2
					).toString(16)
					, 4
					, '0'
				);
				StringBuilder stringBuilder = new StringBuilder().append(CommonConstant.HEADER_START_HEX) // 帧起始符
					.append(headerMessage.getDetectAddress()) // 遥测站地址
					.append(headerMessage.getHubAddress()) // 中心站地址
					.append(headerMessage.getPassword()) // 密码
					.append("37")
					.append(length) // 报文下行标志和报文长度
					.append(CommonConstant.HEADER_RSP_BODY_END_STX_HEX) // 报文体起始符
					.append(askBody) // 正文
					.append(CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX); // 报文体结束符
				stringBuilder.append(Crc16Util.crc16(Hex.decodeHex(stringBuilder.toString()), false).toUpperCase()); // CRC校验码
				log.info("查询实时数据报文帧下行帧消息：{}", stringBuilder);
				ByteBuf byteBuf = Unpooled.buffer();
				byteBuf.writeBytes(HexUtil.decodeHex(stringBuilder.toString()));
				return Unpooled.copiedBuffer(byteBuf);
			} catch (DecoderException e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}



}
