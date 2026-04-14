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

	REVISE_BASE_PARAM("40", "修改的查询基础配置参数(40)", "基础配置参数(40)"),

	BASE_PARAM("41", "查询基础配置参数(41)", "基础配置参数(41)"),

	REVISE_RUNNING_PARAM("42", "修改的运行配置参数(42)", "运行配置参数(42)"),

	RUNNING_PARAM("43", "查询运行配置参数(43)", "运行配置参数(43)"),

	SOFTWARE_VERSION("45", "查询当前软件版本(45)", "软件版本(45)"),

	PRI_COMMAND("unknown", "未知或协议私有命令", "未知或协议私有命令"),

	;

	private final String code;

	private final String desc;

	private final String formatStr;

	private static final Map<String, FrameCommandCodeEnum> CODE_MAP = Arrays.stream(values())
		.collect(Collectors.toMap(FrameCommandCodeEnum::getCode, Function.identity()));

	public static FrameCommandCodeEnum getFrameFuncEnum(String code) {
		if (code == null) return null;
		FrameCommandCodeEnum commandCodeEnum = CODE_MAP.get(code);
		return commandCodeEnum == null ? FrameCommandCodeEnum.PRI_COMMAND : commandCodeEnum;
	}


	public String formatName() {
		if (this.getFormatStr() == null || this.getFormatStr().isEmpty()) {
			return String.format("%s(%s)", this.getDesc(), this.getCode());
		}
		return this.getFormatStr();
	}


	// ================== 湖南私有协议 ==================
	@Getter
	@AllArgsConstructor
	public enum PRI_HUNAN {

		HUNAN_IMG_REPORT_E4("e4", "报送图片-小时报(湖南协议私有)", "图片小时报(pri_湖南)(e4)"),

		HUNAN_IMG_REPORT_E3("e3", "报送图片-加报(湖南协议私有)", "图片加报(pri_湖南)(e3)");

		private final String code;

		private final String desc;

		private final String formatStr;

		private static final Map<String, PRI_HUNAN> HUNAN_CODE_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(PRI_HUNAN::getCode, Function.identity()));


		public static PRI_HUNAN getFrameFuncEnum(String code) {
			if (code == null) return null;
			return HUNAN_CODE_MAP.get(code);
		}

		public String formatName() {
			if (this.getFormatStr() == null || this.getFormatStr().isEmpty()) {
				return String.format("%s(%s)", this.getDesc(), this.getCode());
			}
			return this.getFormatStr();
		}
	}


	// ================== 公司自定义协议 ==================
	@Getter
	@AllArgsConstructor
	public enum PRI_COMPANY {
		COMP_CUSTOM_E3("e3", "公司自定义遥测站报送升级结果", "公司报送升级报(pri_comp)(e3)"),
		COMP_CUSTOM_E0("e0", "公司自定义遥测站报送设备重启结果", "公司报送设备重启报(pri_comp)(e0)"),
		COMP_CUSTOM_E2("e2", "公司自定义遥测站报送设备召测结果", "公司报送设备召测报(pri_comp)(e2)"),

		;

		private final String code;
		private final String desc;
		private final String formatStr;

		private static final Map<String, PRI_COMPANY> COMP_CODE_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(PRI_COMPANY::getCode, Function.identity()));

		public static PRI_COMPANY getFrameFuncEnum(String code) {
			if (code == null) return null;
			return COMP_CODE_MAP.get(code);
		}
		public String formatName() {
			if (this.getFormatStr() == null || this.getFormatStr().isEmpty()) {
				return String.format("%s(%s)", this.getDesc(), this.getCode());
			}
			return this.getFormatStr();
		}
	}

}
