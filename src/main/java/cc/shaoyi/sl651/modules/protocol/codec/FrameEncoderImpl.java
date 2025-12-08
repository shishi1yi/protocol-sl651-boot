package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.constant.CommonConstant;
import cc.shaoyi.sl651.common.utils.Crc16Util;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.core.date.DateUtil;
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

    /**
     * 编码查询报文
     */
    @Override
    public ByteBuf encodeAsk(HexFrameHeaderMessage headerMessage) {
        if (headerMessage == null) {
            return null;
        }

        try {
            // 生成随机流水号（1-65535）
            int serialNo = RandomUtil.randomInt(1, 65536);
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
        int bodyLength = NumberUtil.ceilDiv(body.length(), 2);
        String binaryStr = CommonConstant.DOWN_TAG +
                StrUtil.padPre(Integer.toBinaryString(bodyLength), 12, '0');
        return StrUtil.padPre(Integer.toHexString(Integer.parseInt(binaryStr, 2)), 4, '0');
    }

    /**
     * 添加CRC校验码
     * @throws DecoderException 当十六进制字符串格式错误时抛出
     */
    private String appendCrc(String hexMessage) throws DecoderException {
        String crc = Crc16Util.crc16(Hex.decodeHex(hexMessage), false).toUpperCase();
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
