package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.utils.FrameM124Util;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Shao Yi
 * @description
 * @date 2022年08月03日 17:25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrameBodyDecoderImpl implements FrameBodyDecoder {

	private final Sl651NettyContentProperties sl651NettyContentProperties;

	/**
	 * 处理M3类型正文帧消息
	 * @param bodyFrame
	 * @return
	 */
	@Override
	public HexFrameBodyMessage decodeM3Body(char[] bodyFrame) {
		// TODO: 2022/12/7 请求应答模式
		return null;
	}


	/**
	 * 处理M1\M2\M4类型正文帧消息
	 * @param bodyFrame
	 * @param commandCodeEnum
	 * @return
	 */
	@Override
	public HexFrameBodyMessage decodeM124Body(char[] bodyFrame, FrameCommandCodeEnum commandCodeEnum) {
		if (commandCodeEnum == null) {
			return null;
		}
        return switch (commandCodeEnum) {
            case REGULAR_REPORT, OVERTIME_REPORT, TEST_REPORT, CURRENT_REPORT ->
                    decodeM124BodyByRegularReport(bodyFrame);
            case HEART_BEAT -> decodeM124BodyByHeartBeat(bodyFrame);
            case HOUR_REPORT -> decodeM124BodyByHourReport(bodyFrame);
            default -> null;
        };
	}


	/**
	 * 定时上报
	 * @param bodyFrame
	 * @return
	 */
	private HexFrameBodyMessage decodeM124BodyByRegularReport(char[] bodyFrame) {
		HexFrameBodyMessage body = new HexFrameBodyMessage().setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame))
			.setSendTime(FrameM124Util.getBodySendTime(bodyFrame))
			.setDetectAddress(FrameM124Util.getBodyDetectAddress(bodyFrame))
			.setDetectAddressTypeCode(FrameM124Util.getBodyDetectAddressTypeCode(bodyFrame))
			.setObserveTime(FrameM124Util.getBodyObserveTime(bodyFrame))
			.setPropertiesMessage(FrameUtil.getRegularReportBodyProperties(FrameM124Util.getBodyElementByRegularReport(bodyFrame)));
		return body;
	}

	/**
	 * 链路维持（心跳包）
	 * @param bodyFrame
	 * @return
	 */
	private HexFrameBodyMessage decodeM124BodyByHeartBeat(char[] bodyFrame) {
		HexFrameBodyMessage body = new HexFrameBodyMessage();
		body.setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame));
		body.setSendTime(FrameM124Util.getBodySendTime(bodyFrame));
		return body;
	}


	/**
	 * 小时报
	 * @param bodyFrame
	 * @return
	 */
	private HexFrameBodyMessage decodeM124BodyByHourReport(char[] bodyFrame) {
		HexFrameBodyMessage body = new HexFrameBodyMessage().setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame))
			.setSendTime(FrameM124Util.getBodySendTime(bodyFrame))
			.setDetectAddress(FrameM124Util.getBodyDetectAddress(bodyFrame))
			.setDetectAddressTypeCode(FrameM124Util.getBodyDetectAddressTypeCode(bodyFrame))
			.setObserveTime(FrameM124Util.getBodyObserveTime(bodyFrame));
		/**
		 * 根据配置选择对应定义的标识符，解析报文中的设备数据
		 */
		if (sl651NettyContentProperties.getHunanTransfer()) {
			body.setPropertiesMessage(FrameUtil.getHourReportHuNanBodyProperties(FrameM124Util.getBodyElementByRegularReport(bodyFrame)));
		} else {
			body.setPropertiesMessage(FrameUtil.getHourReportBodyProperties(FrameM124Util.getBodyElementByRegularReport(bodyFrame)));
		}
		return body;
	}
}
