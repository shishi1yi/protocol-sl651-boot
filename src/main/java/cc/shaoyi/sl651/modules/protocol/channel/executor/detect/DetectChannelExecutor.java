package cc.shaoyi.sl651.modules.protocol.channel.executor.detect;

import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutor;
import cc.shaoyi.sl651.modules.protocol.channel.executor.CopyOnWriteHashMap;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author ShaoYi
 * @Description  遥测站通道会话执行器
 * 根据遥测站编码，缓存的通道会话
 * @createTime 2023年06月01日 16:47
 */
@Slf4j
public class DetectChannelExecutor extends ChannelExecutor {

	private final Map<String, Channel> channelMap = new CopyOnWriteHashMap<>();

	private final Map<String, HexFrameHeaderMessage> channelHeaderMap = new CopyOnWriteHashMap<>();

	private static boolean isSameChannel(Channel channel, Channel oldChannel) {
		return oldChannel.remoteAddress().equals(channel.remoteAddress()) &&
			oldChannel.localAddress().equals(channel.localAddress());
	}

	@Override
	public String channelType() {
		return ChannelTypeEnum.detect.getCode();
	}

	/**
	 * 缓存通道会话
	 * @param channelId
	 * @param channel
	 * @param headerMessage
	 */
	@Override
	public void loadChannel(String channelId, Channel channel, HexFrameHeaderMessage headerMessage) {
		if (channelMap.containsKey(channelId)) {
			Channel oldChannel = channelMap.get(channelId);
			if (!isSameChannel(channel, oldChannel)) {
				channelMap.put(channelId, channel);
				channelHeaderMap.put(channelId, headerMessage);
				oldChannel.close();
			}
		} else {
			channelMap.put(channelId, channel);
			channelHeaderMap.put(channelId, headerMessage);
		}
	}



	/**
	 * 移除通道会话
	 * @param channel
	 */
	@Override
	public void removeChannel(Channel channel) {
		channelMap.entrySet()
			.stream()
			.filter(channelEntry -> isSameChannel(channelEntry.getValue(), channel))
			.findFirst()
			.ifPresent(channelEntry -> {
				channelMap.remove(channelEntry.getKey());
				channelHeaderMap.remove(channelEntry.getKey());
			});
	}

	/**
	 * 打印
	 */
	@Override
	public void print() {
		log.info("通道会话：{}", channelMap);
	}


	/**
	 * 获取会话通道
	 * @param channelId
	 * @return
	 */
	@Override
	public Channel getChannel(String channelId) {
		return channelMap.get(channelId);
	}


	/**
	 * 获取会话存在的测站地址列表
	 * @return
	 */
	@Override
	public Set<String> getChannelIdSet() {
		return channelMap.keySet();
	}

	/**
	 * 判断指定会话是否在线
	 * @param channelId
	 * @return
	 */
	@Override
	public boolean isOnline(String channelId) {
		return Objects.nonNull(channelMap.get(channelId));
	}


	/**
	 * 获取缓存的 header的消息
	 * @param channelId
	 * @return
	 */
	@Override
	public HexFrameHeaderMessage getHeaderMessage(String channelId) {
		return channelHeaderMap.get(channelId);
	}


}
