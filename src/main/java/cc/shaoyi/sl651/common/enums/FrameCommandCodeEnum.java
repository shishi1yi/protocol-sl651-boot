package cc.shaoyi.sl651.common.enums;

import lombok.Getter;

/**
 * @author Shao Yi
 * @description 命令码枚举类
 * @date 2022年08月03日 13:27
 */
@Getter
public enum FrameCommandCodeEnum {

	HEART_BEAT("2F", "链路维持-心跳包(2F)"),

	TEST_REPORT("30", "测试上报(30)"),

	REGULAR_REPORT("32", "定时上报(32)"),

	OVERTIME_REPORT("33", "加时上报(33)"),

	HOUR_REPORT("34", "小时上报(34)"),

	CURRENT_REPORT("37", "查询实时数据(37)");

	private final String code;
	private final String desc;

	FrameCommandCodeEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static FrameCommandCodeEnum getFrameFuncEnum(String code) {
		for (FrameCommandCodeEnum commandCodeEnum : FrameCommandCodeEnum.values()) {
			if (commandCodeEnum.code.equals(code)) {
				return commandCodeEnum;
			}
		}
		return null;
	}

}
