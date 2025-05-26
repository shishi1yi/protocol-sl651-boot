package cc.shaoyi.sl651.modules.protocol.biz;


import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageReq;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageResp;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;

public interface IBizService {

	/**
	 * 处理业务
	 * @param hexFrameWrapper
	 * @return
	 */
	boolean handler(HexFrameWrapper hexFrameWrapper);


	/**
	 * 解析帧
	 * @param message
	 * @return
	 */
	FrameMessageResp parseFrame(FrameMessageReq message);
}
