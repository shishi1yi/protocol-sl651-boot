package cc.shaoyi.sl651.common.utils;


import cc.shaoyi.sl651.common.constant.CommonConstant;
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
import cn.hutool.log.StaticLog;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
     */
    public static boolean isM3Mode(char[] frame) {
        return CommonConstant.HEADER_START_BODY_M3_FLAG_HEX.equalsIgnoreCase(HexStringUtil.int2HexStr(frame[13]));
    }

    /**
     * 获取中心站地址
     */
    public static String getHeaderHubAddress(char[] frame) {
        return HexStringUtil.int2HexStr(frame[2]);
    }

    /**
     * 获取遥测站地址
     */
    public static String getHeaderDetectAddress(char[] frame) {
        return HexStringUtil.int2HexStr(frame, 3, 7);
    }

    /**
     * 获取密码
     */
    public static String getHeaderPwd(char[] frame) {
        return HexStringUtil.int2HexStr(frame, 8, 9);
    }

    /**
     * 获取功能码
     */
    public static String getHeaderCommandCode(char[] frame) {
        return HexStringUtil.int2HexStr(frame[10]);
    }

    /**
     * 获取报文正文长度
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
		StaticLog.info("crc16Modbus算出的值：{}" , crc16CodeHexValue);
		String crc16Code = StrUtil.padPre(crc16CodeHexValue, 4, '0');
		StaticLog.info("原始CRC16校验码：{}" , originalCRC16Code);
		StaticLog.info("补全计算得出CRC16校验码：{}" , crc16Code);
		return StrUtil.equalsIgnoreCase(originalCRC16Code, crc16Code);
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
	 * 处理定时报报文正文内容
	 * @param bodyElementFrame
	 * @return
	 */
    public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getRegularReportBodyProperties(char[] bodyElementFrame) {
		LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();
		if (ArrayUtil.isNotEmpty(bodyElementFrame)) {
			for (int index = 0; index < bodyElementFrame.length;) {
				index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
			}
		}
        return elementMap;
    }


	/**
	 * 处理小时报报文正文内容
	 * @param bodyElementFrame
	 * @return
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHourReportBodyProperties(char[] bodyElementFrame) {
		LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();
		if (ArrayUtil.isNotEmpty(bodyElementFrame)) {
			// 每组数据，前两字节为标识符，其中第一个字节为标识引导符，第二个字节定义数据信息
			for (int index = 0; index < bodyElementFrame.length;) {
				String mark = Integer.toHexString(bodyElementFrame[index]);
				switch (mark) {
					case "f0" :
					case "f1" : {
						// 处理出现在正文标识符中 时间标识符（f0）、测站编码标识符（f1）
						index = specialF0AndF1BodyPropertiesMessages(bodyElementFrame, index, elementMap);
						break;
					}
					case "f4" : {
						// 1小时内每5分钟时段的降雨量
						index = specialF4BodyPropertiesMessages(bodyElementFrame, index, elementMap);
						break;
					}
					case "f5" :
					case "f6" :
					case "f7" :
					case "f8" :
					case "f9" :
					case "fa" :
					case "fb" :
					case "fc" : {
						// 处理1小时内每5分钟间隔的相对水位数据
						index = specialF5TOFCBodyPropertiesMessages(bodyElementFrame, index, elementMap);
						break;
					}
					default: {
						index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
					}
				}

			}
		}
		return elementMap;
	}

	/**
	 * 处理小时报报文正文内容 （湖南省解析规则）
	 * @param bodyElementFrame
	 * @return
	 */
	public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHourReportHuNanBodyProperties(char[] bodyElementFrame) {
		LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();
		if (ArrayUtil.isNotEmpty(bodyElementFrame)) {
			// 每组数据，前两字节为标识符，其中第一个字节为标识引导符，第二个字节定义数据信息
			for (int index = 0; index < bodyElementFrame.length;) {
				String mark = Integer.toHexString(bodyElementFrame[index]);
				try {
					switch (mark) {
						case "f0" :
						case "f1" : {
							// 处理出现在正文标识符中 时间标识符（f0）、测站编码标识符（f1）
							index = specialF0AndF1BodyPropertiesMessages(bodyElementFrame, index, elementMap);
							break;
						}
						case "f4" : {
							// 1小时内每5分钟时段的降雨量
							index = specialF4BodyPropertiesMessages(bodyElementFrame, index, elementMap);
							break;
						}
						case "f5" :
						case "f6" :
						case "f7" :
						case "f8" :
						case "f9" :
						case "fa" :
						case "fb" :
						case "fc" : {
							// 处理1小时内每5分钟间隔的相对水位数据
							index = specialF5TOFCBodyPropertiesMessages(bodyElementFrame, index, elementMap);
							break;
						}
						case "ff": {
							/**
							 * 处理安全监测设备数据
							 * 安全监测标识码都是2个字节（4个字符），且都是以ff开头
							 */
							index = huNanBodyPropertiesMessages(bodyElementFrame, index, elementMap);
							break;
						}
						default: {
							index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
						}
					}
				} catch (Exception e) {
					log.info(
						"湖南省解析规则异常，报文正文内容：{}, 已解析结果：{}",
						HexStringUtil.int2HexStr(bodyElementFrame, 0, bodyElementFrame.length -1),
						JSONUtil.toJsonStr(elementMap)
					);
					throw new RuntimeException(e);
				}

			}
			huNanElementInvalid(elementMap);
		}
		return elementMap;
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
	 * 解析单一属性值
	 */
	private static int singleBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		// 每组数据，前两字节为标识符，其中第一个字节为标识引导符，第二个字节定义数据信息
		String typeCode = Integer.toHexString(bodyElementFrame[index]);
		// 判断是否为扩展字段，如果读取到的一个字节内容为ff，则为扩展字段，需要往后再读一个字节
		if ("ff".equals(typeCode)) {
			index += 1;
			typeCode += HexStringUtil.int2HexStr(bodyElementFrame[index]);
		}
		char elementInfoChar = bodyElementFrame[index + 1];
		List<HexFrameBodyPropertiesMessage> elementList = elementMap.get(typeCode);
		if (CollUtil.isEmpty(elementList)) {
			elementList = new LinkedList<>();
			elementMap.put(typeCode, elementList);
		}
		// 是否为负数的flag
		AtomicBoolean negativeNumFlag = new AtomicBoolean(false);
		// 属性数据的字节数
		AtomicInteger dataSize = new AtomicInteger(getBodyElementByteSize(elementInfoChar));
		// 属性数据的小数位
		int decimalSize = getBodyElementDecimalSize(elementInfoChar);
		int tempIndex = index;
		String originalData = Opt.of(HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + 2))
			.filter(firstFrame -> StrUtil.equalsIgnoreCase(firstFrame, "ff")) // 判断是否为ff开头，是则为负数
			.map(firstFrame -> {
				negativeNumFlag.set(true);
				//dataSize.getAndIncrement(); 因为ff 占了一个字节，但是在报文里没有体现，所以自增1
				return HexStringUtil.int2HexStr(bodyElementFrame, tempIndex + 2, tempIndex + 2 + dataSize.get() - 1);
			})
			.orElseGet(() -> HexStringUtil.int2HexStr(bodyElementFrame, tempIndex + 2, tempIndex + 2 + dataSize.get() - 1));
		HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setTypeCode(typeCode)
			.setDataSize(dataSize.get())
			.setDecimalSize(decimalSize)
			.setOriginalData(HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + 2 + dataSize.get() - 1))
			.setVal(Opt.of(originalData)
				.filter(_originalData -> negativeNumFlag.get())
				.map(_originalData -> {
					String subOriginalData = StrUtil.subAfter(_originalData, "ff", false);
					String cutNumStr = StrUtil.removeSuffix(new StringBuilder(subOriginalData).insert((dataSize.get() - 1) * 2 - decimalSize, ".").toString(), StrUtil.DOT);
					return NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(StrUtil.removeSuffix(cutNumStr, StrUtil.DOT)).negate() : null;
				})
				.orElseGet(() -> {
					String cutNumStr = StrUtil.removeSuffix(new StringBuilder(originalData).insert(dataSize.get() * 2 - decimalSize, ".").toString(), StrUtil.DOT);
					return NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(StrUtil.removeSuffix(cutNumStr, StrUtil.DOT)) : null;
				}));
		elementList.add(hexFrameBodyPropertiesMessage);
		index += (2 + hexFrameBodyPropertiesMessage.getDataSize());
		return index;
	}

	/**
	 * 解析时间标识符（f0）和测站编码标识符（f1）
	 */
	private static int specialF0AndF1BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String typeCode = Integer.toHexString(bodyElementFrame[index]);
		String originalData = HexStringUtil.int2HexStr(bodyElementFrame, index + 2, index + 6);
		HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(1)
			.setOriginalData(originalData)
			.setDecimalSize(5)
			.setTypeCode(typeCode)
			.setVal(null);
		elementMap.put(typeCode, Lists.newArrayList(hexFrameBodyPropertiesMessage));
		index += 7;
		return index;
	}


	/**
	 * 解析1小时内每5分钟时段的降雨量
	 */
	private static int specialF4BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String rain_in_an_hour_mark = Integer.toHexString(bodyElementFrame[index]);
		// 一小时内每5分钟时段的降雨量数据定义（占的字节数以及小数位）
		char rain_in_an_hour_data_def_char = bodyElementFrame[++index];
		// 属性数据的字节数
		int rain_in_an_hour_data_size = getBodyElementByteSize(rain_in_an_hour_data_def_char);
		// 雨量原始数据
		String rain_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + rain_in_an_hour_data_size);
		// 一小时内每5分钟时段的降雨量
		List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(rain_in_an_hour_original_data, 2))
			.map(originalData -> {
				return new HexFrameBodyPropertiesMessage().setDataSize(1)
					.setOriginalData(originalData)
					.setDecimalSize(1)
					.setTypeCode(null)
					.setVal(
						Opt.ofNullable(originalData)
							.filter(dec_data -> !StrUtil.containsAnyIgnoreCase(dec_data, "ff"))
							.map(dec_data -> {
								String bcd_data = StrUtil.padPre(new BigInteger(dec_data, 16).toString(10), 3, '0');
								String cutNumStr = StrUtil.removeSuffix(
									new StringBuilder(bcd_data).insert(bcd_data.length() - 1, StrUtil.DOT).toString()
									, StrUtil.DOT
								);
								return Convert.toBigDecimal(cutNumStr);
							})
							.orElse(null)
					);
			})
			.collect(Collectors.toList());
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
		String water_level_in_an_hour_mark = Integer.toHexString(bodyElementFrame[index]);
		// 相对水位数据定义（占的字节数以及小数位）
		char water_level_in_an_hour_def_char = bodyElementFrame[++index];
		// 属性数据的字节数
		int water_level_in_an_hour_data_size = getBodyElementByteSize(water_level_in_an_hour_def_char);
		// 1小时内每5分钟间隔的相对水位原始数据（字节数据）
		String water_level_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + water_level_in_an_hour_data_size);
		List<HexFrameBodyPropertiesMessage> frameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(water_level_in_an_hour_original_data, 4))
			.map(originalData -> {
				return new HexFrameBodyPropertiesMessage().setDataSize(2)
					.setOriginalData(originalData)
					.setTypeCode(null)
					.setDecimalSize(2)
					.setVal(
						Opt.ofNullable(originalData)
							.filter(dec_data -> !StrUtil.containsAnyIgnoreCase(dec_data, "ffff"))
							.map(dec_data -> {
								String bcd_data = StrUtil.padPre(new BigInteger(dec_data, 16).toString(10), 5, '0');
								String cutNumStr = StrUtil.removeSuffix(
									new StringBuilder(bcd_data).insert(bcd_data.length() - 2, StrUtil.DOT).toString()
									, StrUtil.DOT
								);
								return Convert.toBigDecimal(cutNumStr);
							})
							.orElse(null)
					);
			})
			.collect(Collectors.toList());
		// 记录 1小时内每5分钟间隔的相对水位（共12条）
		elementMap.put(water_level_in_an_hour_mark, frameBodyPropertiesMessageList);
		// 跳过当前标识符位以及1小时内每5分钟间隔的相对水位（字节长度）
		index += 25;
		return index;
	}


	/**
	 * 解析湖南省报文里特殊的正文内容
	 */
	private static int huNanBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
		String ffMark = Integer.toHexString(bodyElementFrame[index]);
		String safeMark = ffMark + HexStringUtil.int2HexStr(bodyElementFrame[index + 1]);
		switch (safeMark) {
			case "ff11" : {
				/**
				 * 6个渗压监测点编号
				 * 每个编号为4字节BCD码，一共24字节
				 * 如果编号为(00000000)标识无效，后面对应的数据值不需要解析
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						return new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(0);
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff14" : {
				/**
				 * 6个渗压水位数据
				 * 每4字节为一组，3位小数，277.301米
				 * N(7,3)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(3);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalData).insert(originalData.length() - hexFrameBodyPropertiesMessage.getDecimalSize(), StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(cutNumStr) : null;
						hexFrameBodyPropertiesMessage.setVal(val);
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff12" :
			case "ff13" : {
				/**
				 * ff12：2个渗流监测点编号
				 * ff13：2个位移监测点编号
				 * 每4字节为一组,N(8)BCD码,一共8字节
				 * 如果编号为(00000000)标识无效，后面对应的数据值不需要解析
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						return new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(0);
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff15" : {
				/**
				 * 2个渗流监测点采集值
				 * 每4字节为一组，3位小数，3位小数，单位为L/秒
				 * N(7,3)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(3);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalData).insert(originalData.length() - hexFrameBodyPropertiesMessage.getDecimalSize(), StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(cutNumStr) : null;
						hexFrameBodyPropertiesMessage.setVal(val);
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff16" :
			case "ff17" :
			case "ff18" : {
				/**
				 * ff16: 2个水平X位移监测点位移值
				 * ff17: 2个水平Y位移监测点位移值
				 * ff18: 2个垂直位移采集值
				 * 5字节为一组，十进制浮点数，2位小数,最高位为FF标识是负数
				 * N(8,2)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 10))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(5)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(0);

						String originalVal = StrUtil.removePreAndLowerFirst(originalData, 2);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalVal).insert(originalVal.length() - 2, StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						if (StrUtil.startWithIgnoreCase(originalData, "ff")) {
							BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(StrUtil.removeSuffix(cutNumStr, StrUtil.DOT)).negate() : null;
							hexFrameBodyPropertiesMessage.setVal(val);
						} else {
							BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(StrUtil.removeSuffix(cutNumStr, StrUtil.DOT)) : null;
							hexFrameBodyPropertiesMessage.setVal(val);
						}
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff19" : {
				/**
				 *  6个定位坐标经度坐标
				 *  5字节为一组，经度：N(9,6)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 10))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(5)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(6);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalData).insert(originalData.length() - hexFrameBodyPropertiesMessage.getDecimalSize(), StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(cutNumStr) : null;
						hexFrameBodyPropertiesMessage.setVal(val);
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff20" : {
				/**
				 *  6个定位坐标纬度坐标
				 *  4字节为一组，纬度：N(8,6)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(6);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalData).insert(originalData.length() - hexFrameBodyPropertiesMessage.getDecimalSize(), StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(cutNumStr) : null;
						hexFrameBodyPropertiesMessage.setVal(val);
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			case "ff21" : {
				/**
				 *  6个垂直高程
				 *  4字节为一组
				 *  N(7,3)
				 */
				// 跳过ff标识符索引
				++index;
				// 数据定义（占的字节数以及小数位）
				char def_char = bodyElementFrame[++index];
				// 属性数据的字节数
				int data_size = getBodyElementByteSize(def_char);
				String original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + data_size);
				List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Arrays.stream(StrUtil.split(original_data, 8))
					.map(originalData -> {
						HexFrameBodyPropertiesMessage hexFrameBodyPropertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(4)
							.setOriginalData(originalData)
							.setTypeCode(null)
							.setDecimalSize(3);
						String cutNumStr = StrUtil.removeSuffix(
							new StringBuilder(originalData).insert(originalData.length() - hexFrameBodyPropertiesMessage.getDecimalSize(), StrUtil.DOT).toString()
							, StrUtil.DOT
						);
						BigDecimal val = NumberUtil.isNumber(cutNumStr) ? Convert.toBigDecimal(cutNumStr) : null;
						hexFrameBodyPropertiesMessage.setVal(val);
						return hexFrameBodyPropertiesMessage;
					})
					.collect(Collectors.toList());
				// 记录
				elementMap.put(safeMark, hexFrameBodyPropertiesMessageList);
				index = index + data_size + 1;
				break;
			}
			default: {
				index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
			}
		}
		return index;
	}



}
