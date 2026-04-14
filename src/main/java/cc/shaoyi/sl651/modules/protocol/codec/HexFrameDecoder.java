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


    /**
     * SL651-2014 协议报文解码
     *
     * @param byteBuf Netty 的字节缓冲区，包含接收到的原始字节数据
     * @param channel 当前的 Netty 会话通道
     * @return HexFrameWrapper 包含解析结果和报文对象的包装类
     */
    private HexFrameWrapper decodeFrame(ByteBuf byteBuf, Channel channel) {
        HexFrameWrapper wrapper = new HexFrameWrapper();
        HexFrameMessage frameMessage = new HexFrameMessage();
        wrapper.setMessage(frameMessage);

        // 1. 数据预处理：将 Netty 的 ByteBuf 转换为 十六进制字符串 (HexDump)
        String frameHexStr = ByteBufUtil.hexDump(byteBuf);

        try {
            // 记录通道的远程(设备端)和本地(服务端)IP及端口信息
            wrapper.setRemoteAddress(channel.remoteAddress().toString())
                    .setLocalAddress(channel.localAddress().toString());

            log.info("会话通道:{}, hex帧消息报文 {}", channel, frameHexStr);
            wrapper.setOriginalFrame(frameHexStr);

            // 将 Hex 字符串转换为字符数组，方便后续的按位/按字节截取解析
            char[] frame = HexStringUtil.hexStr2CharArray(frameHexStr);

            // 2. 数据校验：进行 CRC16 校验 (SL651 规约强制要求)
            if (FrameUtil.verifyCRC16Code(frameHexStr)) {

                // ------------------ 校验成功，开始核心解析 ------------------

                // 3. 解析帧头：提取中心站地址、遥测站地址(设备ID)、密码、功能码(CommandCode)等
                HexFrameHeaderMessage headerMessage = frameHeaderDecoder.decodeHexHeader(frame);
                frameMessage.setHeader(headerMessage);

                // 获取报文体的长度，用于后续截取 Body 数据
                int bodyLength = headerMessage.getBodyLength().intValue();

                // 4. 解析报文体：根据 SL651 的工作模式进行分支处理
                if (headerMessage.isM3Mode()) {
                    // M3 模式：通常是查询应答模式或简单的链路维持（心跳），报文体结构相对简单
                    frameMessage.setBody(frameBodyDecoder.decodeM3Body(FrameUtil.getM3Body(frame, bodyLength)));
                } else {
                    // M1, M2, M4 模式：自报、定时上报等业务数据，需要结合具体的功能码（CommandCode）进行深入解析
                    frameMessage.setBody(
                            frameBodyDecoder.decodeM124Body(
                                    FrameUtil.getM124Body(frame, bodyLength),
                                    FrameCommandCodeEnum.getFrameFuncEnum(headerMessage.getCommandCode()), // 根据功能码获取枚举
                                    headerMessage.getCommandCode()
                            )
                    );
                }

                // 5. 补充尾部信息：设置报文结束符和提取到的 CRC 校验码
                frameMessage.setBodyEndFrameMark(FrameUtil.getBodyEndSymbol(frame));
                frameMessage.setCrcCode(FrameUtil.getCRC16Code(frame));
                wrapper.setSuccess(true); // 标记解析成功

                // 6. 成功日志记录
                // 判断是否为 "2f" (在 SL651 中 2F 一般为链路维持/心跳报文)
                if (!"2f".equalsIgnoreCase(wrapper.getMessage().getHeader().getCommandCode())) {
                    // 非心跳报文（业务数据），记录详细的解析日志，方便排查业务数据
                    LogUtil.logJsonMessage("sl651报文解析成功", wrapper.getMessage().getHeader().getDetectAddress(), wrapper);
                } else {
                    // 心跳报文，仅用普通 INFO 级别记录，避免日志刷屏
                    log.info("sl651解析成功，解析结果 {}", JSONUtil.toJsonStr(wrapper));
                }

            } else {
                // ------------------ 校验失败处理 ------------------
                wrapper.setSuccess(false);

                // 提取遥测站地址（GatewayCode/设备ID），用于在日志中知道是哪个设备发来的错包
                // 逻辑：找到帧起始符 "7e7e"，跳过随后的2个字符(通常是中心站地址)，截取后续的10个字符(遥测站地址)
                String gatewayCode = StrUtil.sub(
                        StrUtil.subAfter(frameHexStr, "7e7e", false),
                        2,  // 开始索引
                        12  // 结束索引（截取长度为 12 - 2 = 10）
                );
                LogUtil.logJsonMessage("sl651报文CRC16校验失败", gatewayCode, wrapper);
            }

        } catch (Exception e) {
            // ------------------ 异常捕获处理 ------------------
            log.error("会话通道:{}, sl651-2014 hex帧消息编码错误, hex帧消息报文 {}", channel, frameHexStr, e); // 注意这里用已转换的 frameHexStr 即可
            wrapper.setSuccess(false);

            // 发生异常时，同样尝试提取设备 ID 以便记录日志追踪
            String gatewayCode = StrUtil.sub(
                    StrUtil.subAfter(frameHexStr, "7e7e", false),
                    2,
                    12
            );
            LogUtil.logJsonMessage("sl651报文hex帧消息编码错误", gatewayCode, wrapper);

        } finally {
            // 7. 清理工作：无论成功、失败还是抛出异常，都必须将 ByteBuf 的读指针移到最后
            // 防止出现 Netty 内存泄漏 或 半包/粘包导致的下一次解析错乱
            byteBuf.skipBytes(byteBuf.readableBytes());
        }

        return wrapper;
    }


}
