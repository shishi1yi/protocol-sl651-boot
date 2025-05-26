package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Shao Yi
 * @description 16进制报文消息对象整合对象
 * @date 2022年08月03日 14:03
 */
@Data
@Accessors(chain = true)
@ToString
public class HexFrameWrapper implements Serializable {

	boolean isSuccess;

	/**
	 * 帧消息
	 */
	private HexFrameMessage message;

	/**
	 * 原始帧消息
	 */
	private String originalFrame;

	/**
	 * 远程地址
	 */
	private String remoteAddress;

	/**
	 * 本地地址
	 */
	private String localAddress;

	@Serial
	private static final long serialVersionUID = -5054935764477716404L;
}
