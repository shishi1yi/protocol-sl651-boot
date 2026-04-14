package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.constant.CommonConstant;
import cc.shaoyi.sl651.common.enums.ParamDataTypeEnum;
import cc.shaoyi.sl651.common.utils.Crc16Util;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cc.shaoyi.sl651.modules.protocol.entity.ParamItem;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Shao Yi
 * @description 帧消息编码器
 * @date 2022年08月03日 19:02
 */
@Slf4j
@Component
public class FrameEncoderImpl implements FrameEncoder {

    /**
     * 帧消息编码
     */
    @Override
    public ByteBuf encodeReply(HexFrameWrapper frameWrapper) {
        if (frameWrapper.getMessage().getHeader().isM3Mode()) {
            return encodeM3Reply(frameWrapper);
        }
        return encodeM24Reply(frameWrapper);
    }

    /**
     * 编码M3报文
     */
    private ByteBuf encodeM3Reply(HexFrameWrapper frameWrapper) {
        // TODO
        return null;
    }

    /**
     * 编码M24报文
     */
    private ByteBuf encodeM24Reply(HexFrameWrapper frameWrapper) {
        try {
            HexFrameHeaderMessage header = frameWrapper.getMessage().getHeader();

            // 心跳报文不需要应答
            if (StrUtil.equalsIgnoreCase(header.getCommandCode(), CommonConstant.HEARTBEAT_COMMAND_CODE)) {
                return null;
            }

            // 构建报文正文
            String replyBody = buildBodyWithSerialNo(frameWrapper.getMessage().getBody());
            if (StrUtil.isBlank(replyBody)) {
                return null;
            }

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(header, replyBody, CommonConstant.HEADER_RSP_BODY_END_EOT_HEX);

            LogUtil.logULFrameMessage("sl651回应的报文", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码M24报文失败", e);
            return null;
        }
    }


    @Override
    public ByteBuf encodeAsk37(Integer serialNo, HexFrameHeaderMessage headerMessage) {
        if (headerMessage == null) {
            return null;
        }

        try {
            String askBody = buildBodyWithSerialNo(serialNo);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    askBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "37"  // 查询实时数据命令码
            );

            log.info("查询实时数据报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码查询报文失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeAskE0(Integer serialNo, HexFrameHeaderMessage headerMessage) {
        if (headerMessage == null) {
            return null;
        }

        try {
            String askBody = buildBodyWithSerialNo(serialNo);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    askBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "E0"  // 远程重启
            );

            log.info("远程重启报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码远程重启报文失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeAskE2(Integer serialNo, HexFrameHeaderMessage headerMessage) {
        if (headerMessage == null) {
            return null;
        }

        try {
            String askBody = buildBodyWithSerialNo(serialNo);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    askBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "E2"  // 远程召测
            );

            log.info("召测报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码召测报文失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeAsk45(Integer serialNo, HexFrameHeaderMessage headerMessage) {
        if (headerMessage == null) {
            return null;
        }

        try {
            String askBody = buildBodyWithSerialNo(serialNo);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    askBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "45"  // 查询当前软件版本
            );

            log.info("查询当前软件版本报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码查询当前软件版本报文失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeUpgrade(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons) {
        if (headerMessage == null) {
            return null;
        }
        try {

            // 添加遥测地址（02 + 探测地址）
            String obj = "02" + headerMessage.getDetectAddress();

            // 构建报文参数
            Map<String, ParamItem> params = buildParamItemMap(paramJsons);

            // 构建报文体（流水号 + 时间戳 + 遥测地址 + 参数）
            String upgradeBody = buildBodyWithParam(serialNo, obj, params);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    upgradeBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "E1"  // 自定义命令码（固件下发）
            );

            log.info("[下发固件]报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码[下发固件]失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeCommand41(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons) {
        if (headerMessage == null) {
            return null;
        }
        try {

            List<String> reqs = paramJsons.stream().map(paramJson -> {
                return paramJson.getStr("paramIdentifier");
            }).filter(Objects::nonNull).collect(Collectors.toList());

            // 构建报文体（流水号 + 时间戳 + 请求参数）
            String upgradeBody = buildBodyWithReq(serialNo, "", reqs);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    upgradeBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "41"  // 自定义命令码（查询基础配置）
            );

            log.info("[下发查询基础配置]报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码[下发查询基础配置]失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeCommand40(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons) {
        if (headerMessage == null) {
            return null;
        }
        try {

            // 构建报文参数
            Map<String, ParamItem> params = buildParamItemMap(paramJsons);

            // 构建报文体（流水号 + 时间戳  + 参数）
            String upgradeBody = buildBodyWithParam(serialNo, "", params);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    upgradeBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "40"  // 自定义命令码（修改基础参数）
            );

            log.info("[下发修改基础参数]报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码[下发修改基础参数]失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeCommand42(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons) {
        if (headerMessage == null) {
            return null;
        }
        try {

            // 构建报文参数
            Map<String, ParamItem> params = buildParamItemMap(paramJsons);

            // 构建报文体（流水号 + 时间戳  + 参数）
            String upgradeBody = buildBodyWithParam(serialNo, "", params);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    upgradeBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "42"  // 自定义命令码（修改运行参数）
            );

            log.info("[下发修改运行参数]报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码[下发修改运行参数]失败", e);
            return null;
        }
    }

    @Override
    public ByteBuf encodeCommand43(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons) {
        if (headerMessage == null) {
            return null;
        }
        try {

            List<String> reqs = paramJsons.stream().map(paramJson -> {
                return paramJson.getStr("paramIdentifier");
            }).filter(Objects::nonNull).collect(Collectors.toList());

            // 构建报文体（流水号 + 时间戳 + 请求参数）
            String upgradeBody = buildBodyWithReq(serialNo, "", reqs);

            // 组装并返回完整报文
            String completeMessage = buildCompleteMessage(
                    headerMessage,
                    upgradeBody,
                    CommonConstant.HEADER_RSP_BODY_END_ENQ_HEX,
                    "43"  // 自定义命令码（查询运行参数配置）
            );

            log.info("[下发查询运行参数配置]报文帧下行帧消息：{}", completeMessage);

            return createByteBuf(completeMessage);

        } catch (Exception e) {
            log.error("编码[下发查询运行参数配置]失败", e);
            return null;
        }
    }


    /**
     * 构建报文参数
     */
    private static Map<String, ParamItem> buildParamItemMap(List<JSONObject> paramJsons) {
        // 构建报文体（根据实际需求解析 paramJsons 生成对应的参数字符串）
        Map<String, ParamItem> params = Maps.newLinkedHashMap();
        // 解析 paramJsons，假设每个 JSONObject 包含 "paramIdentifier" 和 "paramValue" 字段
        for (JSONObject paramJson : paramJsons) {
            ParamDataTypeEnum dataType = ParamDataTypeEnum.getType(paramJson.getStr("paramDataType"));
            params.put(
                    paramJson.getStr("paramIdentifier"),
                    new ParamItem(
                            paramJson.getStr("paramValue"),
                            dataType
                    )
            );
        }
        return params;
    }

    /**
     * 构建带流水号的报文体（流水号 + 时间戳）
     */
    private String buildBodyWithSerialNo(HexFrameBodyMessage body) {
        if (body == null || body.getSerialNo() == null) {
            return "";
        }
        return buildBodyWithSerialNo(body.getSerialNo().intValue());
    }

    /**
     * 构建带流水号的报文体（流水号 + 时间戳）
     */
    private String buildBodyWithSerialNo(int serialNo) {
        String serialNoHex = StrUtil.padPre(Integer.toHexString(serialNo), 4, '0');
        String timestamp = DateUtil.format(DateUtil.date(), "yyMMddHHmmss");
        return serialNoHex + timestamp;
    }

    /**
     * 构建自定义参数的报文体
     * * 报文结构：流水号(4字节) + 时间戳(12字节) + 对象标识 + [参数项标识 + 数据定义字节 + 参数内容]...
     * * @param serialNo 流水号
     * @param obj      对象标识符
     * @param params   参数集合 (Key: 参数标识, Value: 参数项具体内容与类型)
     * @return 组装完成的完整 16 进制报文体字符串
     */
    private String buildBodyWithParam(int serialNo, String obj, Map<String, ParamItem> params) {
        // 1. 组装报文头部基础信息：流水号补齐4位 + yyMMddHHmmss 格式时间戳
        String serialNoHex = StrUtil.padPre(Integer.toHexString(serialNo), 4, '0');
        String timestamp = DateUtil.format(DateUtil.date(), "yyMMddHHmmss");

        // 2. 遍历参数集合，依次将各个参数组装为 16 进制字符串
        String paramStr = params.entrySet().stream().map(entry -> {
            String identifier = entry.getKey();
            ParamItem item = entry.getValue();

            // 容错处理：若参数项或内容为空，默认下发全 F 异常值 (长度 08)
            if (item == null || item.getContent() == null) {
                return identifier + "08" + "FF";
            }

            String content = item.getContent();
            ParamDataTypeEnum paramDataType = item.getType();

            // ==========================================
            // 核心一：基于正则表达式进行数据形态的精准分类
            // ==========================================

            // 1. 特殊数值 (Special Number)
            // 匹配逻辑：包含小数点的数字(正负均可，如 3.14, -15.2) 或 纯负整数(如 -100)
            // 作用：这些数字需要提取小数位，且负数需要被转换为特定的 FF/FF0 协议前缀
            boolean isSpecialNumber = content.matches("^-?\\d+\\.\\d+$|^-\\d+$");

            // 2. 原生16进制或正整数 (Raw Hex or Positive Integer)
            // 匹配逻辑：完全由 0-9 且/或 a-f/A-F 组成的字符串 (如 00004030a0, 12345)
            // 作用：此类数据已经是底层可直接识别的格式，不需要转码，仅需校验奇偶长度补齐即可
            boolean isRawHexOrPosInt = content.matches("^[0-9a-fA-F]+$");

            // 3. 强文本类型 (Text)
            // 匹配逻辑：显式指定为 STRING 类型，或者在 AUTO 模式下不属于上述两种数字/原生报文的格式
            // 作用：如 "admin", "V1.0.0", "http://..."，必须被转义为 ASCII Hex
            boolean isTextType = (paramDataType == ParamDataTypeEnum.STRING) ||
                    (paramDataType == ParamDataTypeEnum.AUTO && !isSpecialNumber && !isRawHexOrPosInt);

            // ==========================================
            // 核心二：不同数据形态的独立组装分支
            // ==========================================
            if (isTextType) {
                // 【分支A：文本处理】
                // 场景：包含字母且不属于合法纯16进制的数据 (如版本号 V1.0, 网址等)
                // 动作：按 UTF-8 转换为 ASCII 对应的 16 进制字符串
                content = HexUtil.encodeHexStr(content, StandardCharsets.UTF_8);

                int byteLength = content.length() / 2;
                String lengthHex = calculateDataDefByte(byteLength, 0); // 纯文本无小数位概念

                return identifier + lengthHex + content;

            } else if (isSpecialNumber) {
                // 【分支B：特殊数字处理】
                // 场景：合法的小数(如 12.5) 或负数(如 -20)
                int decimalPlaces = 0;

                // B.1 提取小数位并去除小数点
                if (content.contains(".")) {
                    int dotIndex = content.indexOf(".");
                    decimalPlaces = content.length() - 1 - dotIndex;
                    content = content.replace(".", "");
                }

                // B.2 符号处理与长度补齐
                if (content.startsWith("-")) {
                    content = content.replace("-", ""); // 先去掉负号
                    // 协议规定：负数前置标识符。如果去掉负号后是奇数位则补 FF0，偶数位则补 FF
                    if (content.length() % 2 != 0) {
                        content = "FF0" + content;
                    } else {
                        content = "FF" + content;
                    }
                } else {
                    // 正小数 (如 3.14 -> 314)，若为奇数位，直接在最前面补 0
                    if (content.length() % 2 != 0) {
                        content = "0" + content;
                    }
                }

                // B.3 计算最终占用的字节数并生成数据定义字节
                int byteLength = content.length() / 2;
                String lengthHex = calculateDataDefByte(byteLength, decimalPlaces);

                return identifier + lengthHex + content;

            } else {
                // 【分支C：原生报文或普通正整数】
                // 场景：纯正整数(如 1234) 或 本身就是 Hex 的原生报文(如 00004030a0)
                // 动作：无需任何转换和符号处理，直接当做报文内容，仅做最基础的奇数位补 0 规整
                if (content.length() % 2 != 0) {
                    content = "0" + content;
                }

                int byteLength = content.length() / 2;
                String lengthHex = calculateDataDefByte(byteLength, 0); // 原生报文与整数无小数位

                return identifier + lengthHex + content;
            }
        }).reduce((a, b) -> a + b).orElse(""); // 将所有参数拼成一个长字符串

        // 3. 组装并返回最终报文：流水号 + 时间戳 + 对象标识 + 参数集报文
        return serialNoHex + timestamp + obj + paramStr;
    }

    /**
     * 构建自定义请求参数的报文体
     */
    private String buildBodyWithReq(int serialNo, String obj, List<String> reqs) {
        String serialNoHex = StrUtil.padPre(Integer.toHexString(serialNo), 4, '0');
        String timestamp = DateUtil.format(DateUtil.date(), "yyMMddHHmmss");

        String reqStr = reqs.stream().reduce((a, b) -> a + b).orElse("");

        return serialNoHex + timestamp + obj + reqStr;
    }

    /**
     * 根据 SL651 协议 表26 计算 HEX/BCD 编码的“数据定义”字节（低位字节）
     * 规则：高5位为数据字节数，低3位为小数点后位数
     *
     * @param byteLength    数据包含的字节数 (范围 0~31)
     * @param decimalPlaces 小数点后的位数 (范围 0~7)
     * @return 2位 16进制格式的字符串 (例如 "12")
     */
    private String calculateDataDefByte(int byteLength, int decimalPlaces) {
        // 限制最大范围，防止位移溢出覆盖其他位（防御性编程）
        int safeByteLength = Math.min(byteLength, 31);
        int safeDecimalPlaces = Math.min(decimalPlaces, 7);

        // 字节数左移 3 位 (至高 5 位)，与小数位数 (低 3 位) 进行按位或运算
        int dataDefByte = (safeByteLength << 3) | (safeDecimalPlaces & 0x07);

        return StrUtil.padPre(Integer.toHexString(dataDefByte), 2, '0').toUpperCase();
    }

    /**
     * 构建完整报文（使用原命令码）
     */
    private String buildCompleteMessage(HexFrameHeaderMessage header, String body, String endSymbol) throws DecoderException {
        return buildCompleteMessage(header, body, endSymbol, header.getCommandCode());
    }

    /**
     * 构建完整报文（指定命令码）
     */
    private String buildCompleteMessage(HexFrameHeaderMessage header, String body, String endSymbol, String commandCode) throws DecoderException {
        String lengthField = calculateLengthField(body);
        String hexMessage = buildHexFrame(header, commandCode, lengthField, body, endSymbol);
        return appendCrc(hexMessage);
    }

    /**
     * 组装报文帧（不含CRC）
     */
    private String buildHexFrame(HexFrameHeaderMessage header, String commandCode,
                                 String lengthField, String body, String endSymbol) {
        return CommonConstant.HEADER_START_HEX +
                header.getDetectAddress() +
                header.getHubAddress() +
                header.getPassword() +
                commandCode +
                lengthField +
                CommonConstant.HEADER_RSP_BODY_END_STX_HEX +
                body +
                endSymbol;
    }

    /**
     * 计算报文长度字段（下行标志 + 12位长度）
     */
    private String calculateLengthField(String body) {
        // 1. 计算字节数（Hex 字符串长度除以 2）
        int bodyLength = body.length() / 2;

        // 2. 拼接二进制：方向位(4位) + 长度位(12位)
        // CommonConstant.DOWN_TAG 是 "1000" : 表示中心站下行（控制命令、查询、确认应答）
        String binaryStr = CommonConstant.DOWN_TAG +
                StrUtil.padPre(Integer.toBinaryString(bodyLength), 12, '0');

        // 3. 转回十六进制，并强制补齐 4 位（例如 8008）
        return StrUtil.padPre(Integer.toHexString(Integer.parseInt(binaryStr, 2)), 4, '0');
    }

    /**
     * 添加CRC校验码
     * @throws DecoderException 当十六进制字符串格式错误时抛出
     */
    private String appendCrc(String hexMessage) throws DecoderException {
        // 获取 4 位 CRC 字符串
        String crc = Crc16Util.crc16(Hex.decodeHex(hexMessage), false).toUpperCase();
        // 核心修复：强制补足 4 位，防止出现奇数长度
        crc = StrUtil.padPre(crc, 4, '0');

        return hexMessage + crc;
    }

    /**
     * 创建ByteBuf
     */
    private ByteBuf createByteBuf(String hexStr) {
        byte[] bytes = HexUtil.decodeHex(hexStr);
        return Unpooled.wrappedBuffer(bytes);
    }
}
