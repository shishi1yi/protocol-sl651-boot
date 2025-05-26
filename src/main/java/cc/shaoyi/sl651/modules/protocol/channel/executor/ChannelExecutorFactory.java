package cc.shaoyi.sl651.modules.protocol.channel.executor;


import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

/**
 * @author ShaoYi
 * @Description 通道会话执行器工厂
 * 提供不同类型的会话执行器
 * @createTime 2023年06月01日 16:14
 */
@Slf4j
public class ChannelExecutorFactory {

	private static ChannelExecutorFactory channelExecutorFactory;

	private final Map<String, ChannelExecutor> channelExecutorMap = new HashMap<>();

	public static ChannelExecutorFactory loadInstance() {
		if (Objects.isNull(channelExecutorFactory)) {
			channelExecutorFactory = new ChannelExecutorFactory();
		}
		return channelExecutorFactory;
	}

	public ChannelExecutor getExecutor(ChannelTypeEnum channelType) {
		if (CollUtil.isEmpty(channelExecutorMap)) {
			ServiceLoader<ChannelExecutor> loader = ServiceLoader.load(ChannelExecutor.class);
			loader.forEach(channelExecutor -> {
				channelExecutorMap.put(channelExecutor.channelType(), channelExecutor);
			});
		}
		return channelExecutorMap.get(channelType.getCode());
	}
}
