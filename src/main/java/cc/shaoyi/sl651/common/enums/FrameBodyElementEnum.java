package cc.shaoyi.sl651.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ShaoYi
 * @since 2025年11月27日 13:45
 */
@Getter
@AllArgsConstructor
public enum FrameBodyElementEnum {

	// ==========================================
	// F0H ~ FDH 标识符引导符 (控制/定义类)
	// ==========================================
	el_f0("f0", "tt", "观测时间引导符"),
	el_f1("f1", "st", "测站编码引导符"),
	el_f2("f2", "rgzs", "人工置数"),
	el_f3("f3", "pic", "图片信息"),
	el_f4("f4", "drp", "1h内每5min时段雨量"),
	el_f5("f5", "drz1", "1h内5min间隔相对水位1"),
	el_f6("f6", "drz2", "1h内5min间隔相对水位2"),
	el_f7("f7", "drz3", "1h内5min间隔相对水位3"),
	el_f8("f8", "drz4", "1h内5min间隔相对水位4"),
	el_f9("f9", "drz5", "1h内5min间隔相对水位5"),
	el_fa("fa", "drz6", "1h内5min间隔相对水位6"),
	el_fb("fb", "drz7", "1h内5min间隔相对水位7"),
	el_fc("fc", "drz8", "1h内5min间隔相对水位8"),
	el_fd("fd", "data", "流速批量数据传输"),

	// ==========================================
	// 01H ~ 75H 数据要素 (环境/水文/设备参数)
	// ==========================================

	// --- 01H ~ 11H (基础气象与土壤) ---
	el_01("01", "ac", "断面面积"),
	el_02("02", "ai", "瞬时气温"),
	el_03("03", "c", "瞬时水温"),
	el_04("04", "drxnn", "时间步长码"),
	el_05("05", "dt", "时段长，降水、引排水、抽水历时"),
	el_06("06", "ed", "日蒸发量"),
	el_07("07", "ej", "当前蒸发"),
	el_08("08", "fl", "气压"),
	el_09("09", "gh", "闸坝、水库闸门开启高度"),
	el_0a("0a", "gn", "输水设备、闸门(组)编号"),
	el_0b("0b", "gs", "输水设备类别"),
	el_0c("0c", "gt", "水库、闸坝闸门开启孔数"),
	el_0d("0d", "gtp", "地温"),
	el_0e("0e", "h", "地下水瞬时埋深"),
	el_0f("0f", "hw", "波浪高度"),
	el_10("10", "m10", "10cm处土壤含水量"),
	el_11("11", "m20", "20cm处土壤含水量"),

	// --- 12H ~ 1FH (深层土壤与降水) ---
	el_12("12", "m30", "30cm处土壤含水量"),
	el_13("13", "m40", "40cm处土壤含水量"),
	el_14("14", "m50", "50cm处土壤含水量"),
	el_15("15", "m60", "60cm处土壤含水量"),
	el_16("16", "m80", "80cm处土壤含水量"),
	el_17("17", "m100", "100cm处土壤含水量"),
	el_18("18", "mst", "湿度"),
	el_19("19", "ns", "开机台数"),
	el_1a("1a", "p1", "1h时段降水量"),
	el_1b("1b", "p2", "2h时段降水量"),
	el_1c("1c", "p3", "3h时段降水量"),
	el_1d("1d", "p6", "6h时段降水量"),
	el_1e("1e", "p12", "12h时段降水量"),
	el_1f("1f", "pd", "日降水量"),

	// --- 20H ~ 2FH (降水详情与流量) ---
	el_20("20", "pj", "当前降水量"),
	el_21("21", "pn01", "1min时段降水量"),
	el_22("22", "pn05", "5min时段降水量"),
	el_23("23", "pn10", "10min时段降水量"),
	el_24("24", "pn30", "30min时段降水量"),
	el_25("25", "pr", "暴雨量"),
	el_26("26", "pt", "降水量累计值"),
	el_27("27", "q", "瞬时流量、抽水流量"),
	el_28("28", "q1", "取(排)水口流量1"),
	el_29("29", "q2", "取(排)水口流量2"),
	el_2a("2a", "q3", "取(排)水口流量3"),
	el_2b("2b", "q4", "取(排)水口流量4"),
	el_2c("2c", "q5", "取(排)水口流量5"),
	el_2d("2d", "q6", "取(排)水口流量6"),
	el_2e("2e", "q7", "取(排)水口流量7"),
	el_2f("2f", "q8", "取(排)水口流量8"),

