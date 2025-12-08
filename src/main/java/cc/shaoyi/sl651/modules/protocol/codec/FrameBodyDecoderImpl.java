package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.utils.FrameM124Util;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
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

        switch (commandCodeEnum) {
            case REGULAR_REPORT:
            case OVERTIME_REPORT:
            case TEST_REPORT:
            case CURRENT_REPORT: {
                return decodeM124BodyByRegularReport(bodyFrame);
            }
            case HEART_BEAT: {
                return decodeM124BodyByHeartBeat(bodyFrame);
            }
            case HOUR_REPORT: {
                return decodeM124BodyByHourReport(bodyFrame);
            }
            case IMG_REPORT: {
                return decodeM124BodyByIMGReport(bodyFrame);
            }
            case IMG_REPORT_E3:
            case IMG_REPORT_E4: {
                // 在这里判断湖南传输条件
                if (sl651NettyContentProperties.getHunanTransfer()) {
                    return decodeM124BodyByIMGHunanReport(bodyFrame);
                }
                // 不满足条件，返回null
                return null;
            }
            default:
                return null;
        }
    }


    /**
     * 组装通用部分数据内容
     */
    private static HexFrameBodyMessage buildCommonHexFrameBodyMessage(char[] bodyFrame) {
        HexFrameBodyMessage bodyMessage = new HexFrameBodyMessage();

        String bodySendTimeStr = FrameM124Util.getBodySendTime(bodyFrame);
        String bodyObserveTimeStr = FrameM124Util.getBodyObserveTime(bodyFrame);

        boolean validSend = bodySendTimeStr.matches("\\d{14}");
        boolean validObserve = bodyObserveTimeStr.matches("\\d{12}");
        // 是否处理发送时间
        if (validSend) {
            DateTime bodySendTime = DateUtil.parse(bodySendTimeStr, "yyyyMMddHHmmss");
            bodyMessage.setSendTime(bodySendTimeStr)
                    .setSendTimeShow(bodySendTime.toString(DatePattern.NORM_DATETIME_PATTERN));
        }
        // 是否处理观测时间
        if (validObserve) {
            DateTime bodyObserveTime = DateUtil.parse(bodyObserveTimeStr, "yyyyMMddHHmm");
            bodyMessage.setObserveTime(bodyObserveTimeStr)
                    .setObserveTimeShow(bodyObserveTime.toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN));
        }
        return bodyMessage.setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame))
                .setDetectAddress(FrameM124Util.getBodyDetectAddress(bodyFrame))
                .setDetectAddressTypeCode(FrameM124Util.getBodyDetectAddressTypeCode(bodyFrame));
    }

    /**
     * 定时上报
     * @param bodyFrame
     * @return
     */
    private HexFrameBodyMessage decodeM124BodyByRegularReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        body.setPropertiesMessage(FrameUtil.periodReportBodyProperties(FrameM124Util.getBodyElementByRegularReport(bodyFrame)));
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
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        /**
         * 根据配置选择对应定义的标识符，解析报文中的设备数据
         */
        char[] bodyElementFrame = FrameM124Util.getBodyElementByRegularReport(bodyFrame);
        if (sl651NettyContentProperties.getHunanTransfer()) {
            body.setPropertiesMessage(FrameUtil.getHourReportHuNanBodyProperties(bodyElementFrame));
        } else {
            body.setPropertiesMessage(FrameUtil.hourReportBodyProperties(bodyElementFrame));
        }
        return body;
    }

    /**
     * 图片报处理 (标准协议)
     */
    private HexFrameBodyMessage decodeM124BodyByIMGReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 截取到观测时间之后的正文
        char[] elementByRegularReport = FrameM124Util.getBodyElementByRegularReport(bodyFrame);
        // 使用标准协议解析
        body.setPropertiesMessage(FrameUtil.getStandardImgBodyPropertiesMessages(elementByRegularReport));
        return body;
    }

    /**
     * 湖南图片报处理 (湖南扩展协议)
     */
    private HexFrameBodyMessage decodeM124BodyByIMGHunanReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 湖南协议图片报(e4、e3)正文部分没有观测时间
        char[] imgHunanRegularReport = FrameM124Util.getBodyElementReport(bodyFrame);
        body.setPropertiesMessage(FrameUtil.getHunanImgBodyPropertiesMessages(imgHunanRegularReport));
        return body;
    }
}
