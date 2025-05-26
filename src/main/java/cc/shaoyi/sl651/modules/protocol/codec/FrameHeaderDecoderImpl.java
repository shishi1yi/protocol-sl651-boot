package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Shao Yi
 * @description
 * @date 2022年08月03日 13:46
 */
@Slf4j
@Component
public class FrameHeaderDecoderImpl implements FrameHeaderDecoder {


	@Override
	public HexFrameHeaderMessage decodeHexHeader(char[] hexFrame) {
		HexFrameHeaderMessage headerMessage = new HexFrameHeaderMessage();
		headerMessage.setHubAddress(FrameUtil.getHeaderHubAddress(hexFrame))
			.setDetectAddress(FrameUtil.getHeaderDetectAddress(hexFrame))
			.setPassword(FrameUtil.getHeaderPwd(hexFrame))
			.setCommandCode(FrameUtil.getHeaderCommandCode(hexFrame))
			.setBodyLength(FrameUtil.getBodyLen(hexFrame))
			.setM3Mode(FrameUtil.isM3Mode(hexFrame));
		if (headerMessage.isM3Mode()) {
			// TODO 解析m3模式报文头特殊参数
			headerMessage.setFrameCnt(0);
			headerMessage.setFrameSerialNo(null);
		}
		return headerMessage;
	}
}
