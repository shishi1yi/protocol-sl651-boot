package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ShaoYi
 * @Description 标识符引导符
 * @createTime 2025年05月24日 15:49
 */
@Getter
@AllArgsConstructor
public enum PropertiesTypeCodeEnum {

	// 1a:小时降雨量
	hex_1a("1a", "小时降雨量"),

	// f4:1小时内每5分钟降雨量
	hex_f4("f4", "1小时内每5分钟降雨量");

	private final String code;

	private final String desc;
}
