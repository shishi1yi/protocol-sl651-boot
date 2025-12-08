package cc.shaoyi.sl651.modules.protocol.config;

import ch.qos.logback.core.PropertyDefinerBase;
import cn.hutool.core.net.NetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2024年02月20日 17:20
 */
@Slf4j
public class LogBackIpConfig extends PropertyDefinerBase {

	private static String webIP;
	static {
		try {
			webIP = NetUtil.getLocalhostStr();
		} catch (Exception e) {
			log.error("获取日志Ip异常", e);
			webIP = null;
		}
	}

	@Override
	public String getPropertyValue() {
//		return "10.50.114.2";
		return webIP;
	}
}
