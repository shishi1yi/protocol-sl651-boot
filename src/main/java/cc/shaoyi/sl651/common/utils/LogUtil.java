package cc.shaoyi.sl651.common.utils;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2024年11月11日 10:09
 */
@Slf4j
public class LogUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    static {
        // ============================
        // 1. 容错性配置 (防止报错)
        // ============================

        // [序列化] 如果一个对象没有任何 public 字段或 getter 方法，不要报错，而是输出 {}
        // 场景：有时候临时定义了一个空类作为占位符
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // [反序列化] 如果 JSON 中有某个字段，但 Java 类中没有对应属性，不要报错，直接忽略
        // 场景：以后协议升级加了字段，旧代码也不会崩
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // ============================
        // 2. 可读性配置 (日志更友好)
        // ============================

        // [时间] 不使用时间戳 (long类型)，而是打印成由人可读的字符串
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);


        // 设置默认时间格式

        // 创建 Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // 定义时间格式：yyyy-MM-dd HH:mm:ss
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        // 配置 LocalDateTime 的序列化 (Object -> JSON String)
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        // 配置 LocalDateTime 的反序列化 (JSON String -> Object)
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        // 注册模块到 ObjectMapper
        objectMapper.registerModule(javaTimeModule);

        // 设置时区 (对旧版 Date 类型依然有效，对 LocalDateTime 序列化无影响但建议保留)
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        // ============================
        // 3. 洁癖配置 (可选)
        // ============================

        // [过滤 Null] 序列化时忽略 null 值的字段
        // 场景：你的报文中有很多无效数据解析结果是 null，开启这个会让日志清爽很多，只显示有值的数据
        // 警告：如果你需要通过 null 来判断解析失败，就不要开这个
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static void logJsonMessage(String description, String code, Object object) {
        Map<String, Object> logMap = Maps.newLinkedHashMap();
        logMap.put("description", description);
        logMap.put("protocolMessageTargetCode", code);
        logMap.put("data", object);
        try {
            log.info("{}", objectMapper.writeValueAsString(logMap));
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
        }
    }

    public static void logULFrameMessage(String description, String hexStr) {
        try {
            String lowerCaseHexStr = hexStr.toLowerCase(Locale.ROOT);
            String gatewayCode = StrUtil.sub(
                    StrUtil.subAfter(lowerCaseHexStr, "7e7e", false), // 从关键词后开始
                    0,  // 往后偏移0个长度
                    10  // 偏移0后再取10个字符（0 + 10 = 10）
            );
            Map<String, Object> protocolFrameMap = Maps.newLinkedHashMap();
            protocolFrameMap.put("description", description);
            protocolFrameMap.put("protocolFrameTargetCode", gatewayCode);
            protocolFrameMap.put("direction", "ul");
            protocolFrameMap.put("frameHex", lowerCaseHexStr);
            log.info("{}", objectMapper.writeValueAsString(protocolFrameMap));
        } catch (Exception e) {
            log.error("打印上行帧出错", e);
        }
    }

    public static void logDLFrameMessage(String description, String hexStr) {
        try {
            String lowerCaseHexStr = hexStr.toLowerCase(Locale.ROOT);
            String gatewayCode = StrUtil.sub(
                    StrUtil.subAfter(lowerCaseHexStr, "7e7e", false), // 从关键词后开始
                    2,  // 往后偏移2个长度
                    12  // 偏移2后再取10个字符（2 + 10 = 12）
            );
            Map<String, Object> protocolFrameMap = Maps.newLinkedHashMap();
            protocolFrameMap.put("description", description);
            protocolFrameMap.put("protocolFrameTargetCode", gatewayCode);
            protocolFrameMap.put("direction", "dl");
            protocolFrameMap.put("frameHex", lowerCaseHexStr);
            log.info("{}", objectMapper.writeValueAsString(protocolFrameMap));
        } catch (Exception e) {
            log.error("打印下行帧出错", e);
        }
    }
}
