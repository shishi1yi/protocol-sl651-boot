package cc.shaoyi.sl651.common.utils;


import cc.shaoyi.sl651.common.constant.CommonConstant;
import cc.shaoyi.sl651.common.enums.FrameBodyElementEnum;
import cc.shaoyi.sl651.common.enums.FrameParamBodyElementEnum;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyPropertiesMessage;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 水文协议报文工具类
 *
 * @author shaoyi
 * @date 2022/5/11 22:44
 */
@Slf4j
public class FrameUtil {

    /**
     * 判断报文是否为M3多包传输模式
     * @param frame
     * @return boolean
     */
    public static boolean  isM3Mode(char[] frame) {
        return CommonConstant.HEADER_START_BODY_M3_FLAG_HEX.equalsIgnoreCase(HexStringUtil.int2HexStr(frame[13]));
    }

    /**
     * 获取中心站地址
     * @param frame
     * @return java.lang.String
     */
    public static String getHeaderHubAddress(char[] frame) {
        return HexStringUtil.int2HexStr(frame[2]);
    }

    /**
     * 获取遥测站地址
     * @param frame
     * @return java.lang.String
     */
    public static String getHeaderDetectAddress(char[] frame) {
        return HexStringUtil.int2HexStr(frame, 3, 7);
    }

    /**
     * 获取密码
     * @param frame
     * @return java.lang.String
     */
    public static String getHeaderPwd(char[] frame) {
        return HexStringUtil.int2HexStr(frame, 8, 9);
    }

    /**
     * 获取功能码
     * @param frame
     * @return java.lang.String
     */
    public static String getHeaderCommandCode(char[] frame) {
        return HexStringUtil.int2HexStr(frame[10]);
    }

    /**
     * 获取报文正文长度
     * @param frame
     * @return java.lang.Long
     */
    public static Long getBodyLen(char[] frame) {
        return Long.parseLong(HexStringUtil.int2HexStr(frame, 11, 12), 16);
    }

    public static char[] getM3Body(char[] frame, int bodyLen) {
        char[] bodyFrame = new char[bodyLen];
        // M3报文头比M124多3个字节
        System.arraycopy(frame, (2 + 1 + 5 + 2 + 1 + 2 + 1 + 3), bodyFrame, 0, bodyLen);
        return bodyFrame;
    }

    public static char[] getM124Body(char[] frame, int bodyLen) {
        char[] bodyFrame = new char[bodyLen];
        System.arraycopy(frame, (2 + 1 + 5 + 2 + 1 + 2 + 1), bodyFrame, 0, bodyLen);
        return bodyFrame;
    }

    /**
     * 获取报文CRC16校验码
     *
     * @param frame
     * @return java.lang.String
     */
    public static String getCRC16Code(char[] frame) {
        // 位于报文末尾，2字节
        return HexStringUtil.int2HexStr(frame, frame.length - 2, frame.length - 1);
    }

    public static String getBodyEndSymbol(char[] frame) {
        // 位于报文末尾，1字节
        return HexStringUtil.int2HexStr(frame, frame.length - 3, frame.length - 3);
    }

    /**
     * 报文crc16校验
     * @param frame
     * @return boolean
     */
    public static boolean verifyCRC16Code(String frame) {
		String framePre = StrUtil.subPre(frame, frame.length() - 4);
		String originalCRC16Code = StrUtil.subSuf(frame, frame.length() - 4);
		CRC16Modbus crc16Modbus = new CRC16Modbus();
		crc16Modbus.update(HexUtil.decodeHex(StrUtil.replace(framePre, " ", "")));
		String crc16CodeHexValue = crc16Modbus.getHexValue();
		String crc16Code = StrUtil.padPre(crc16CodeHexValue, 4, '0');
		boolean verifyResult = StrUtil.equalsIgnoreCase(originalCRC16Code, crc16Code);
		if (!verifyResult) {
			log.error("[CRC16校验码-校验不通过] CRC16Modbus算出:{}，补全CRC16校验码:{}，报文帧中校验码:{}" , crc16CodeHexValue, crc16Code, originalCRC16Code);
		}
		return verifyResult;
    }

    public static String computeCRC16Code(char[] data) {
        return Crc16Util.crc16(new String(data).getBytes(), false);
    }

    public static int getBodyElementByteSize(char c) {
        // 16进制字符串转十进制，字节高5位为数据字节数
        return (Integer.parseInt(Integer.toHexString(c), 16) & 0xf8) >> 3;
    }

    public static int getBodyElementDecimalSize(char c) {
        // 16进制字符串转十进制，字节低3位为小数位数
        return Integer.parseInt(Integer.toHexString(c), 16) & 0x07;
    }

	/**
	 * 统一的解析执行包装器 (消除重复的 try-catch 与日志代码)
	 *
	 * @param frame      报文正文字符数组
	 * @param methodName 当前正在执行的解析类型（用于日志排查，例如"基础参数解析"）
	 * @param parseLogic 核心的循环解析逻辑 (Lambda表达式)
	 * @return 解析结果 Map
	 */
	private static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> parseWithCatch(
		char[] frame,
		String methodName,
		BiConsumer<char[], LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>>> parseLogic) {

		// 1. 统一初始化 Map
		LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();

		// 2. 统一防御空报文
		if (ArrayUtil.isEmpty(frame)) {
			return elementMap;
		}

		try {
			// 3. 执行外部传进来的核心解析逻辑
			parseLogic.accept(frame, elementMap);
		} catch (Exception e) {
			// 4. 统一的极其详细的异常日志记录！
			log.error("[{}] 规则解析异常！\n原始报文正文: {}\n报错前已成功解析的内容: {}",
				methodName,
				HexStringUtil.int2HexStr(frame, 0, frame.length - 1),
				JSONUtil.toJsonStr(elementMap),
				e);
			throw new RuntimeException("报文解析异常: " + methodName, e);
		}

		return elementMap;
	}


