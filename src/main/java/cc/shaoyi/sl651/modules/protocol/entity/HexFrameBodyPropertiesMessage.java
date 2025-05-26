package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author Shao Yi
 * @description 正文属性对象
 * @date 2022年08月03日 17:31
 */
@Data
@Accessors(chain = true)
@ToString
public class HexFrameBodyPropertiesMessage {

	// 标识符引导符
	private String typeCode;

	// 数据字节数
	private Integer dataSize;

	// 小数位数
	private Integer decimalSize;

	// 原始数据
	private String originalData;

	// 实际值
	private BigDecimal val;
}
