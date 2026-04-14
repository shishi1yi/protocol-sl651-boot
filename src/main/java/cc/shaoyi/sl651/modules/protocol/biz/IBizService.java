package cc.shaoyi.sl651.modules.protocol.biz;


import cc.shaoyi.sl651.modules.protocol.entity.EncodeMessage;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageReq;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageResp;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.json.JSONObject;

import java.util.List;

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


	/**
	 * 升级
	 */
	EncodeMessage upgrade(String detectAddr, List<JSONObject> params);

	/**
	 * 发送 【查询实时数据】报文
	 */
	EncodeMessage op37(String detectAddr);

	/**
	 * 发送 【远程重启】报文
	 */
	EncodeMessage opE0(String detectAddr);

	/**
	 * 发送 【召测】报文
	 */
	EncodeMessage opE2(String detectAddr);

	/**
	 * 发送 【查询当前软件版本报文】报文
	 */
	EncodeMessage op45(String detectAddr);

	/**
	 * 发送 【查询基本参数配置】报文
	 */
	EncodeMessage op41(String detectAddr, List<JSONObject> params);

	/**
	 * 发送 【修改基本参数配置】报文
	 */
	EncodeMessage op40(String detectAddr, List<JSONObject> params);

	/**
	 * 发送 【修改运行参数配置】报文
	 */
	EncodeMessage op42(String detectAddr, List<JSONObject> params);


	/**
	 * 发送 【查询运行参数配置】报文
	 */
	EncodeMessage op43(String detectAddr, List<JSONObject> params);
}
