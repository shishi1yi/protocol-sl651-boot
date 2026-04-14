package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.json.JSONObject;
import io.netty.buffer.ByteBuf;

import java.util.List;

public interface FrameEncoder {

	/**
	 * 帧消息编码
	 */
	ByteBuf encodeReply(HexFrameWrapper frameWrapper);

	/**
	 * 编码查询报文
	 */
	ByteBuf encodeAsk37(Integer serialNo, HexFrameHeaderMessage headerMessage);

	/**
	 * 编码远程重启报文
	 */
	ByteBuf encodeAskE0(Integer serialNo, HexFrameHeaderMessage headerMessage);

	/**
	 * 编码召测报文
	 */
	ByteBuf encodeAskE2(Integer serialNo, HexFrameHeaderMessage headerMessage);

	/**
	 * 编码查询当前软件版本报文
	 */
	ByteBuf encodeAsk45(Integer serialNo, HexFrameHeaderMessage headerMessage);

	/**
	 * 编码升级报文
	 */
	ByteBuf encodeUpgrade(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons);

	/**
	 * 编码 【查询基本参数配置】 报文
	 */
	ByteBuf encodeCommand41(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons);

	/**
	 * 编码 【修改基本参数配置】 报文
	 */
	ByteBuf encodeCommand40(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons);

	/**
	 * 编码 【修改运行参数配置】 报文
	 */
	ByteBuf encodeCommand42(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons);


	/**
	 * 编码 【查询运行参数配置】 报文
	 */
	ByteBuf encodeCommand43(Integer serialNo, HexFrameHeaderMessage headerMessage, List<JSONObject> paramJsons);

}
