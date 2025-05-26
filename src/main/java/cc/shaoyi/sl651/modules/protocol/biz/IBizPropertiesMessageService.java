package cc.shaoyi.sl651.modules.protocol.biz;


import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2025年05月24日 15:19
 */
public interface IBizPropertiesMessageService {


	/**
	 * 处理补充属性消息
	 */
	void handleJoinPropertiesMessage(HexFrameWrapper hexFrameWrapper);


	/**
	 * 小时报文 处理补充属性消息
	 * 因特殊情况，对接第三方场景下，导致部分属性无法进行直传，但根据已知内容可以推算出
	 */
	void handleHourReportJoinPropertiesMessage(HexFrameWrapper hexFrameWrapper);
}
