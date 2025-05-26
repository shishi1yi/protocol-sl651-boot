package cc.shaoyi.sl651.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;
import java.util.Map;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2024年11月11日 10:09
 */
@Slf4j
public class LogUtil {

	public static void logJsonMessage(String description, String code, Object object) {
		Map<String, Object> logMap = Maps.newLinkedHashMap();
		logMap.put("description", description);
		logMap.put("protocolMessageTargetCode", code);
		logMap.put("data", object);
		log.info("{}", JSONUtil.toJsonStr(logMap));
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
			log.info("{}", JSONUtil.toJsonStr(protocolFrameMap));
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
			log.info("{}", JSONUtil.toJsonStr(protocolFrameMap));
		} catch (Exception e) {
			log.error("打印下行帧出错", e);
		}
	}
}
