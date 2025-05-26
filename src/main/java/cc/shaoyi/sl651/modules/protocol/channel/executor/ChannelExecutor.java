package cc.shaoyi.sl651.modules.protocol.channel.executor;

import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import io.netty.channel.Channel;

import java.util.Set;

/**
 * @author ShaoYi
 * @Description 通道会话执行器
 * @createTime 2023年06月01日 16:35
 */
public abstract class ChannelExecutor {


	/**
	 * 类型
	 * @return
	 */
	public abstract String channelType();


	/**
	 * 缓存通道会话
	 * @param channelId
	 * @param channel
	 * @param headerMessage
	 */
	public abstract void loadChannel(String channelId, Channel channel, HexFrameHeaderMessage headerMessage);

	/**
	 * 移除通道会话
	 * @param channel
	 */
	public abstract void removeChannel(Channel channel);

	/**
	 * 打印
	 */
	public abstract void print();

	/**
	 * 获取会话通道
	 * @param channelId
	 * @return
	 */
	public abstract Channel getChannel(String channelId);

	/**
	 * 获取会话存在的测站地址列表
	 * @return
	 */
	public abstract Set<String> getChannelIdSet();

	/**
	 * 判断指定会话是否在线
	 * @param channelId
	 * @return
	 */
	public abstract boolean isOnline(String channelId);


	/**
	 * 获取缓存的 header的消息
	 * @param channelId
	 * @return
	 */
	public abstract HexFrameHeaderMessage getHeaderMessage(String channelId);
}
