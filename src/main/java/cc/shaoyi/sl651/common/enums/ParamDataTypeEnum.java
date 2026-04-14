package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ShaoYi
 * @since 2026年03月13日 15:14
 */
@Getter
@AllArgsConstructor
public enum ParamDataTypeEnum {

	/** 强制作为文本处理 (ASCII) */
	STRING("STRING", "文本 (ASCII)"),
	/** 自动判断 (依赖 NumberUtil.isNumber) */
	AUTO("AUTO", "自动判断");


	private final String typeCode;

	private final String desc;

	private static final Map<String, ParamDataTypeEnum> paramDataTypeMap = Arrays.stream(values())
		.collect(Collectors.toMap(ParamDataTypeEnum::getTypeCode, Function.identity()));

	public static ParamDataTypeEnum getType(String typeCode) {
		ParamDataTypeEnum dataType = paramDataTypeMap.get(typeCode);
		return dataType == null ? ParamDataTypeEnum.AUTO : dataType;
	}
}
