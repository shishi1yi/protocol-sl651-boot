package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ShaoYi
 * @since 2026年03月11日 11:54
 */
@Getter
@AllArgsConstructor
public enum FrameParamBodyElementEnum {


	el_param_01("01", "中心站地址"),

	el_param_02("02", "遥测站地址"),

	el_param_03("03", "密码"),

	el_param_04("04", "中心站1主信道类型以及地址"),
	el_param_05("05", "中心站1备用信道类型以及地址"),
	el_param_06("06", "中心站2信道类型以及地址"),
	el_param_07("07", "中心站2备用信道类型以及地址"),
	el_param_08("08", "中心站3信道类型以及地址"),
	el_param_09("09", "中心站3备用信道类型以及地址"),
	el_param_0a("0a", "中心站4信道类型以及地址"),
	el_param_0b("0b", "中心站4备用信道类型以及地址"),

	el_param_0c("0c", "工作方式"),
	el_param_0d("0d", "遥测站采集要素设置"),
	el_param_0e("0e", "中继站(集合转发站)服务地址范围"),
	el_param_0f("0f", "遥测站通信设备识别号"),

	el_param_ex_10("10", "物联网管理平台地址"),

	el_param_ex_11("11", "报文类型"),
	el_param_ex_12("12", "设备属性"),

	el_param_ex_ff07("ff07", "升级结果"),
	el_param_ex_ff00("ff00", "遥测站软件版本"),


	// 运行参数
	el_param_20("20", "定时报时间间隔"),
	el_param_21("21", "加报时间间隔"),
	el_param_22("22", "降水量日起始时间"),
	el_param_23("23", "采样间隔"),
	el_param_25("25", "雨量计分辨率"),
	el_param_27("27", "雨量加报阈值"),
	el_param_28("28", "水位基值1"),
	el_param_30("30", "水位修正值1"),
	el_param_38("38", "加报水位1"),
	el_param_42("42", "流量加报阈值"),
	el_param_ex_ff01("ff01", "水位标识符"),
	el_param_ex_ff02("ff02", "水位计类型"),
	el_param_ex_ff03("ff03", "水位采集信息"),


	el_param_ex("","自定义标识");

	private final String hexCode;

	private final String elementName;

	private static final Map<String, FrameParamBodyElementEnum> HEX_CODE_MAP = Arrays.stream(values())
		.collect(Collectors.toMap(FrameParamBodyElementEnum::getHexCode, Function.identity()));


	/**
	 * 根据hexCode查找枚举
	 */
	public static FrameParamBodyElementEnum getByHexCode(String hexCode) {
		if (hexCode == null) return null;
		return HEX_CODE_MAP.getOrDefault(hexCode, el_param_ex);
	}


	/**
	 * 名称
	 */
	public String getFormatElementName() {
		if (this.getElementName() == null || this.getElementName().isEmpty()) {
			return String.format("hex编码(%s)", this.getHexCode());
		}
		return this.getElementName();
	}
}
