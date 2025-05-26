package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Shao Yi
 * @description 16进制消息
 * @date 2022年08月02日 16:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ProtocolHexMessage {

	// 帧起始符 2个字节
	private byte[] beginFrameMark = {0x7E, 0x7E};

	// 中心站地址 1个字节
	private byte hubAddress;

	// 遥测站地址 5个字节
	private byte[] detectAddress = new byte[5];

	// 密码 2个字节
	private byte[] password = new byte[2];

	// 功能码 1个字节
	private byte commandCode;

	// 报文上下标识以及正文长度 2个字节
	private byte[] bodyMarkBodyLength = new byte[2];

	// 报文起始符 1个字节
	private byte bodyBeginFrameMark = 0x02;

	// 报文正文
	private byte[] body;

	// 报文结束符 1个字节
	private byte bodyEndFrameMark = 0x06;

	// 校验码 2个字节
	private byte[] checkCode = new byte[2];

}
