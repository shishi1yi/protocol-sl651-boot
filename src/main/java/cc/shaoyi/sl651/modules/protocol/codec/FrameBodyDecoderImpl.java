package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.enums.TransferProtocolTypeEnum;
import cc.shaoyi.sl651.common.utils.FrameM124Util;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyPropertiesMessage;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;

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


    // 发送时间正则 yyyyMMddHHmmss：匹配 19xx或20xx年, 01-12月, 01-31日, 00-23时, 00-59分, 00-59秒
    private static final String REGEX_SEND_TIME = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])([01]\\d|2[0-3])([0-5]\\d)([0-5]\\d)$";

    // 观测时间正则 yyyyMMddHHmm：匹配 19xx或20xx年, 01-12月, 01-31日, 00-23时, 00-59分
    private static final String REGEX_OBSERVE_TIME = "^(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])([01]\\d|2[0-3])([0-5]\\d)$";

    /**
     * 处理M3类型正文帧消息
     */
    @Override
    public HexFrameBodyMessage decodeM3Body(char[] bodyFrame) {
        // TODO: 2022/12/7 请求应答模式
        return null;
    }


    /**
     * 处理M1\M2\M4类型正文帧消息
     */
    @Override
    public HexFrameBodyMessage decodeM124Body(char[] bodyFrame, FrameCommandCodeEnum commandCodeEnum, String rawCode) {
        if (commandCodeEnum == null) {
            return null;
        }

        switch (commandCodeEnum) {
            case REGULAR_REPORT:
            case OVERTIME_REPORT:
            case TEST_REPORT:
            case CURRENT_REPORT:
                return decodeM124BodyByRegularReport(bodyFrame);
            case HEART_BEAT:
                return decodeM124BodyByHeartBeat(bodyFrame);
            case HOUR_REPORT:
                return decodeM124BodyByHourReport(bodyFrame);
            case IMG_REPORT:
                return decodeM124BodyByIMGReport(bodyFrame);
            case BASE_PARAM:
            case REVISE_BASE_PARAM:
                // 在这里判断企业传输条件
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.PRI_COMPANY) {
                    return decodeM124BodyByBaseParamCompanyReport(bodyFrame);
                }
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.STANDARD) {
                    // 按照标准方式去走
                    return decodeM124BodyByBaseParam(bodyFrame);
                }
            case RUNNING_PARAM:
            case REVISE_RUNNING_PARAM:
                // 在这里判断企业传输条件
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.PRI_COMPANY) {
                    return decodeM124BodyByRunningParamCompanyReport(bodyFrame);
                }
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.STANDARD) {
                    // 按照标准方式去走
                    return decodeM124BodyByRunningParamReport(bodyFrame);
                }
            case SOFTWARE_VERSION:
                return decodeM124BodyBySoftwareVersion(bodyFrame);
            case PRI_COMMAND:
                // 在这里判断湖南传输条件
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.HUNAN) {
                    // 【注意】这里不能写死 ""，需要传入真实的原始码，比如 "e3" 或 "e4"
                    FrameCommandCodeEnum.PRI_HUNAN priHunan = FrameCommandCodeEnum.PRI_HUNAN.getFrameFuncEnum(rawCode);

                    // 必须判空，否则下面 switch 会抛 NullPointerException
                    if (priHunan == null) {
                        return null;
                    }

                    switch (priHunan) {
                        case HUNAN_IMG_REPORT_E4:
                        case HUNAN_IMG_REPORT_E3:
                            return decodeM124BodyByIMGHunanReport(bodyFrame);
                        default:
                            return null;
                    }
                }
                // 在这里判断企业传输条件
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.PRI_COMPANY) {
                    FrameCommandCodeEnum.PRI_COMPANY priCompany = FrameCommandCodeEnum.PRI_COMPANY.getFrameFuncEnum(rawCode);
                    if (priCompany == null) {
                        return null;
                    }

                    if (priCompany == FrameCommandCodeEnum.PRI_COMPANY.COMP_CUSTOM_E3) {
                        return decodeM124BodyByCustomE3CompanyReport(bodyFrame);
                    }
                    if (priCompany == FrameCommandCodeEnum.PRI_COMPANY.COMP_CUSTOM_E0) {
                        return decodeM124BodyByRemoteRestart(bodyFrame);
                    }
                    if (priCompany == FrameCommandCodeEnum.PRI_COMPANY.COMP_CUSTOM_E2) {
                        return decodeM124BodyByRemoteWakeup(bodyFrame);
                    }
                    return null;
                }
                return null;
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

        // 使用精准正则进行第一层拦截，直接过滤掉 41月、38时 这种离谱数据
        boolean validSend = StringUtils.hasText(bodySendTimeStr) && bodySendTimeStr.matches(REGEX_SEND_TIME);
        boolean validObserve = StringUtils.hasText(bodyObserveTimeStr) && bodyObserveTimeStr.matches(REGEX_OBSERVE_TIME);


        // 是否处理发送时间
        if (validSend) {
            try {
                // Try-catch 作为第二层拦截（防止正则漏掉的逻辑错误，比如 2月30日）
                DateTime bodySendTime = DateUtil.parse(bodySendTimeStr, "yyyyMMddHHmmss");
                bodyMessage.setSendTime(bodySendTimeStr)
                        .setSendTimeShow(bodySendTime.toString(DatePattern.NORM_DATETIME_PATTERN));
            } catch (Exception e) {
                log.warn("发送时间解析失败, 疑似非法日期: {}", bodySendTimeStr);
            }
        }

        // 是否处理观测时间
        if (validObserve) {
            try {
                DateTime bodyObserveTime = DateUtil.parse(bodyObserveTimeStr, "yyyyMMddHHmm");
                bodyMessage.setObserveTime(bodyObserveTimeStr)
                        .setObserveTimeShow(bodyObserveTime.toString(DatePattern.NORM_DATETIME_MINUTE_PATTERN));
            } catch (Exception e) {
                log.warn("观测时间解析失败, 疑似非法日期: {}", bodyObserveTimeStr);
            }
        }

        return bodyMessage.setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame))
                .setDetectAddress(FrameM124Util.getBodyDetectAddress(bodyFrame))
                .setDetectAddressTypeCode(FrameM124Util.getBodyDetectAddressTypeCode(bodyFrame));
    }

    /**
     * 常规结构上报（通用的单一KV）
     */
    private HexFrameBodyMessage decodeM124BodyByRegularReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        body.setPropertiesMessage(FrameUtil.simpleReportBodyProperties(FrameM124Util.getBodyElementByRegularReport(bodyFrame)));
        return body;
    }

    /**
     * 设备重启
     */
    private HexFrameBodyMessage decodeM124BodyByRemoteRestart(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.simpleReportBodyProperties(bodyElementFrame));
        return body;
    }

    /**
     * 设备召测
     */
    private HexFrameBodyMessage decodeM124BodyByRemoteWakeup(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.simpleReportBodyProperties(bodyElementFrame));
        return body;
    }




    /**
     * 链路维持（心跳包）
     */
    private HexFrameBodyMessage decodeM124BodyByHeartBeat(char[] bodyFrame) {
        HexFrameBodyMessage body = new HexFrameBodyMessage();
        body.setSerialNo(FrameM124Util.getBodySerialNo(bodyFrame));
        body.setSendTime(FrameM124Util.getBodySendTime(bodyFrame));
        return body;
    }


    /**
     * 小时报
     */
    private HexFrameBodyMessage decodeM124BodyByHourReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        /**
         * 根据配置选择对应定义的标识符，解析报文中的设备数据
         */
        char[] bodyElementFrame = FrameM124Util.getBodyElementByRegularReport(bodyFrame);
        if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.HUNAN) {
            body.setPropertiesMessage(FrameUtil.getHourReportHuNanBodyProperties(bodyElementFrame));
        } else {
            body.setPropertiesMessage(FrameUtil.getHourReportBodyProperties(bodyElementFrame));
        }
        return body;
    }


    /**
     * 基础配置参数 (标准协议)
     */
    private HexFrameBodyMessage decodeM124BodyByBaseParam(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.baseParamReportBodyProperties(bodyElementFrame));
        return body;
    }

    /**
     * 基础配置参数 (公司自定义解析规则入口)
     */
    private HexFrameBodyMessage decodeM124BodyByBaseParamCompanyReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.baseParamPriCompanyReportBodyProperties(bodyElementFrame));
        return body;
    }

    /**
     * 运行配置参数
     */
    private HexFrameBodyMessage decodeM124BodyByRunningParamReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.runParamReportBodyProperties(bodyElementFrame));
        return body;
    }


    /**
     * 运行配置参数(公司自定义解析规则入口)
     */
    private HexFrameBodyMessage decodeM124BodyByRunningParamCompanyReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.runParamPriCompanyReportBodyProperties(bodyElementFrame));
        return body;
    }

    /**
     * 软件版本
     */
    private HexFrameBodyMessage decodeM124BodyBySoftwareVersion(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        body.setPropertiesMessage(FrameUtil.getSoftwareVersionBodyPropertiesMessages(bodyElementFrame));
        return body;
    }

    /**
     * 图片报处理 (标准协议)
     */
    private HexFrameBodyMessage decodeM124BodyByIMGReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 截取到观测时间之后的正文
        char[] bodyElementFrame = FrameM124Util.getBodyElementByRegularReport(bodyFrame);
        // 使用标准协议解析
        body.setPropertiesMessage(FrameUtil.getStandardImgBodyPropertiesMessages(bodyElementFrame));
        return body;
    }

    /**
     * 湖南图片报处理 (湖南扩展协议)
     */
    private HexFrameBodyMessage decodeM124BodyByIMGHunanReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 湖南协议图片报(e4、e3)正文部分没有观测时间
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame);
        body.setPropertiesMessage(FrameUtil.getHunanImgBodyPropertiesMessages(bodyElementFrame));
        return body;
    }

    /**
     * 自定义参数报处理 (遥测站上发升级结果——E3企业自定义协议)
     */
    private HexFrameBodyMessage decodeM124BodyByCustomE3CompanyReport(char[] bodyFrame) {
        HexFrameBodyMessage body = buildCommonHexFrameBodyMessage(bodyFrame);
        // 15 是指 流水号+发报时间+遥测地址 的总字节数
        char[] bodyElementFrame = FrameM124Util.getBodyElementReport(bodyFrame, 15);
        LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> companyE3BodyProperties = FrameUtil.getCustomCompanyE3BodyPropertiesMessages(bodyElementFrame);
        body.setPropertiesMessage(companyE3BodyProperties);
        return body;
    }
}
