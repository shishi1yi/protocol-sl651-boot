package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Shao Yi
 * @description 报文正文消息对象
 * @date 2022年08月03日 17:27
 */
@Data
@Accessors(chain = true)
@ToString
public class HexFrameBodyMessage {

	// 流水号
	private Long serialNo;

	// 发报时间 YYMMDDHHmmSS
	private String sendTime;

	// 遥测站地址
	private String detectAddress;

	// 遥测站分类编码
	private String detectAddressTypeCode;

	// 观测时间 YYMMDDHHmm
	private String observeTime;

	// 遥测信息
	private LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> propertiesMessage;


}
