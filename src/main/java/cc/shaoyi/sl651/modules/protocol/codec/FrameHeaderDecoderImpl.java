package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cn.hutool.core.lang.Opt;
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

        // 中心站地址
        String hubAddress = FrameUtil.getHeaderHubAddress(hexFrame);

        // 测站地址，即测站码
        String detectAddress = FrameUtil.getHeaderDetectAddress(hexFrame);

        // 密码
        String pwd = FrameUtil.getHeaderPwd(hexFrame);

        // 功能码
        String commandCode = FrameUtil.getHeaderCommandCode(hexFrame);
        FrameCommandCodeEnum frameCommandCodeEnum = FrameCommandCodeEnum.getFrameFuncEnum(commandCode);
        // 正文长度
        Long bodyLen = FrameUtil.getBodyLen(hexFrame);

        headerMessage.setHubAddress(hubAddress)
                .setDetectAddress(detectAddress)
                .setPassword(pwd)
                .setCommandCode(commandCode)
                .setCommandName(
                        Opt.ofNullable(frameCommandCodeEnum)
                                .map(FrameCommandCodeEnum::formatName)
                                .orElse(String.format("编码(%s)", commandCode))
                )
                .setBodyLength(bodyLen)
                .setM3Mode(FrameUtil.isM3Mode(hexFrame));
        if (headerMessage.isM3Mode()) {
            // TODO 解析m3模式报文头特殊参数
            headerMessage.setFrameCnt(0);
            headerMessage.setFrameSerialNo(null);
        }
        return headerMessage;
	}
}