	// --- 30H ~ 38H (综合流量与气象/电源) ---
	el_30("30", "qa", "总出库流量、过闸总流量"),
	el_31("31", "qz", "输水设备流量、过闸(组)流量"),
	el_32("32", "sw", "输沙量"),
	el_33("33", "uc", "风向"),
	el_34("34", "ue", "风力(级)"),
	el_35("35", "us", "风速"),
	el_36("36", "va", "断面平均流速"),
	el_37("37", "vj", "当前瞬时流速"),
	el_38("38", "vt", "电源电压"),

	// --- 39H ~ 43H (水位信息) ---
	el_39("39", "z", "瞬时河道水位、潮位"),
	el_3a("3a", "zb", "库(闸、站)下水位"),
	el_3b("3b", "zu", "库(闸、站)上水位"),
	el_3c("3c", "z1", "取(排)水口水位1"),
	el_3d("3d", "z2", "取(排)水口水位2"),
	el_3e("3e", "z3", "取(排)水口水位3"),
	el_3f("3f", "z4", "取(排)水口水位4"),
	el_40("40", "z5", "取(排)水口水位5"),
	el_41("41", "z6", "取(排)水口水位6"),
	el_42("42", "z7", "取(排)水口水位7"),
	el_43("43", "z8", "取(排)水口水位8"),

	// --- 44H ~ 4DH (水质参数) ---
	el_44("44", "sq", "含沙量"),
	el_45("45", "zt", "遥测站状态及报警信息"),
	el_46("46", "ph", "pH值"),
	el_47("47", "do", "溶解氧"),
	el_48("48", "cond", "电导率"),
	el_49("49", "turb", "浊度"),
	el_4a("4a", "codmn", "高锰酸盐指数"),
	el_4b("4b", "redox", "氧化还原电位"),
	el_4c("4c", "nh4n", "氨氮"),
	el_4d("4d", "tp", "总磷"),
	el_4e("4e", "tn", "总氮"),
	el_4f("4f", "toc", "总有机碳"),

	// --- 50H ~ 57H (重金属参数) ---
	el_50("50", "cu", "铜"),
	el_51("51", "zn", "锌"),
	el_52("52", "se", "硒"),
	el_53("53", "as", "砷"),
	el_54("54", "thg", "总汞"),
	el_55("55", "cd", "镉"),
	el_56("56", "pb", "铅"),
	el_57("57", "chla", "叶绿素a"),



	// --- 58H ~ 5FH (水压信息) ---
	el_58("58", "wp1", "水压1"),
	el_59("59", "wp2", "水压2"),
	el_5a("5a", "wp3", "水压3"),
	el_5b("5b", "wp4", "水压4"),
	el_5c("5c", "wp5", "水压5"),
	el_5d("5d", "wp6", "水压6"),
	el_5e("5e", "wp7", "水压7"),
	el_5f("5f", "wp8", "水压8"),

	// --- 60H ~ 6FH (水表水量) ---
	el_60("60", "syl1", "水表1剩余水量"),
	el_61("61", "syl2", "水表2剩余水量"),
	el_62("62", "syl3", "水表3剩余水量"),
	el_63("63", "syl4", "水表4剩余水量"),
	el_64("64", "syl5", "水表5剩余水量"),
	el_65("65", "syl6", "水表6剩余水量"),
	el_66("66", "syl7", "水表7剩余水量"),
	el_67("67", "syl8", "水表8剩余水量"),
	el_68("68", "sbl1", "水表1每小时水量"),
	el_69("69", "sbl2", "水表2每小时水量"),
	el_6a("6a", "sbl3", "水表3每小时水量"),
	el_6b("6b", "sbl4", "水表4每小时水量"),
	el_6c("6c", "sbl5", "水表5每小时水量"),
	el_6d("6d", "sbl6", "水表6每小时水量"),
	el_6e("6e", "sbl7", "水表7每小时水量"),
	el_6f("6f", "sbl8", "水表8每小时水量"),

