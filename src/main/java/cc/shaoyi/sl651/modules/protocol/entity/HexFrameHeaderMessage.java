package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author Shao Yi
 * @description 报文头部帧消息对象
 * @date 2022年08月03日 13:27
 */
@Data
@Accessors(chain = true)
@ToString
public class HexFrameHeaderMessage {

	// 中心站地址
	private String hubAddress;

	// 遥测站地址
	private String detectAddress;

	// 密码
	private String password;

	// 功能码
	private String commandCode;

	// 正文长度
	private Long bodyLength;

	// 是否为m3模式
	private boolean m3Mode;

	// 包总数，m3模式才有
	private Integer frameCnt;

	// 包序列号，m3模式才有
	private String frameSerialNo;
}
