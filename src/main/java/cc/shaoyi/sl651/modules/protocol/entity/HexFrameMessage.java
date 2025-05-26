package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Shao Yi
 * @description 16进制报文消息对象
 * @date 2022年08月03日 14:04
 */
@Data
@Accessors(chain = true)
@ToString
public class HexFrameMessage {

	private HexFrameHeaderMessage header;

	private HexFrameBodyMessage body;

	// 报文结束符
	private String bodyEndFrameMark;

	// CRC16校验码
	private String crcCode;

}
