package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Shao Yi
 * @description 命令码枚举类
 * @date 2022年08月03日 13:27
 */
@Getter
@AllArgsConstructor
public enum FrameCommandCodeEnum {

	HEART_BEAT("2f", "链路维持-心跳包(2f)", "心跳包(2f)"),

	TEST_REPORT("30", "测试上报(30)", "测试报(30)"),

	REGULAR_REPORT("32", "定时上报(32)", "定时报(32)"),

	OVERTIME_REPORT("33", "加时上报(33)", "加时报(33)"),

	HOUR_REPORT("34", "小时上报(34)", "小时报(34)"),

	IMG_REPORT("36", "报送图片", "报送图片(36)"),

	CURRENT_REPORT("37", "查询实时数据(37)", "查询报(37)"),

	IMG_REPORT_E4("e4", "报送图片-小时报(湖南协议私有)", "图片小时报(pri_湖南)(e4)"),

	IMG_REPORT_E3("e3", "报送图片-加报(湖南协议私有)", "图片加报(pri_湖南)(e3)");

	private final String code;

	private final String desc;

	private final String formatStr;

	private static final Map<String, FrameCommandCodeEnum> CODE_MAP = Arrays.stream(values())
		.collect(Collectors.toMap(FrameCommandCodeEnum::getCode, Function.identity()));

	public static FrameCommandCodeEnum getFrameFuncEnum(String code) {
		if (code == null) return null;
		return CODE_MAP.get(code);
	}


	public String formatName() {
		if (this.getFormatStr() == null || this.getFormatStr().isEmpty()) {
			return String.format("%s(%s)", this.getDesc(), this.getCode());
		}
		return this.getFormatStr();
	}

}
