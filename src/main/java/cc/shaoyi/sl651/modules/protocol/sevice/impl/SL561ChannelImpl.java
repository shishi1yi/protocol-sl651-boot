package cc.shaoyi.sl651.modules.protocol.sevice.impl;


import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cc.shaoyi.sl651.modules.protocol.biz.IBizService;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutor;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutorFactory;
import cc.shaoyi.sl651.modules.protocol.codec.FrameEncoder;
import cc.shaoyi.sl651.modules.protocol.entity.ChannelOnline;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageReq;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageResp;
import cc.shaoyi.sl651.modules.protocol.sevice.ISL561Channel;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年06月02日 09:46
 */
@RestController
@RequiredArgsConstructor
public class SL561ChannelImpl implements ISL561Channel {

	private final FrameEncoder frameEncoder;

	private final IBizService bizService;

	/**
	 * 在线的测站列表
	 * @return
	 */
	@Override
	@GetMapping(CHANNEL_ONLINE)
	public List<ChannelOnline> detectChannelOnline() {
		return ChannelExecutorFactory.loadInstance()
				.getExecutor(ChannelTypeEnum.detect)
				.getChannelIdSet()
				.stream()
				.map(channelId -> new ChannelOnline().setOnline(1).setDetectAdds(channelId))
				.collect(Collectors.toList());
	}

	/**
	 * 指定测站的在线情况
	 * @param detectSet
	 * @return
	 */
	@Override
	@GetMapping(CHANNEL_ONLINE_INTENT)
	public List<ChannelOnline> detectChannelOnline(Set<String> detectSet) {
		ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance()
			.getExecutor(ChannelTypeEnum.detect);
		return
			detectSet.stream()
				.map(detect -> new ChannelOnline().setDetectAdds(detect)
					.setOnline(channelExecutor.isOnline(detect) ? 1: 0))
				.collect(Collectors.toList());
	}


	/**
	 * 获取测站数据
	 * @param detectAddr
	 * @return
	 */
	@Override
	@GetMapping(DETECT_DATA)
	public Boolean fetchDetectData(String detectAddr) {
		ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
		Channel channel = channelExecutor.getChannel(detectAddr);
		if (Objects.nonNull(channel)) {
			ByteBuf byteBuf = frameEncoder.encodeAsk(channelExecutor.getHeaderMessage(detectAddr));
			if (Objects.nonNull(byteBuf)) {
				channel.writeAndFlush(byteBuf);
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	/**
	 * 解析帧报文
	 * @param message
	 * @return
	 */
	@Override
	public FrameMessageResp parseFrame(FrameMessageReq message) {
		FrameMessageResp resp = bizService.parseFrame(message);
		return resp;
	}


}