	// --- 70H ~ 75H (交流电参数) ---
	el_70("70", "vta", "交流A相电压"),
	el_71("71", "vtb", "交流B相电压"),
	el_72("72", "vtc", "交流C相电压"),
	el_73("73", "via", "交流A相电流"),
	el_74("74", "vib", "交流B相电流"),
	el_75("75", "vic", "交流C相电流"),

	// --- 76H-EFH ---
	// --- FFXXH ---
	el_ex("","","自定义标识");


	private final String hexCode;

	private final String asciiCode;

	private final String elementName;

	private static final Map<String, FrameBodyElementEnum> HEX_CODE_MAP = Arrays.stream(values())
		.collect(Collectors.toMap(FrameBodyElementEnum::getHexCode, Function.identity()));

	private static final Map<String, FrameBodyElementEnum> ASCII_CODE_MAP = Arrays.stream(values())
		.collect(Collectors.toMap(FrameBodyElementEnum::getAsciiCode, Function.identity()));

	/**
	 * 根据hexCode查找枚举
	 */
	public static FrameBodyElementEnum getByHexCode(String hexCode) {
		if (hexCode == null) return null;
		return HEX_CODE_MAP.getOrDefault(hexCode, el_ex);
	}

	/**
	 * 根据asciiCode查找枚举
	 */
	public static FrameBodyElementEnum getByAsciiCode(String asciiCode) {
		if (asciiCode == null) return null;
		return ASCII_CODE_MAP.getOrDefault(asciiCode, el_ex);
	}

	/**
     * 名称
	 */
	public String getFormatElementName() {
		if (this.getElementName() == null || this.getElementName().isEmpty()) {
			return String.format("hex编码(%s)_ascii编码(%s)", this.getHexCode(), this.getAsciiCode());
		}
		return this.getElementName();
	}


	@Getter
	@AllArgsConstructor
	public static enum HunanBodyElementEnum {

		el_hunan_ff11("ff11", "", "渗压监测点编号"),

		el_hunan_ff12("ff12", "", "渗流监测点编号"),

		el_hunan_ff13("ff13", "", "位移监测点编号"),

		el_hunan_ff14("ff14", "", "渗压水位"),

		el_hunan_ff15("ff15", "", "渗流"),

		el_hunan_ff16("ff16", "", "水平X位移"),

		el_hunan_ff17("ff17", "", "水平Y位移"),

		el_hunan_ff18("ff18", "", "垂直Z位移"),

		el_hunan_ff19("ff19", "", "经度坐标"),

		el_hunan_ff20("ff20", "", "纬度坐标"),

		el_hunan_ff21("ff21", "", "垂直高程"),

		el_hunan_ex("","","自定义标识");

		private final String hexCode;

		private final String asciiCode;

		private final String elementName;

		private static final Map<String, HunanBodyElementEnum> HEX_CODE_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(HunanBodyElementEnum::getHexCode, Function.identity()));

//		private static final Map<String, HunanBodyElementEnum> ASCII_CODE_MAP = Arrays.stream(values())
//			.collect(Collectors.toMap(HunanBodyElementEnum::getAsciiCode, Function.identity()));

		/**
		 * 根据hexCode查找枚举
		 */
		public static HunanBodyElementEnum getByHexCode(String hexCode) {
			if (hexCode == null) return null;
			return HEX_CODE_MAP.getOrDefault(hexCode, el_hunan_ex);
		}

		/**
		 * 根据asciiCode查找枚举
		 */
//		public static HunanBodyElementEnum getByAsciiCode(String asciiCode) {
//			if (asciiCode == null) return null;
//			return ASCII_CODE_MAP.getOrDefault(asciiCode, el_hunan_ex);
//		}

		/**
		 * 名称
		 */
		public String getFormatElementName() {
			if (this.getElementName() == null || this.getElementName().isEmpty()) {
				return String.format("hex编码(%s)_ascii编码(%s)", this.getHexCode(), this.getAsciiCode());
			}
			return String.format("湖南_%s", this.getElementName());
		}
	}
}
