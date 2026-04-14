package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author ShaoYi
 * @since 2026年03月05日 15:44
 */
@Builder
@Data
@Accessors(chain = true)
public class EncodeMessage implements Serializable {

	/**
	 * 测站地址
	 */
	private String detectAddress;

	/**
	 * 流水号
	 */
	private Integer serialNo;

	/**
	 * 流水号（16进制）
	 */
	private String serialNoHex;

	/**
	 * 报文帧（16进制）
	 */
	private String frameHex;

	/**
	 * 回文码 （测站地址 + 功能码 + 流水号 组成）
	 */
	private String reqCode;


	private static final long serialVersionUID = 3876487702339539640L;
}
