package cc.shaoyi.sl651.common.constant;

/**
 * 通用常量
 *
 * @author shaoyi
 */
public interface CommonConstant {

	String APPLICATION_PROTOCOL_SL651 = "protocol-sl651";

	Integer MAX_FRAME_LEN = 4095;

	// 帧起始符，2字节
	String HEADER_START_HEX = "7E7E";

	// M3模式报文起始符SYN，1字节
	String HEADER_START_BODY_M3_FLAG_HEX = "16";

	// 报文控制符EOT，传输结束退出通信
	String HEADER_RSP_BODY_END_EOT_HEX = "04";

	// 传输正文起始标记STX
	String HEADER_RSP_BODY_END_STX_HEX = "02";

	// 传输正文结束标记ETX
	String HEADER_RSP_BODY_END_ETX_HEX = "03";

	// 传输正文结束标记ACK
	String HEADER_RSP_BODY_END_ENQ_HEX = "05";

	// 传输正文结束标记ACK
	String HEADER_RSP_BODY_END_ACK_HEX = "06";

	// 下行报文标识和0长报文体
	String HEADER_RSP_DOWN_SYMBOL_AND_ZERO_LEN_HEX = "8000";

	String HEARTBEAT_COMMAND_CODE = "2f";

	String DOWN_TAG = "1000";

	String UP_TAG = "0000";


}
