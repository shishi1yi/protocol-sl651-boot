package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyMessage;

/**
 * @author Shao Yi
 * @description 报文正文帧消息解码器
 * @date 2022年08月03日 13:40
 */
public interface FrameBodyDecoder {

	/**
	 * 处理M3类型正文帧消息
	 * @param bodyFrame
	 * @return
	 */
	HexFrameBodyMessage decodeM3Body(char[] bodyFrame);

	/**
	 * 处理M1\M2\M4类型正文帧消息
	 * @param bodyFrame
	 * @param commandCodeEnum
	 * @return
	 */
	HexFrameBodyMessage decodeM124Body(char[] bodyFrame, FrameCommandCodeEnum commandCodeEnum);


}
