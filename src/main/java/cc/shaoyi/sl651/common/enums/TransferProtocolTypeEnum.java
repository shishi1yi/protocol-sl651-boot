package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ShaoYi
 * @since 2026年03月18日 13:44
 * 定义协议枚举
 */
@Getter
@AllArgsConstructor
public enum TransferProtocolTypeEnum {
	STANDARD,    // 标准
	HUNAN,      // 湖南协议
	PRI_COMPANY // 公司自定义协议
}
