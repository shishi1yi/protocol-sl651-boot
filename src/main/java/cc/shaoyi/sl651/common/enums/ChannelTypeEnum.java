package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年06月01日 16:47
 */
@Getter
@AllArgsConstructor
public enum ChannelTypeEnum {

	detect("detect", "遥测站的连接通道");

	private final String code;
	private final String desc;
}
