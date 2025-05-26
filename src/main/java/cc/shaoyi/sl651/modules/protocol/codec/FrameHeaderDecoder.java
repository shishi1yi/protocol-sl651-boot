package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;

/**
 * @author Shao Yi
 * @description 报文头部帧消息解码器
 * @date 2022年08月03日 13:40
 */
public interface FrameHeaderDecoder {


	/**
	 * 解码16进制报文头部帧消息
	 * @param hexFrame 16进制报文
	 * @return
	 */
	HexFrameHeaderMessage decodeHexHeader(char[] hexFrame);


}
