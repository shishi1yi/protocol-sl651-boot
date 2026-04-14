package cc.shaoyi.sl651.modules.protocol.entity;

import cc.shaoyi.sl651.common.enums.ParamDataTypeEnum;
import lombok.Getter;

/**
 * @author ShaoYi
 * @since 2026年03月13日 15:14
 */
@Getter
public class ParamItem {
	private final String content;
	private final ParamDataTypeEnum type;

	// 构造函数：仅传内容，默认走自动判断逻辑
	public ParamItem(String content) {
		this.content = content;
		this.type = ParamDataTypeEnum.AUTO;
	}

	// 构造函数：显式指定类型
	public ParamItem(String content, ParamDataTypeEnum type) {
		this.content = content;
		this.type = type != null ? type : ParamDataTypeEnum.AUTO;
	}

}
