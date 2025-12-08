package cc.shaoyi.sl651.common.utils;


import cc.shaoyi.sl651.common.constant.CommonConstant;
import cc.shaoyi.sl651.common.enums.FrameBodyElementEnum;
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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
    public static boolean  isM3Mode(char[] frame) {
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
     * 处理定时报报文正文内容
     */
    public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> periodReportBodyProperties(char[] bodyElementFrame) {
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
     */
    public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> hourReportBodyProperties(char[] bodyElementFrame) {
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
     * <p>
     * 说明：解析报文中的【标识符(1-2字节) + 数据定义(1字节) + 数据内容(N字节)】格式
     *
     * @param bodyElementFrame 报文正文全部字符数组
     * @param index            当前解析的游标索引
     * @param elementMap       结果存储容器
     * @return 解析完成后的新游标索引 (指向下一段数据的开始)
     */
    private static int singleBodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
        // ==========================================
        // 1. 解析标识符 (Type Code)
        // ==========================================
        // 读取当前字节作为标识符
        String typeCode = Integer.toHexString(bodyElementFrame[index]);

        // 判断是否为扩展标识符：如果当前字节是 ff，说明标识符占2个字节，需要向后多读一位
        if ("ff".equals(typeCode)) {
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
        // 数据起始位置：当前索引 + 1(标识符占位，如果是ff之前已经+1了) + 1(数据定义占位) + 1(指向下一位)
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
                // 例如：ff123 -> 123
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
        // 5. 封装对象并存储 (Build & Store)
        // ==========================================
        // 获取枚举定义的名称
        FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(typeCode);
        String typeName = Opt.ofNullable(frameBodyElementEnum)
                .map(FrameBodyElementEnum::getFormatElementName)
                .orElse("");

        // 构建结果对象
        HexFrameBodyPropertiesMessage message = new HexFrameBodyPropertiesMessage()
                .setTypeCode(typeCode)
                .setTypeName(typeName)
                .setIndex(0) // 单一属性默认索引为0
                .setDataSize(dataSize)
                .setDecimalSize(decimalSize)
                .setOriginalData(originalData)
                .setVal(finalVal);

        // 将结果存入 Map，如果 key 不存在则创建新 List
        List<HexFrameBodyPropertiesMessage> elementList = elementMap.computeIfAbsent(typeCode, k -> new LinkedList<>());
        elementList.add(message);

        // ==========================================
        // 6. 更新游标索引 (Update Index)
        // ==========================================
        // 新索引 = 当前标识符位置 + 2 (1字节定义+1字节偏移) + 数据长度
        // 注意：这里的 +2 是指跳过 "数据定义字节" 和 "偏移量"，这也是为了配合外层循环的逻辑
        index += (2 + dataSize);

        return index;
    }


    /**
     * 解析时间标识符（f0）和测站编码标识符（f1）
     */
    private static int specialF0AndF1BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
        String typeCode = Integer.toHexString(bodyElementFrame[index]);
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
        String rain_in_an_hour_mark = Integer.toHexString(bodyElementFrame[index]);
        // 一小时内每5分钟时段的降雨量数据定义（占的字节数以及小数位）
        char rain_in_an_hour_data_def_char = bodyElementFrame[++index];
        // 属性数据的字节数
        int rain_in_an_hour_data_size = getBodyElementByteSize(rain_in_an_hour_data_def_char);
        // 雨量原始数据
        String rain_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + rain_in_an_hour_data_size);

        // 标识符引导符
        FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(rain_in_an_hour_mark);

        // 一小时内每5分钟时段的降雨量
        List<HexFrameBodyPropertiesMessage> hexFrameBodyPropertiesMessageList = Lists.newArrayList();
        String[] rain_in_an_hour_arrays = StrUtil.split(rain_in_an_hour_original_data, 2);
        for (int i = 0; i < rain_in_an_hour_arrays.length; i++) {
            String originalData = rain_in_an_hour_arrays[i];
            HexFrameBodyPropertiesMessage propertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(1)
                    .setOriginalData(originalData)
                    .setDecimalSize(1)
                    .setTypeCode(rain_in_an_hour_mark)
                    .setTypeName(
                            Opt.ofNullable(frameBodyElementEnum)
                                    .map(FrameBodyElementEnum::getFormatElementName)
                                    .orElse("")
                    )
                    .setIndex(i)
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
        String water_level_in_an_hour_mark = Integer.toHexString(bodyElementFrame[index]);
        // 相对水位数据定义（占的字节数以及小数位）
        char water_level_in_an_hour_def_char = bodyElementFrame[++index];
        // 属性数据的字节数
        int water_level_in_an_hour_data_size = getBodyElementByteSize(water_level_in_an_hour_def_char);

        // 标识符引导符
        FrameBodyElementEnum frameBodyElementEnum = FrameBodyElementEnum.getByHexCode(water_level_in_an_hour_mark);

        // 1小时内每5分钟间隔的相对水位原始数据（字节数据）
        List<HexFrameBodyPropertiesMessage> frameBodyPropertiesMessageList =  Lists.newArrayList();

        String water_level_in_an_hour_original_data = HexStringUtil.int2HexStr(bodyElementFrame, index + 1, index + water_level_in_an_hour_data_size);
        String[] water_level_in_an_hour_original_data_arrays = StrUtil.split(water_level_in_an_hour_original_data, 4);

        for (int i = 0; i < water_level_in_an_hour_original_data_arrays.length; i++) {
            String originalData = water_level_in_an_hour_original_data_arrays[i];
            HexFrameBodyPropertiesMessage propertiesMessage = new HexFrameBodyPropertiesMessage().setDataSize(2)
                    .setOriginalData(originalData)
                    .setTypeCode(water_level_in_an_hour_mark)
                    .setTypeName(
                            Opt.ofNullable(frameBodyElementEnum)
                                    .map(FrameBodyElementEnum::getFormatElementName)
                                    .orElse("")
                    )
                    .setIndex(i)
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
            frameBodyPropertiesMessageList.add(propertiesMessage);
        }
        // 记录 1小时内每5分钟间隔的相对水位（共12条）
        elementMap.put(water_level_in_an_hour_mark, frameBodyPropertiesMessageList);
        // 跳过当前标识符位以及1小时内每5分钟间隔的相对水位（字节长度）
        index += 25;
        return index;
    }



    /**
     * 解析图片报 (标准协议)
     */
    public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getStandardImgBodyPropertiesMessages(char[] bodyElementFrame) {
        LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();
        if (ArrayUtil.isNotEmpty(bodyElementFrame)) {
            // 每组数据，前两字节为标识符，其中第一个字节为标识引导符，第二个字节定义数据信息
            for (int index = 0; index < bodyElementFrame.length;) {
                String mark = Integer.toHexString(bodyElementFrame[index]);
                try {
                    if ("f3".equals(mark)) {
                        index = specialStandardF3BodyPropertiesMessages(bodyElementFrame, index, elementMap);
                        // 标准F3通常读取到末尾，index此时应该等于length，循环会结束。
                        // 即使没结束，也应continue以防singleBodyPropertiesMessages再次处理
                        continue;
                    }
                    index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
                } catch (Exception e) {
                    log.info(
                            "标准解析(图片)规则异常，报文正文内容：{}, 已解析结果：{}",
                            HexStringUtil.int2HexStr(bodyElementFrame, 0, bodyElementFrame.length -1),
                            JSONUtil.toJsonStr(elementMap)
                    );
                    throw new RuntimeException(e);
                }
            }
        }
        return elementMap;
    }



    /**
     * 解析图片报 (湖南协议)
     */
    public static LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> getHunanImgBodyPropertiesMessages(char[] bodyElementFrame) {
        LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap = Maps.newLinkedHashMap();
        if (ArrayUtil.isNotEmpty(bodyElementFrame)) {
            for (int index = 0; index < bodyElementFrame.length;) {
                String mark = Integer.toHexString(bodyElementFrame[index]);
                try {
                    if ("f3".equals(mark)) {
                        index = specialHunanF3BodyPropertiesMessages(bodyElementFrame, index, elementMap);
                        continue; // 必须 continue
                    }
                    index = singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
                } catch (Exception e) {
                    log.info(
                            "湖南省解析(图片)规则异常，报文正文内容：{}, 已解析结果：{}",
                            HexStringUtil.int2HexStr(bodyElementFrame, 0, bodyElementFrame.length -1),
                            JSONUtil.toJsonStr(elementMap)
                    );
                    throw new RuntimeException(e);
                }
            }
        }
        return elementMap;
    }


    /**
     * 处理标准 F3 逻辑
     * F3 (Tag) + F3 (Fixed Data Def) + All Data
     */
    private static int specialStandardF3BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
        String img_mark = Integer.toHexString(bodyElementFrame[index]);
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
     * 处理湖南 F3 逻辑
     * F3 (Tag) + Length (1 Byte) + Data (Length Bytes)
     */
    private static int specialHunanF3BodyPropertiesMessages(char[] bodyElementFrame, int index, LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> elementMap) {
        String img_mark = Integer.toHexString(bodyElementFrame[index]);

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
        String ffMark = Integer.toHexString(bodyElementFrame[index]);
        String safeMark = ffMark + HexStringUtil.int2HexStr(bodyElementFrame[index + 1]);
        FrameBodyElementEnum.HunanBodyElementEnum hunanBodyElementEnum = FrameBodyElementEnum.HunanBodyElementEnum.getByHexCode(safeMark);

        // 如果无法识别该标识符，则按单一属性值处理
        if (hunanBodyElementEnum == null || hunanBodyElementEnum == FrameBodyElementEnum.HunanBodyElementEnum.el_hunan_ex) {
            return singleBodyPropertiesMessages(bodyElementFrame, index, elementMap);
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
                message.setVal(calculateValue(data, config));
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
