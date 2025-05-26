package cc.shaoyi.sl651.modules.protocol.codec;

import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutorFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ShaoYi
 * @Description 心跳处理器
 * @createTime 2023年11月14日 10:59
 */
@Slf4j
public class HeartbeatServerHandler extends SimpleChannelInboundHandler<String> {

	private int times;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
		log.info(msg);
	}


	private void removeChannel(ChannelHandlerContext ctx) {
		ChannelExecutorFactory.loadInstance()
			.getExecutor(ChannelTypeEnum.detect)
			.removeChannel(ctx.channel());
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		IdleStateEvent event = (IdleStateEvent) evt;
		String eventDesc = switch (event.state()) {
            case READER_IDLE -> "读空闲";
            case WRITER_IDLE -> "写空闲";
            case ALL_IDLE -> "读写空闲";
        };
        log.info("[心跳检测]{}发生超时事件--{}", ctx.channel().remoteAddress(), eventDesc);
		times++;
		if (times >= 3) {
			log.info("[心跳检测]{}空闲次数已经达三次, 关闭连接", ctx.channel().remoteAddress());
			//ctx.writeAndFlush("you are out");
			removeChannel(ctx);
			ctx.channel().close();
		}
	}
}