	/**
	 * 处理单一键值报文正文内容
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> simpleReportBodyProperties(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "单一键值报文", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				index = parseSinglePropertiesMessages(frame, index, map, false);
			}
		});
	}

	/**
	 * 处理遥测基础参数配置报文正文内容 (标准协议入口)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> baseParamReportBodyProperties(char[] bodyElementFrame) {
		// 传入 false 代表使用标准解析规则
		return doParseBaseParamBodyProperties(bodyElementFrame, false);
	}

	/**
	 * 处理遥测基础参数配置报文正文内容 (公司自定义解析规则入口)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> baseParamPriCompanyReportBodyProperties(char[] bodyElementFrame) {
		// 传入 true 代表使用公司自定义解析规则
		return doParseBaseParamBodyProperties(bodyElementFrame, true);
	}

	/**
	 * 统一的核心解析【基础参数】调度逻辑
	 *
	 * @param bodyElementFrame 报文正文全部字符数组
	 * @param isCustomProtocol 是否为公司自定义协议 (决定是否将自定义标识引导符 视为特殊信道)
	 * @return 解析结果 Map
	 */
	private static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> doParseBaseParamBodyProperties(char[] bodyElementFrame, boolean isCustomProtocol) {

		return parseWithCatch(bodyElementFrame, "基础参数配置", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				// 建议加上 .toLowerCase()，防止部分十六进制工具类输出 "0A" 大写导致 switch 匹配失败
				String mark = HexStringUtil.int2HexStr(frame[index]).toLowerCase();

				switch (mark) {
					case "01":
						index = special01ParamBodyPropertiesMessages(frame, index, map);
						break;
					case "04": case "05": case "06": case "07":
					case "08": case "09": case "0a": case "0b":
						index = special04To0BParamBodyPropertiesMessages(frame, index, map);
						break;
					case "10":
						// 【区分逻辑】：针对 标识引导符（10）做分流
						if (isCustomProtocol) {
							// 自定义规则：将 标识引导符（10） 作为物联网管理平台信道，与 04 至 0b 解法一样
							index = special04To0BParamBodyPropertiesMessages(frame, index, map);
						} else {
							// 标准规则：不认识 10，直接走默认的普通单一属性解析机制
							index = parseSinglePropertiesMessages(frame, index, map, true);
						}
						break;
					case "12":
						// 【区分逻辑】：针对 标识引导符（12）做分流
						if (isCustomProtocol) {
							// 自定义标识引导符（12）自定义规则
							index = specialPriCompanyEx12ParamBodyPropertiesMessages(frame, index, map);
						} else {
							// 标准规则：不认识 12，直接走默认的普通单一属性解析机制
							index = parseSinglePropertiesMessages(frame, index, map, true);
						}
						break;
					case "0c":
						index = special0CParamBodyPropertiesMessages(frame, index, map);
						break;
					case "0d":
						index = special0DParamBodyPropertiesMessages(frame, index, map);
						break;
					case "0e":
						index = special0EParamBodyPropertiesMessages(frame, index, map);
						break;
					case "0f":
						index = special0FParamBodyPropertiesMessages(frame, index, map);
						break;
					default:
						// 兜底：所有不属于 special 的标识符，统统走普通解析
						index = parseSinglePropertiesMessages(frame, index, map, true);
						break;
				}
			}
		});
	}



	/**
	 * 处理遥测运行参数配置报文正文内容 (标准协议入口)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> runParamReportBodyProperties(char[] bodyElementFrame) {
		// 传入 false 代表使用标准解析规则
		return doParseRunParamBodyProperties(bodyElementFrame, false);
	}

	/**
	 * 处理遥测运行参数配置报文正文内容 (公司自定义解析规则入口)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> runParamPriCompanyReportBodyProperties(char[] bodyElementFrame) {
		// 传入 true 代表使用公司自定义解析规则
		return doParseRunParamBodyProperties(bodyElementFrame, true);
	}


	/**
	 * 统一的核心解析【运行参数】调度逻辑
	 *
	 * @param bodyElementFrame 报文正文全部字符数组
	 * @param isCustomProtocol 是否为公司自定义协议 (决定是否将自定义标识引导符 视为特殊信道)
	 * @return 解析结果 Map
	 */
	private static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> doParseRunParamBodyProperties(char[] bodyElementFrame, boolean isCustomProtocol) {
		return parseWithCatch(bodyElementFrame, "运行参数配置", (frame, map) -> {
			// TODO 实现解析【运行参数】逻辑，以及区分是否是公司要求的自定义标识引导符
			for (int index = 0; index < frame.length;) {
				// 建议加上 .toLowerCase()，防止部分十六进制工具类输出 "0A" 大写导致 switch 匹配失败
				String mark = HexStringUtil.int2HexStr(frame[index]).toLowerCase();

				if ("ff".equals(mark)) {
					String markSuffix = HexStringUtil.int2HexStr(frame[index + 1]).toLowerCase();
					mark = mark + markSuffix;
				}

				switch (mark) {
					case "ff03":
						// 【区分逻辑】：针对 标识引导符（ff03）做分流
						if (isCustomProtocol) {
							// 自定义规则：将 标识引导符（ff03） 作为水位采集信息
							index = specialFF03ParamBodyPropertiesMessages(frame, index, map);
						} else {
							// 标准规则：不认识 ff03，直接走默认的普通单一属性解析机制
							index = parseSinglePropertiesMessages(frame, index, map, true);
						}
						break;
					default:
						// 兜底：所有不属于 special 的标识符，统统走普通解析
						index = parseSinglePropertiesMessages(frame, index, map, true);
						break;
				}
			}
		});
	}


	/**
	 * 处理小时报报文正文内容
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHourReportBodyProperties(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "标准小时报", (frame, map) -> {
			// 每组数据，前两字节为标识符，其中第一个字节为标识引导符，第二个字节定义数据信息
			for (int index = 0; index < frame.length;) {
				String mark = Integer.toHexString(frame[index]);
				switch (mark) {
					case "f0" :
					case "f1" :
						// 处理出现在正文标识符中 时间标识符（f0）、测站编码标识符（f1）
						index = specialF0AndF1BodyPropertiesMessages(frame, index, map);
						break;
					case "f4" :
						// 1小时内每5分钟时段的降雨量
						index = specialF4BodyPropertiesMessages(frame, index, map);
						break;
					case "f5" : case "f6" : case "f7" : case "f8" :
					case "f9" : case "fa" : case "fb" : case "fc" :
						// 处理1小时内每5分钟间隔的相对水位数据
						index = specialF5TOFCBodyPropertiesMessages(frame, index, map);
						break;
					default:
						index = parseSinglePropertiesMessages(frame, index, map, false);
						break;
				}
			}
		});
	}

	/**
	 * 处理小时报报文正文内容 （湖南省解析规则）
	 * @param bodyElementFrame
	 * @return
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHourReportHuNanBodyProperties(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "湖南省小时报", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				String mark = Integer.toHexString(frame[index]);
				switch (mark) {
					case "f0" : case "f1" :
						index = specialF0AndF1BodyPropertiesMessages(frame, index, map);
						break;
					case "f4" :
						index = specialF4BodyPropertiesMessages(frame, index, map);
						break;
					case "f5" : case "f6" : case "f7" : case "f8" :
					case "f9" : case "fa" : case "fb" : case "fc" :
						index = specialF5TOFCBodyPropertiesMessages(frame, index, map);
						break;
					case "ff":
						// 处理安全监测设备数据
						index = huNanBodyPropertiesMessages(frame, index, map);
						break;
					default:
						index = parseSinglePropertiesMessages(frame, index, map, false);
						break;
				}
			}
			// 循环解析完成后，直接操作 map 进行无效数据置空处理
			huNanElementInvalid(map);
		});
	}


	/**
	 * 根据湖南规约的小时报，去除无效的数据值
	 * @param elementMap
	 */
	private static void huNanElementInvalid(LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		if (CollUtil.isNotEmpty(elementMap)) {
			/**
			 * ff11: 6个渗压监测点编号  ===>  ff14: 6个渗压水位数据
			 * ff12: 2个渗流监测点编号  ===>  ff15: 2个渗流监测点采集值
			 * ff13: 2个位移监测点编号  ===>  ff16: 2个水平X位移监测点位移值、ff17: 2个水平Y位移监测点位移值、ff18: 2个垂直位移采集值
			 * 如果编号为(00000000)标识无效，后面对应的数据值不需要解析
			 */
			huNanHandleElementInvalidToNull(elementMap, "ff11", Lists.newArrayList("ff14"));
			huNanHandleElementInvalidToNull(elementMap, "ff12", Lists.newArrayList("ff15"));
			huNanHandleElementInvalidToNull(elementMap, "ff13", Lists.newArrayList("ff16", "ff17", "ff18"));
		}
	}

	/**
	 * 处理无效的数据值
	 * @param elementMap
	 * @param signCode
	 * @param signContentCodeList
	 */
	private static void huNanHandleElementInvalidToNull(LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap, String signCode, List<String> signContentCodeList) {
		List<HexFrameBodyPropertiesMessage> signBodyPropertiesMessageList = elementMap.get(signCode);
		if (CollUtil.isNotEmpty(signBodyPropertiesMessageList)) {
			// 获取无效的标识的索引值
			List<Integer> sign_invalid_index_list = IntStream.range(0, signBodyPropertiesMessageList.size())
				.boxed()
				.filter(index -> {
					HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = signBodyPropertiesMessageList.get(index);
					return StrUtil.equals(hexFrameBodyPropertiesMessage.getOriginalData(), "00000000");
				})
				.collect(Collectors.toList());

			// 根据索引值，把val设置为空
			for (String signContentCode : signContentCodeList) {
				List<HexFrameBodyPropertiesMessage> signContentbBodyPropertiesMessageList = elementMap.get(signContentCode);
				if (CollUtil.isNotEmpty(sign_invalid_index_list) && CollUtil.isNotEmpty(signContentbBodyPropertiesMessageList)) {
					IntStream.range(0, signContentbBodyPropertiesMessageList.size())
						.boxed()
						.filter(sign_invalid_index_list::contains)
						.forEach(index -> {
							HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = signContentbBodyPropertiesMessageList.get(index);
							hexFrameBodyPropertiesMessage.setVal(null);
						});
				}
			}

		}
	}


	/**
	 * 统一解析单一属性值 (兼容普通正文与参数正文)
	 * <p>
	 * 说明：解析报文中的【标识符(1-2字节) + 数据定义(1字节) + 数据内容(N字节)】格式
	 *
	 * @param bodyElementFrame 报文正文全部字符数组
	 * @param index            当前解析的游标索引
	 * @param elementMap       结果存储容器
	 * @param isParamMode      是否为参数模式（true: 使用 FrameParamBodyElementEnum 解析名称; false: 使用 FrameBodyElementEnum 解析名称）
	 * @return 解析完成后的新游标索引 (指向下一段数据的开始)
	 */
	private static int parseSinglePropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap, boolean isParamMode) {
		// ==========================================
		// 1. 解析标识符 (Type Code)
		// ==========================================
		// 读取当前字节作为标识符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		// 判断是否为扩展标识符：如果当前字节是 ff，说明标识符占2个字节，需要向后多读一位
		if ("ff".equalsIgnoreCase(typeCode)) {
			index += 1; // 游标后移
			// 拼接扩展标识符，例如 ff + 01 = ff01
			typeCode += HexStringUtil.int2HexStr(bodyElementFrame[index]);
		}

		// ==========================================
		// 2. 解析数据定义 (Data Definition)
		// ==========================================
		// 标识符的下一位是数据定义字节，包含了数据长度和小数位信息
		char elementInfoChar = bodyElementFrame[index + 1];
		// 根据协议算法计算数据内容的字节长度
		int dataSize = getBodyElementByteSize(elementInfoChar);
		// 根据协议算法计算数据的小数位数
		int decimalSize = getBodyElementDecimalSize(elementInfoChar);

		// ==========================================
		// 3. 提取原始数据 (Original Data)
		// ==========================================
		// 数据起始位置：当前索引 + 1(标识符占位) + 1(数据定义占位) + 1(指向下一位)
		// 简化计算：当前 index 指向的是标识符最后一位，所以数据开始于 index + 2
		int dataStartIndex = index + 2;
		int dataEndIndex = index + 2 + dataSize - 1;

		// 截取 16 进制原始数据字符串
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, dataStartIndex, dataEndIndex);

		// ==========================================
		// 4. 数值转换与计算 (Value Calculation)
		// ==========================================
		BigDecimal finalVal = null;

		// 检查数据是否有效
		if (StrUtil.isNotBlank(originalData)) {
			// 4.1 判断是否为负数：根据协议，数据以 ff 开头表示负数
			boolean isNegative = StrUtil.startWithIgnoreCase(originalData, "ff");

			// 4.2 准备计算用的纯数字符串
			String calcData = originalData;
			if (isNegative) {
				// 如果是负数，去除开头的 ff 标识，保留后续数值部分
				calcData = StrUtil.subAfter(originalData, "ff", false);
			}

			// 4.3 处理小数点
			// 只有当数据长度足以插入小数点时才进行处理
			if (calcData.length() > decimalSize) {
				// 计算插入小数点的位置：总长度 - 小数位数
				int insertPos = calcData.length() - decimalSize;

				// 插入小数点，例如：12345, 小数位2 -> 123.45
				String numStr = new StringBuilder(calcData).insert(insertPos, ".").toString();
				// 去除末尾可能多余的小数点（防止出现 123. 的情况）
				numStr = StrUtil.removeSuffix(numStr, ".");

				// 4.4 转换为 BigDecimal
				if (NumberUtil.isNumber(numStr)) {
					finalVal = Convert.toBigDecimal(numStr);
					// 如果是负数标识，则取反
					if (isNegative) {
						finalVal = finalVal.negate();
					}
				}
			}
		}

		// ==========================================
		// 5. 封装对象并存储 (Build & Store)  【💡合并差异的核心部分】
		// ==========================================
		String typeName = "";
		// 根据传入的模式标志，决定去哪个枚举类里查找名称
		if (isParamMode) {
			// 参数模式
			FrameParamBodyElementEnum paramEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
			typeName = Opt.ofNullable(paramEnum)
				.map(FrameParamBodyElementEnum::getFormatElementName)
				.orElse("");
		} else {
			// 普通模式
			FrameBodyElementEnum bodyEnum = FrameBodyElementEnum.getByHexCode(typeCode);
			typeName = Opt.ofNullable(bodyEnum)
				.map(FrameBodyElementEnum::getFormatElementName)
				.orElse("");
		}

		// 构建结果对象
		HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
			.setTypeCode(typeCode)
			.setTypeName(typeName)
			.setIndex(0) // 单一属性默认索引为0
			.setDataSize(dataSize)
			.setDecimalSize(decimalSize)
			.setOriginalData(originalData)
			.setContentData(finalVal == null ? null : finalVal.toPlainString())
			.setVal(finalVal);

		// 将结果存入 Map，如果 key 不存在则创建新 List
		List<HexFrameBodyPropertiesMessage> elementList = elementMap.computeIfAbsent(typeCode, k -> new LinkedList<>());
		elementList.add(message);

		// ==========================================
		// 6. 更新游标索引 (Update Index)
		// ==========================================
		// 新索引 = 当前标识符位置 + 2 (1字节定义+1字节偏移) + 数据长度
		// 注意：这里的 +2 是指跳过 "数据定义字节" 和 "偏移量"
		index += (2 + dataSize);

		return index;
	}


	/**
	 * 处理中心站地址（01 标识符引导符）
	 * 4个字节，每个字节为一个中心站地址（hex进制）
	 */
	private static int special01ParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + 5);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "中心站地址";

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		String[] elementSplit = StrUtil.split(originalData, 2);

		for (int i = 0; i < elementSplit.length; i++) {
			String element = elementSplit[i];
			String el = new BigInteger(element, 16).toString(10);
			HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
				.setOriginalData(element)
				.setDecimalSize(0)
				.setDataSize(1)
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s_地址%d", elementName, i + 1))
				.setIndex(i)
				.setContentData(el)
				.setVal(new BigDecimal(el));

			messageList.add(message);
		}


		elementMap.put(typeCode, messageList);

		// 6个字节= 标识引导符（1个字节） + 数据定义（1个字节）+ 中心站地址内容（4个字节）
		return index + 6;
	}


	/**
	 * 设备属性（公司自定义标识引导符【12】）
	 * 例如报文12 30 00 01 08 09 60 99，说明如下：
	 * 30为数据定义，N(10)。
	 * 00，修改RS485-1波特率：00表示300bps，01表示600bps，02表示1200bps，03表示2400bps，04表示4800bps，05表示9600bps，06表示19200bps，07表示57600bps，08表示115200bps；
	 * 01，修改RS485-2波特率，定义同上
	 * 08，修改RS232-1波特率，定义同上
	 * 09，修改RS232-2波特率，定义同上
	 * 修改传感器稳定时间，可设置范围0~60，单位秒
	 * 99，4-20mA量程，可设置范围0~99
	 */
	private static int specialPriCompanyEx12ParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "设备属性";

		// 数据字节数 (理论上这里解析出来应该是 6 字节)
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 截取原始十六进制数据 (例如: 000108096099)
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// 波特率映射表 (索引 0~8 分别对应 300~115200)
		String[] baudRateMapping = {"300", "600", "1200", "2400", "4800", "9600", "19200", "57600", "115200"};
		// 定义这 6 个字节代表的具体属性名称
		String[] attributeNames = {
			"RS485-1波特率", "RS485-2波特率", "RS232-1波特率", "RS232-2波特率",
			"传感器稳定时间(秒)", "4-20mA量程"
		};

		// --- 开始逐字节拆分解析 ---
		if (StrUtil.isNotBlank(originalData)) {
			// 理论上有6个属性，循环处理防止由于厂家少发字节导致下标越界
			int maxAttributes = Math.min(6, originalData.length() / 2);

			for (int i = 0; i < maxAttributes; i++) {
				// 每次截取 1 个字节 (2 个 Hex 字符)
				String byteHex = originalData.substring(i * 2, i * 2 + 2);
				// 因为 SL651 协议通常是 BCD 码（且从 60、99 的取值范围看完全符合 BCD 码特性），直接按十进制解析
				int byteVal = Integer.parseInt(byteHex);

				String contentData = "";
				BigDecimal finalVal = null;

				// 前 4 个字节是波特率解析
				if (i < 4) {
					if (byteVal >= 0 && byteVal < baudRateMapping.length) {
						contentData = baudRateMapping[byteVal];
						finalVal = new BigDecimal(contentData);
					} else {
						contentData = "未知波特率代码(" + byteHex + ")";
						finalVal = new BigDecimal(byteVal); // 容错记录原始值
					}
				}
				// 第 5 个字节是传感器稳定时间，第 6 个字节是量程
				else {
					contentData = String.valueOf(byteVal);
					finalVal = new BigDecimal(byteVal);
				}

				// 构建拆分后的独立属性对象
				HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
					// 采用你之前设计的优秀思路，加入层级标识，例如 "12_0" 代表第一个属性
					.setTypeCode(String.format("%s_%d", typeCode.toUpperCase(), i))
					.setTypeName(elementName + "-" + attributeNames[i])
					.setIndex(i)
					.setDataSize(1) // 拆分后每个属性实际只占 1 字节
					.setOriginalData(byteHex)
					.setContentData(contentData)
					.setVal(finalVal);

				messageList.add(message);
			}
		}
		// ----------------------------------------

		// 外层 key 依然保留原有的标识符 "12"
		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}

	/**
	 * 中心站信道类型以及地址
	 * 信道类型在高位字节，地址在低位字节。
	 * 信道类型用1字节BCD码:1-短信，2-IPV4，3-北斗，4-海事卫星，5-PSTN，6-超短波。
	 * 中心站信道地址长度根据信道类型确定，其中IP型地址应包含地址及端口号，IP地址用6字节BCD码表示，省略“.”;端口号用3字节BCD码表示，紧接在地址之后
	 */
	private static int special04To0BParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);

		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 开始切割处理 originalData ---
		if (originalData.length() >= 2) {
			// 1. 获取信道类型 (1字节 BCD = 前2个十六进制字符)
			String channelTypeCode = originalData.substring(0, 2);
			// 2. 获取地址数据部分 (剩余的字符)
			String addressDataStr = originalData.substring(2);

			String channelTypeName = "";
			String contentData = "";

			switch (channelTypeCode) {
				case "01":
					channelTypeName = "短信";
					contentData = addressDataStr;
					break;
				case "02":
					channelTypeName = "IPV4";
					// IP地址用6字节BCD(12个字符), 端口号用3字节BCD(6个字符), 总共至少18个字符
					if (addressDataStr.length() >= 18) {
						// 截取IP部分
						String ipBcd = addressDataStr.substring(0, 12);
						// 截取端口部分
						String portBcd = addressDataStr.substring(12, 18);

						// 解析IP: 每3个字符表示一段，例如 "192168001001" -> 192.168.1.1
						String ip = Integer.parseInt(ipBcd.substring(0, 3)) + "." +
							Integer.parseInt(ipBcd.substring(3, 6)) + "." +
							Integer.parseInt(ipBcd.substring(6, 9)) + "." +
							Integer.parseInt(ipBcd.substring(9, 12));

						// 解析端口: "008080" -> 8080
						int port = Integer.parseInt(portBcd);

						contentData = ip + ":" + port;
					} else {
						// 数据长度异常时的容错
						contentData = addressDataStr;
					}
					break;
				case "03":
					channelTypeName = "北斗";
					contentData = addressDataStr;
					break;
				case "04":
					channelTypeName = "海事卫星";
					contentData = addressDataStr;
					break;
				case "05":
					channelTypeName = "PSTN";
					contentData = addressDataStr;
					break;
				case "06":
					channelTypeName = "超短波";
					contentData = addressDataStr;
					break;
				default:
					channelTypeName = "未知信道(" + channelTypeCode + ")";
					contentData = addressDataStr;
					break;
			}

			String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "中心站地址";

			// 构建消息对象 1：信道类型
			HexFrameBodyPropertiesMessage messageChannelType = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-信道类型(%s)", elementName, channelTypeName))
				.setDataSize(1)
				.setOriginalData(channelTypeCode)
				// 这里你用的是 channelTypeCode，如果想页面直接显示中文，也可以改成 channelTypeName
				.setContentData(channelTypeCode);

			// 构建消息对象 2：信道地址
			HexFrameBodyPropertiesMessage messageContentData = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-信道地址(%s)", elementName, channelTypeName))
				.setDataSize(elementDataSize - 1)
				.setOriginalData(addressDataStr)
				.setContentData(contentData);

			messageList.add(messageChannelType);
			messageList.add(messageContentData);
		}
		// --- 切割处理结束 ---

		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}

	/**
	 * 工作方式
	 * BCD码，1-自报工作状态;2-自报认工作状态;3-查询/应答工作状态;4-调试或维修状态
	 */
	private static int special0CParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);

		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		if (originalData.length() >= 2) {
			String workTypeName = "";
			switch (originalData) {
				case "01": {
					workTypeName = "自报工作状态";
					break;
				}
				case "02": {
					workTypeName = "自报确认工作状态";
					break;
				}
				case "03": {
					workTypeName = "查询/应答工作状态";
					break;
				}
				case "04": {
					workTypeName = "调试或维修状态";
					break;
				}
				default:
					workTypeName = "未知工作方式(" + originalData + ")";
					break;
			}


			// 构建消息对象 1：信道类型
			HexFrameBodyPropertiesMessage messageWorkType = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-工作方式(%s)", paramBodyElementEnum.getFormatElementName(), workTypeName))
				.setDataSize(1)
				.setOriginalData(originalData)
				.setContentData(originalData);
			messageList.add(messageWorkType);
		}

		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}


	/**
	 * 遥测站采集要素定义表 (从 A1 到 A8)
	 * 二维数组结构：第一维对应字节组 (A1~A8)，第二维对应位 (D0~D7，注意是从最低位到最高位)
	 */
	private static final String[][] TELEMETRY_ELEMENTS = {
		// A1: D0 -> D7
		{"气压", "地温", "湿度", "气温", "风速", "风向", "蒸发量", "降水量"},
		// A2: D0 -> D7
		{"水位1", "水位2", "水位3", "水位4", "水位5", "水位6", "水位7", "水位8"},
		// A3: D0 -> D7
		{"水压", "流量", "流速", "水量", "闸门开度", "波浪", "图片", "地下水埋深"},
		// A4: D0 -> D7
		{"水表1", "水表2", "水表3", "水表4", "水表5", "水表6", "水表7", "水表8"},
		// A5: D0 -> D7
		{"10cm 墒情", "20cm 墒情", "30cm 墒情", "40cm 墒情", "50cm 墒情", "60cm 墒情", "80cm 墒情", "100cm 墒情"},
		// A6: D0 -> D7
		{"水温", "氨氮", "高锰酸盐指数", "氧化还原电位", "浊度", "电导率", "溶解氧", "pH值"},
		// A7: D0 -> D7
		{"镉", "总汞", "砷", "硒", "锌", "总磷", "总氮", "总有机碳"},
		// A8: D0 -> D7 (D3~D7为空)
		{"铅", "铜", "叶绿素a", null, null, null, null, null}
	};

	/**
	 * 遥测站采集要素设置
	 * HEX码。要素对应数据位置“1”有效，置“0”无效
	 */
	private static int special0DParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "遥测站采集要素";

		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 开始解析 originalData (位图解析) ---
		if (StrUtil.isNotBlank(originalData)) {
			// 两个Hex字符代表1个字节 (1 Byte = 8 bits)
			int byteCount = originalData.length() / 2;
			int elementIndex = 0; // 用于记录当前解析出的要素序号

			for (int i = 0; i < byteCount; i++) {
				// 如果报文字节数超出了我们定义的A1~A8组，直接跳出防止越界
				if (i >= TELEMETRY_ELEMENTS.length) {
					break;
				}

				// 截取当前字节的 Hex 字符串，例如 "FF"
				String hexByte = originalData.substring(i * 2, i * 2 + 2);
				// 转换为十进制数字
				int byteVal = Integer.parseInt(hexByte, 16);

				// 循环检查该字节的 8 个 bit (D0 到 D7)
				for (int bit = 0; bit < 8; bit++) {
					// 1. 判断当前位是 1 还是 0
					boolean isActive = (byteVal & (1 << bit)) != 0;
					String bitStrValue = isActive ? "1" : "0";
					BigDecimal bitDecimalValue = isActive ? BigDecimal.ONE : BigDecimal.ZERO;

					// 2. 获取要素名称，如果是 null (未定义)，则给一个占位名称，带上位置信息方便排查
					String element = TELEMETRY_ELEMENTS[i][bit];
					if (element == null) {
						// 例如：未定义(A8-D3)
						element = String.format("未定义(A%d-D%d)", i + 1, bit);
					}

					// 3. 【核心改动】：无论 0、1 还是未定义，全部创建对象并占位
					HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
						.setTypeCode(String.format("%s_a%d_d%d", typeCode, i + 1,  bit))
						// 动态名称，例如："遥测站采集要素-降水量"
						.setTypeName(elementName + "-" + element)
						.setIndex(elementIndex++)
						.setDataSize(elementDataSize) // 整个要素块的总字节数
						// 记录整体的原始Hex，方便排查（如果你想只记录当前1个字节，也可以改成 hexByte）
						.setOriginalData(originalData)
						.setContentData(bitStrValue) // 存入 "1" 或 "0"
						.setVal(bitDecimalValue);    // 存入 BigDecimal.ONE 或 BigDecimal.ZERO

					messageList.add(message);
				}
			}
		}
		// ----------------------------------------

		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}

	/**
	 * 中继站(集合转发站)服务地址范围
	 * 服务地址应尽量连续。标识符引导符后的数据定义字节8Bit均用于表示后续数据的字节数
	 * 编码方法：起始地址1 站数 起始地址2 站数 起始地址3 站数.....
	 * 最多255字节BCD。“起始地址 站数”用6字节BCD表示，固定结构。站数表示从起始地址开始、具有连续站址的遥测站数目，用1字节BCD表示;站数为1时，服务地址即起始地址
	 */
	private static int special0EParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "中继站(集合转发站)服务地址范围";

		// 【重要注意】：根据协议说明“数据定义字节8Bit均用于表示后续数据的字节数”，
		// 这里如果你的 getBodyElementByteSize 内部包含右移截取逻辑(例如去除了小数位前3bit)，可能会导致长度计算错误！
		// 针对 0E 这种特殊协议，最安全的取法是直接把整个字节转十进制（如果你现有的方法没问题，可忽略这段注释）：
		// int elementDataSize = Integer.parseInt(HexStringUtil.int2HexStr(bodyElementFrame[index + 1]), 16);
		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 开始切分 originalData (6字节为一组) ---
		if (StrUtil.isNotBlank(originalData)) {
			// 固定结构：1组 = 6字节 = 12个十六进制字符
			int unitCharLength = 12;
			int unitCount = originalData.length() / unitCharLength;

			for (int i = 0; i < unitCount; i++) {
				// 1. 截取当前这一组的 12 个字符
				String unitHex = originalData.substring(i * unitCharLength, (i + 1) * unitCharLength);

				// 2. 提取 起始地址 (前5个字节 = 前10个字符)
				String startAddress = unitHex.substring(0, 10);

				// 3. 提取 站数 (最后1个字节 = 最后2个字符)
				String stationCountBcd = unitHex.substring(10, 12);
				// 因为是 BCD 码，形如 "05" 直接当作十进制数字解析即可
				int stationCount = Integer.parseInt(stationCountBcd);

				// 4. 组装展示内容，例如："起始地址:1234567890 (共5站)"
//				String contentData = String.format("起始地址:%s (共%d站)", startAddress, stationCount);

				// 5. 构建对象
				HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
					.setTypeCode(typeCode)
					// 动态名称，方便列表展示，例如 "中继站服务地址范围-地址段1"
					.setTypeName(elementName + "-地址段" + (i + 1))
					.setIndex(i)
					// 该段数据的实际大小：6字节
					.setDataSize(6)
					// 保存原始的这 12 个 Hex 字符
					.setOriginalData(unitHex)
					.setContentData(startAddress)
					// 可以把站数存进实际值，便于以后如果有业务计算可以直接取用
					.setVal(new BigDecimal(stationCount));

				messageList.add(message);
			}
		}
		// ----------------------------------------


		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}


	/**
	 * 遥测站通信设备识别号
	 * ASCII码。第1字节表示卡类型:1-移动通信卡，2-北斗卫星通信卡;紧跟在卡类型后的数据为卡识别号
	 */
	private static int special0FParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "通信设备识别号";

		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 开始解析 originalData (ASCII 转换) ---
		if (StrUtil.isNotBlank(originalData) && originalData.length() >= 2) {
			// 1. 解析卡类型 (第1个字节 = 前2个十六进制字符)
			String typeHex = originalData.substring(0, 2);
			String cardTypeName = "";
			String cardTypeStr = "";

			// 兼容性处理：协议规定是ASCII，所以字符 '1' 对应的 Hex 是 "31"，'2' 是 "32"。
			// 但有些不规范的厂家可能会直接发 Hex "01" / "02"，这里一并做兼容
			if ("31".equals(typeHex) || "01".equals(typeHex)) {
				cardTypeName = "移动通信卡";
				cardTypeStr = "1";
			} else if ("32".equals(typeHex) || "02".equals(typeHex)) {
				cardTypeName = "北斗卫星通信卡";
				cardTypeStr = "2";
			} else {
				cardTypeName = "未知卡类型";
				cardTypeStr = typeHex;
			}

			// 2. 解析卡识别号 (剩余的字节，转为 ASCII 字符串)
			String idHex = originalData.substring(2);
			StringBuilder idBuilder = new StringBuilder();

			// 遍历剩余的 Hex 字符串，每 2 个字符作为一个字节转换为 ASCII
			for (int i = 0; i < idHex.length(); i += 2) {
				String hexChar = idHex.substring(i, i + 2);
				// 将十六进制转为十进制整数，再强转为 char (ASCII)
				char c = (char) Integer.parseInt(hexChar, 16);

				// 过滤掉不可见的空字符 (很多设备凑不够长度会用 0x00 补齐)
				if (c != '\0') {
					idBuilder.append(c);
				}
			}

			// trim() 去除头尾可能存在的空格
			String cardId = idBuilder.toString().trim();

			// 3. 构建消息对象 1：卡类型
			HexFrameBodyPropertiesMessage typeMessage = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(elementName + "-卡类型(" + cardTypeName + ")")
				.setIndex(0)
				.setDataSize(1)
				.setOriginalData(typeHex)
				.setContentData(cardTypeStr);

			// 4. 构建消息对象 2：卡识别号
			HexFrameBodyPropertiesMessage idMessage = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(elementName + "-卡识别号")
				.setIndex(1)
				// 总数据大小减去卡类型占用的1个字节
				.setDataSize(elementDataSize - 1)
				.setOriginalData(idHex)
				.setContentData(cardId);

			messageList.add(typeMessage);
			messageList.add(idMessage);
		}
		// ----------------------------------------

		elementMap.put(typeCode, messageList);

		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}



	/**
	 * 解析时间标识符（f0）和测站编码标识符（f1）
	 */
	private static int specialF0AndF1BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + 6);

		FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(typeCode);

		HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(1)
			.setOriginalData(originalData)
			.setDecimalSize(5)
			.setTypeCode(typeCode)
			.setTypeName(
				Opt.ofNullable(frameBodyElementEnum)
					.map(FrameBodyElementEnum::getFormatElementName)
					.orElse("")
			)
			.setIndex(0)
			.setVal(null);
		elementMap.put(typeCode, Lists.newArrayList(hexFrameBodyPropertiesMessage));
		index += 7;
		return index;
	}



	/**
	 * 解析1小时内每5分钟时段的降雨量
	 */
	private static int specialF4BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String rain_in_an_hour_mark = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		// 一小时内每5分钟时段的降雨量数据定义（占的字节数以及小数位）
		char rain_in_an_hour_data_def_char = bodyElementFrame[++index];
		// 属性数据的字节数
		int rain_in_an_hour_data_size = getBodyElementByteSize(rain_in_an_hour_data_def_char);
		// 雨量原始数据
		String rain_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + rain_in_an_hour_data_size);

		// 标识符引导符
		FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(rain_in_an_hour_mark);

		String elementName = frameBodyElementEnum != null ? frameBodyElementEnum.getFormatElementName() : "1h内每5min时段雨量";

		// 一小时内每5分钟时段的降雨量
		List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Lists.newArrayList();
		String[] rain_in_an_hour_arrays = StrUtil.split(rain_in_an_hour_original_data, 2);
		for (int i = 0; i < rain_in_an_hour_arrays.length; i++) {
			String originalData = rain_in_an_hour_arrays[i];

			BigDecimal val = Opt.ofNullable(originalData)
				.filter(dec_data -> !StrUtil.containsAnyIgnoreCase(dec_data, "ff"))
				.map(dec_data -> {
					String bcd_data = StrUtil.padPre(new BigInteger(dec_data, 16).toString(10), 3, '0');
					String cutNumStr = StrUtil.removeSuffix(
						new StringBuilder(bcd_data).insert(bcd_data.length() - 1, StrUtil.DOT).toString()
						, StrUtil.DOT
					);
					return Convert.toBigDecimal(cutNumStr);
				})
				.orElse(null);
			HexFrameBodyPropertiesMessage propertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(1)
				.setOriginalData(originalData)
				.setDecimalSize(1)
				.setTypeCode(rain_in_an_hour_mark)
				.setTypeName(String.format("%s_时段%d", elementName, i + 1))
				.setIndex(i)
				.setContentData(val == null ? null : val.toPlainString())
				.setVal(val);
			hexFrameBodyPropertiesMessageList.add(propertiesMessage);
		}
		// 记录 一小时内每5分钟时段的降雨量（共12条）
		elementMap.put(rain_in_an_hour_mark, hexFrameBodyPropertiesMessageList);
		// 跳过当前标识符位以及12条每5分钟时段的降雨量（字节长度）
		index += 13;
		return index;
	}

	/**
	 * 解析1小时内每5分钟间隔的相对水位数据
	 */
	private static int specialF5TOFCBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 1小时内每5分钟间隔的相对水位标记
		String water_level_in_an_hour_mark = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		// 相对水位数据定义（占的字节数以及小数位）
		char water_level_in_an_hour_def_char = bodyElementFrame[++index];
		// 属性数据的字节数
		int water_level_in_an_hour_data_size = getBodyElementByteSize(water_level_in_an_hour_def_char);

		// 标识符引导符
		FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(water_level_in_an_hour_mark);
		String elementName = frameBodyElementEnum != null ? frameBodyElementEnum.getFormatElementName() : "1小时内每5分钟间隔的相对水位";

		// 1小时内每5分钟间隔的相对水位原始数据（字节数据）
		List<HexFrameBodyPropertiesMessage> frameBodyPropertiesMessageList =  Lists.newArrayList();

		String water_level_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + water_level_in_an_hour_data_size);
		String[] water_level_in_an_hour_original_data_arrays = StrUtil.split(water_level_in_an_hour_original_data, 4);

		for (int i = 0; i < water_level_in_an_hour_original_data_arrays.length; i++) {
			String originalData = water_level_in_an_hour_original_data_arrays[i];

			BigDecimal val = Opt.ofNullable(originalData)
				.filter(dec_data -> !StrUtil.containsAnyIgnoreCase(dec_data, "ffff"))
				.map(dec_data -> {
					String bcd_data = StrUtil.padPre(new BigInteger(dec_data, 16).toString(10), 5, '0');
					String cutNumStr = StrUtil.removeSuffix(
						new StringBuilder(bcd_data).insert(bcd_data.length() - 2, StrUtil.DOT).toString()
						, StrUtil.DOT
					);
					return Convert.toBigDecimal(cutNumStr);
				})
				.orElse(null);
			HexFrameBodyPropertiesMessage propertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(2)
				.setOriginalData(originalData)
				.setTypeCode(water_level_in_an_hour_mark)
				.setTypeName(String.format("%s_位%d", elementName, i + 1))
				.setIndex(i)
				.setDecimalSize(2)
				.setContentData(val == null ? null : val.toPlainString())
				.setVal(val);
			frameBodyPropertiesMessageList.add(propertiesMessage);
		}
		// 记录 1小时内每5分钟间隔的相对水位（共12条）
		elementMap.put(water_level_in_an_hour_mark, frameBodyPropertiesMessageList);
		// 跳过当前标识符位以及1小时内每5分钟间隔的相对水位（字节长度）
		index += 25;
		return index;
	}


	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getSoftwareVersionBodyPropertiesMessages(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "软件版本信息", (frame, map) -> {
			// 版本信息字节数(占一个字节数)
			String byte_count_hex = Integer.toHexString(frame[0]);
			int dataSize = Integer.parseInt(byte_count_hex, 16);

			// 版本信息，跳过[版本信息字节数(占一个字节数)]
			String version_original_data = HexStringUtil.int2HexStr(frame, 1, frame.length-1);
			String version = HexStringUtil.hexToUtf8(version_original_data);

			// 构建对象
			FrameParamBodyElementEnum softwareVersion = FrameParamBodyElementEnum.el_param_ex_ff00;
			HexFrameBodyPropertiesMessage bodyPropertiesMessage = new HexFrameBodyPropertiesMessage()
				.setTypeCode(softwareVersion.getHexCode())
				.setTypeName(softwareVersion.getElementName())
				.setOriginalData(version_original_data)
				.setDataSize(dataSize)
				.setContentData(version);

			map.put(softwareVersion.getHexCode(), Lists.newArrayList(bodyPropertiesMessage));
		});
	}

	/**
	 * 解析图片报 (标准协议)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getStandardImgBodyPropertiesMessages(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "标准图片解析", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				String mark = Integer.toHexString(frame[index]);
				if ("f3".equals(mark)) {
					index = specialStandardF3BodyPropertiesMessages(frame, index, map);
					// 标准F3通常读取到末尾
					continue;
				}
				index = parseSinglePropertiesMessages(frame, index, map, false);
			}
		});
	}



	/**
	 * 解析图片报 (湖南协议)
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHunanImgBodyPropertiesMessages(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "湖南省图片解析", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				String mark = Integer.toHexString(frame[index]);
				if ("f3".equals(mark)) {
					index = specialHunanF3BodyPropertiesMessages(frame, index, map);
					continue; // 必须 continue
				}
				index = parseSinglePropertiesMessages(frame, index, map, false);
			}
		});
	}

	/**
	 * E3遥测站上发升级结果 (定制公司协议)
	 * 仅包含正文部分，如 FF07 和 FF00 报文
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getCustomCompanyE3BodyPropertiesMessages(char[] bodyElementFrame) {
		return parseWithCatch(bodyElementFrame, "定制公司E3解析", (frame, map) -> {
			for (int index = 0; index < frame.length;) {
				// 1. 读取当前标识符
				String mark = HexStringUtil.int2HexStr(frame[index]).toLowerCase();
				// 如果遇到 ff，说明是两字节的扩展标识符，拼接下一位
				if ("ff".equals(mark) && index + 1 < frame.length) {
					mark += HexStringUtil.int2HexStr(frame[index + 1]).toLowerCase();
				}
				// 2. 根据标识符分发解析逻辑
				switch (mark) {
					case "ff07": {
						index = specialCustomCompanyE3FF07Messages(frame, index, map);
						break;
					}
					case "ff00": {
						index = specialCustomCompanyE3FF00Messages(frame, index, map);
						break;
					}
					default: {
						// 兜底逻辑：如果存在其它标准报文，走通用单属性解析
						index = parseSinglePropertiesMessages(frame, index, map, false);
						break;
					}
				}
			}
		});
	}

	/**
	 * 解析 定制公司E3协议 FF07 升级结果
	 * 结构: 标识符(FF07, 2字节) + 数据定义(1字节) + 升级结果(1字节HEX) + 日志报文内容(剩余字节)
	 */
	private static int specialCustomCompanyE3FF07Messages(char[] frame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> map) {
		String typeCode = "ff07";

		// 获取数据定义字节 (ff占1位, 07占1位, 所以数据定义在 index + 2)
		char elementInfoChar = frame[index + 2];
		// 复用原有的获取长度方法
		int dataSize = getBodyElementByteSize(elementInfoChar);

		// 数据截取起止位置
		int dataStartIndex = index + 3;
		int dataEndIndex = dataStartIndex + dataSize - 1;

		String originalData = "";
		if (dataSize > 0 && dataEndIndex < frame.length) {
			originalData = HexStringUtil.int2HexStr(frame, dataStartIndex, dataEndIndex);
		}

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "升级结果";

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 1. 业务数据解析：升级结果 (占用1个字节，即前2个十六进制字符) ---
		String result = originalData.length() >= 2 ? originalData.substring(0, 2) : originalData;
		String resultStr = "11".equals(result) ? "升级成功" : ("00".equals(result) ? "升级失败" : "未知结果(" + result + ")");

		HexFrameBodyPropertiesMessage resultMsg = new HexFrameBodyPropertiesMessage()
			.setTypeCode(typeCode)
			.setTypeName(elementName + "-状态")
			.setIndex(0)
			.setDataSize(1) // 升级状态固定占 1 个字节
			.setOriginalData(result)
			.setContentData(resultStr)
			// 容错处理：确保转 BigDecimal 时如果出现异常字符不会报错
			.setVal(NumberUtil.isNumber(result) ? new BigDecimal(result) : null);

		messageList.add(resultMsg);

		// --- 2. 业务数据解析：日志内容 (如果数据长度大于2个字符，说明存在日志部分) ---
		if (originalData.length() > 2) {
			String logHex = originalData.substring(2);
			String logContent = "";

			if ("ff".equalsIgnoreCase(logHex)) {
				logContent = "无错误日志";
			} else {
				// 将失败原因的 HEX 转成 UTF-8 可读字符串
				logContent = HexStringUtil.hexToUtf8(logHex);
			}

			HexFrameBodyPropertiesMessage logMsg = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(elementName + "-日志")
				.setIndex(1)
				.setDataSize(dataSize - 1) // 总长度减去状态的 1 个字节
				.setOriginalData(logHex)
				.setContentData(logContent);

			messageList.add(logMsg);
		}

		map.put(typeCode, messageList);

		// 更新游标: 标识符(2字节) + 数据定义(1字节) + 数据长度(dataSize)
		return index + 3 + dataSize;
	}

	/**
	 * 解析 定制公司E3协议 FF00 软件版本信息
	 * 结构: 标识符(FF00, 2字节) + 数据定义(1字节) + 软件版本字符串(ASCII)
	 */
	private static int specialCustomCompanyE3FF00Messages(char[] frame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> map) {
		String typeCode = "ff00";

		// 获取数据定义字节
		char elementInfoChar = frame[index + 2];
		// 复用原有的获取长度方法
		int dataSize = getBodyElementByteSize(elementInfoChar);

		int dataStartIndex = index + 3;
		int dataEndIndex = dataStartIndex + dataSize - 1;

		String originalData = "";
		String versionInfo = "";
		if (dataSize > 0 && dataEndIndex < frame.length) {
			originalData = HexStringUtil.int2HexStr(frame, dataStartIndex, dataEndIndex);
			// 解析版本号字符串
			versionInfo = HexStringUtil.hexToUtf8(originalData);
		}

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);
		String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "遥测站软件版本";

		HexFrameBodyPropertiesMessage msg = new HexFrameBodyPropertiesMessage()
			.setTypeCode(typeCode)
			.setTypeName(elementName)
			.setOriginalData(originalData)
			.setDataSize(dataSize)
			.setContentData(versionInfo);

		map.put(typeCode, Lists.newArrayList(msg));

		// 更新游标: 标识符(2) + 数据定义(1) + 数据长度(dataSize)
		return index + 3 + dataSize;
	}


	/**
	 * 处理标准 F3 逻辑
	 * F3 (Tag) + F3 (Fixed Data Def) + All Data
	 */
	private static int specialStandardF3BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String img_mark = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		// 标准协议：F3 后一位固定是数据定义 F3H，数据从 index+2 开始直到结束
		int dataStartIndex = index + 2;

		if (dataStartIndex > bodyElementFrame.length) {
			dataStartIndex = bodyElementFrame.length;
		}
		int img_data_length = bodyElementFrame.length - dataStartIndex;

		// 标识符引导
		FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(img_mark);

		// 图片数据
		String img_data_original_data = "";
		if (img_data_length > 0) {
			img_data_original_data = HexStringUtil.int2HexStr(bodyElementFrame, dataStartIndex, bodyElementFrame.length);
		}

		HexFrameBodyPropertiesMessage bodyPropertiesMessage = new HexFrameBodyPropertiesMessage()
			.setTypeCode(img_mark)
			.setTypeName(
				Opt.ofNullable(frameBodyElementEnum)
					.map(FrameBodyElementEnum::getFormatElementName)
					.orElse("图片数据")
			)
			.setOriginalData(img_data_original_data)
			.setDataSize(img_data_length)
			.setContentData(HexStringUtil.hexToUtf8(img_data_original_data));

		elementMap.put(img_mark, Lists.newArrayList(bodyPropertiesMessage));

		// 返回数组长度，表示处理完毕
		return bodyElementFrame.length;
	}

	/**
	 * 处理中心站地址（FF03 标识符引导符）
	 * 4个字节，每个字节为一个中心站地址（hex进制）
	 */
	private static int specialFF03ParamBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 标识引导符
		String typeCode = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		if ("ff".equalsIgnoreCase(typeCode)) {
			index += 1; // 游标后移
			// 拼接扩展标识符，例如 ff + 01 = ff01
			typeCode += HexStringUtil.int2HexStr(bodyElementFrame[index]);
		}

		FrameParamBodyElementEnum paramBodyElementEnum = FrameParamBodyElementEnum.getByHexCode(typeCode);

		// 数据字节数
		int elementDataSize = getBodyElementByteSize(bodyElementFrame[index + 1]);

		// 取截取长度对应的结束下标是 index + 2 + elementDataSize - 1 = index + elementDataSize + 1
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + elementDataSize + 1);

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// --- 开始切割处理 originalData ---
		if (originalData.length() >= 2) {
			// 获取设备接口编号-1字节
			String interfaceCode = originalData.substring(0, 2);
			// 获取设备地址-1字节
			String deviceAddress = originalData.substring(2, 4);
			// 获取传感器功能码-1字节
			String sensorFunctionCode = originalData.substring(4, 6);
			// 获取起始寄存器地址-2字节
			String beginRegisterAddress = originalData.substring(6, 10);
			// 获取寄存器数量-2字节
			String registerCount = originalData.substring(10, 14);
			// 返回数据起始位置-1字节
			String backDataPosition = originalData.substring(14, 16);
			// 返回数据类型-1字节
			String backDataType = originalData.substring(16, 18);
			// 返回数据除数-2字节
			String backDataDivisor = originalData.substring(18, 22);

			// 翻译水位计接口
			String interfaceCodeCN = "";
			switch (interfaceCode) {
				case "01":
					interfaceCodeCN = "RS485-1";
					break;
				case "02":
					interfaceCodeCN = "RS485-2";
					break;
				case "03":
					interfaceCodeCN = "RS232-1";
					break;
				case "04":
					interfaceCodeCN = "RS232-2";
					break;
				default:
					interfaceCodeCN = "未知设备接口编号(" + interfaceCode + ")";
					break;
			}

			String elementName = paramBodyElementEnum != null ? paramBodyElementEnum.getFormatElementName() : "水位采集信息";

			// 构建消息对象 1：水位计地址
			HexFrameBodyPropertiesMessage messageDeviceAddress = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-水位计地址(%s)", elementName, deviceAddress))
				.setDataSize(1)
				.setOriginalData(deviceAddress)
				.setContentData(deviceAddress);
			// 构建消息对象 2：水位计接口
			HexFrameBodyPropertiesMessage messageInterfaceCode = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-水位计接口(%s)", elementName, interfaceCodeCN))
				.setDataSize(1)
				.setOriginalData(interfaceCode)
				// 这里你用的是 interfaceCode，如果想页面直接显示中文，也可以改成 interfaceCodeCN
				.setContentData(interfaceCode);
			// 构建消息对象 3：传感器功能码
			HexFrameBodyPropertiesMessage messageSensorFunctionCode = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-传感器功能码(%s)", elementName, sensorFunctionCode))
				.setDataSize(1)
				.setOriginalData(sensorFunctionCode)
				.setContentData(sensorFunctionCode);
			// 构建消息对象 4：起始寄存器地址
			HexFrameBodyPropertiesMessage messageBeginRegisterAddress = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-起始寄存器地址(%s)", elementName, beginRegisterAddress))
				.setDataSize(2)
				.setOriginalData(beginRegisterAddress)
				.setContentData(beginRegisterAddress);
			// 构建消息对象 5：寄存器数量
			HexFrameBodyPropertiesMessage messageRegisterCount = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-寄存器数量(%s)", elementName, registerCount))
				.setDataSize(2)
				.setOriginalData(registerCount)
				.setContentData(registerCount);
			// 构建消息对象 6：返回数据起始位置
			HexFrameBodyPropertiesMessage messageBackDataPosition = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-返回数据起始位置(%s)", elementName, backDataPosition))
				.setDataSize(1)
				.setOriginalData(backDataPosition)
				.setContentData(backDataPosition);
			// 构建消息对象 7：返回数据类型
			HexFrameBodyPropertiesMessage messageBackDataType = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-返回数据类型(%s)", elementName, backDataType))
				.setDataSize(1)
				.setOriginalData(backDataType)
				.setContentData(backDataType);
			// 构建消息对象 8：返回数据除数
			HexFrameBodyPropertiesMessage messageBackDataDivisor = new HexFrameBodyPropertiesMessage()
				.setTypeCode(typeCode)
				.setTypeName(String.format("%s-返回数据除数(%s)", elementName, backDataDivisor))
				.setDataSize(1)
				.setOriginalData(backDataDivisor)
				.setContentData(backDataDivisor);

			messageList.add(messageDeviceAddress);
			messageList.add(messageInterfaceCode);
			messageList.add(messageSensorFunctionCode);
			messageList.add(messageBeginRegisterAddress);
			messageList.add(messageRegisterCount);
			messageList.add(messageBackDataPosition);
			messageList.add(messageBackDataType);
			messageList.add(messageBackDataDivisor);

		}

		// --- 切割处理结束 ---
		elementMap.put(typeCode, messageList);
		// 偏移字节 = 标识引导符(1字节) + 数据定义(1字节) + 数据字节数
		return index + elementDataSize + 2;
	}

	/**
	 * 处理湖南 F3 逻辑
	 * F3 (Tag) + Length (1 Byte) + Data (Length Bytes)
	 */
	private static int specialHunanF3BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String img_mark = HexStringUtil.int2HexStr(bodyElementFrame[index]);

		// 湖南协议：F3 后一位是长度
		char img_data_length_def_char = bodyElementFrame[++index];
		String img_data_length_def = HexStringUtil.int2HexStr(img_data_length_def_char);
		int img_data_length = new BigInteger(img_data_length_def, 16).intValue();

		// 标识符引导
		FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(img_mark);

		// 图片数据 start: index + 1, end: index + 1 + length
		// 此时 index 指向长度字节
		int dataStartIndex = index + 1;
		int dataEndIndex = dataStartIndex + img_data_length;

		// 边界保护
		if (dataEndIndex > bodyElementFrame.length) {
			dataEndIndex = bodyElementFrame.length;
		}

		String img_data_original_data = HexStringUtil.int2HexStr(bodyElementFrame, dataStartIndex, dataEndIndex);

		HexFrameBodyPropertiesMessage bodyPropertiesMessage = new HexFrameBodyPropertiesMessage()
			.setTypeCode(img_mark)
			.setTypeName(
				Opt.ofNullable(frameBodyElementEnum)
					.map(FrameBodyElementEnum::getFormatElementName)
					.orElse("图片数据")
			)
			.setOriginalData(img_data_original_data)
			.setDataSize(img_data_length)
			.setContentData(HexStringUtil.hexToUtf8(img_data_original_data));

		elementMap.put(img_mark, Lists.newArrayList(bodyPropertiesMessage));

		// 返回数据结束后的索引
		return dataEndIndex;
	}


	/**
	 * 解析湖南省报文里特殊的正文内容
	 */
	private static int huNanBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String ffMark = HexStringUtil.int2HexStr(bodyElementFrame[index]);
		String safeMark = ffMark + HexStringUtil.int2HexStr(bodyElementFrame[index + 1]);
		FrameBodyElementEnum.HunanBodyElementEnum hunanBodyElementEnum = FrameBodyElementEnum.HunanBodyElementEnum.getByHexCode(safeMark);

		// 如果无法识别该标识符，则按单一属性值处理
		if (hunanBodyElementEnum == null || hunanBodyElementEnum == FrameBodyElementEnum.HunanBodyElementEnum.el_hunan_ex) {
			return parseSinglePropertiesMessages(bodyElementFrame, index, elementMap, false);
		}

		// 跳过ff标识符索引
		++index;
		// 数据定义（占的字节数以及小数位）
		char defChar = bodyElementFrame[++index];
		// 属性数据的字节数
		int dataSize = getBodyElementByteSize(defChar);
		// 原始数据
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + dataSize);

		// 根据不同类型解析数据
		List<HexFrameBodyPropertiesMessage> messageList = parseHunanData(
			hunanBodyElementEnum,
			originalData
		);

		elementMap.put(safeMark, messageList);
		return index + dataSize + 1;
	}

	/**
	 * 解析湖南省数据的统一方法
	 */
	private static List<HexFrameBodyPropertiesMessage> parseHunanData(
		FrameBodyElementEnum.HunanBodyElementEnum elementEnum,
		String originalData) {

		List<HexFrameBodyPropertiesMessage> messageList = Lists.newArrayList();

		// 配置参数：每组字节数、小数位、是否支持负数
		ParseConfig config = getParseConfig(elementEnum);

		// 分割数据
		String[] dataArrays = StrUtil.split(originalData, config.groupByteSize * 2);

		for (int i = 0; i < dataArrays.length; i++) {
			String data = dataArrays[i];
			HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
				.setDataSize(config.groupByteSize)
				.setOriginalData(data)
				.setTypeCode(elementEnum.getHexCode())
				.setTypeName(elementEnum.getFormatElementName())
				.setIndex(i)
				.setDecimalSize(config.decimalSize);

			// 根据是否需要计算值来设置val
			if (config.needCalculateValue) {
				BigDecimal val = calculateValue(data, config);
				message.setVal(val)
					.setContentData(val == null ? null : val.toPlainString());
			}

			messageList.add(message);
		}

		return messageList;
	}

	/**
	 * 计算数值
	 */
	private static BigDecimal calculateValue(String data, ParseConfig config) {
		// 处理负数情况
		boolean isNegative = config.supportNegative && StrUtil.startWithIgnoreCase(data, "ff");
		String valueData = isNegative ? StrUtil.removePreAndLowerFirst(data, 2) : data;

		// 插入小数点
		if (config.decimalSize > 0) {
			int insertPos = valueData.length() - config.decimalSize;
			valueData = new StringBuilder(valueData)
				.insert(insertPos, StrUtil.DOT)
				.toString();
			valueData = StrUtil.removeSuffix(valueData, StrUtil.DOT);
		}

		// 转换为数值
		if (!NumberUtil.isNumber(valueData)) {
			return null;
		}

		BigDecimal result = Convert.toBigDecimal(valueData);
		return isNegative ? result.negate() : result;
	}

	/**
	 * 获取解析配置
	 */
	private static ParseConfig getParseConfig(FrameBodyElementEnum.HunanBodyElementEnum elementEnum) {
		switch (elementEnum) {
			case el_hunan_ff11:  // 6个渗压监测点编号。每个编号为4字节BCD码，一共24字节，如果编号为(00000000)标识无效，后面对应的数据值不需要解析
			case el_hunan_ff12:  // 2个渗流监测点编号。每个编号为4字节BCD码，一共8字节，如果编号为(00000000)标识无效，后面对应的数据值不需要解析
			case el_hunan_ff13:  // 2个位移监测点编号。每个编号为4字节BCD码，一共8字节，如果编号为(00000000)标识无效，后面对应的数据值不需要解析
				return new ParseConfig(4, 0, false, false);

			case el_hunan_ff14:  // 6个渗压水位数据 N(7,3)。每4字节为一组，3位小数，277.301米
			case el_hunan_ff15:  // 2个渗流监测点采集值 N(7,3)。每4字节为一组，3位小数，3位小数，单位为L/秒
			case el_hunan_ff21:  // 6个垂直高程 N(7,3)。4字节为一组
				return new ParseConfig(4, 3, false, true);

			case el_hunan_ff16:  // 2个水平X位移 N(8,2)。5字节为一组，十进制浮点数，2位小数,最高位为FF标识是负数
			case el_hunan_ff17:  // 2个水平Y位移 N(8,2)。5字节为一组，十进制浮点数，2位小数,最高位为FF标识是负数
			case el_hunan_ff18:  // 2个垂直位移 N(8,2)。5字节为一组，十进制浮点数，2位小数,最高位为FF标识是负数
				return new ParseConfig(5, 2, true, true);

			case el_hunan_ff19:  // 6个经度坐标 N(9,6)。5字节为一组
				return new ParseConfig(5, 6, false, true);

			case el_hunan_ff20:  // 6个纬度坐标 N(8,6)。 4字节为一组
				return new ParseConfig(4, 6, false, true);

			default:
				return new ParseConfig(0, 0, false, false);
		}
	}

	/**
	 * 解析配置类
	 */
	private static class ParseConfig {
		final int groupByteSize;      // 每组字节数
		final int decimalSize;         // 小数位数
		final boolean supportNegative; // 是否支持负数
		final boolean needCalculateValue; // 是否需要计算值

		ParseConfig(int groupByteSize, int decimalSize, boolean supportNegative, boolean needCalculateValue) {
			this.groupByteSize = groupByteSize;
			this.decimalSize = decimalSize;
			this.supportNegative = supportNegative;
			this.needCalculateValue = needCalculateValue;
		}
	}



}
