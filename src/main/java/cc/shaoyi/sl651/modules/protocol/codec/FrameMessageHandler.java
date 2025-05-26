package cc.shaoyi.sl651.modules.protocol.codec;


import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cc.shaoyi.sl651.modules.protocol.biz.IBizService;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutorFactory;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameHeaderMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Shao Yi
 * @description 帧消息处理器
 * @date 2022年08月03日 18:52
 */
@Slf4j
public class FrameMessageHandler extends ChannelInboundHandlerAdapter {

	private final FrameEncoder frameEncoder;

	private final IBizService bizService;

	public FrameMessageHandler(FrameEncoder frameEncoder, IBizService bizService) {
		this.frameEncoder = frameEncoder;
		this.bizService = bizService;
	}

	private void print() {
		ChannelExecutorFactory.loadInstance()
			.getExecutor(ChannelTypeEnum.detect)
			.print();
	}

	private void removeChannel(ChannelHandlerContext ctx) {
		ChannelExecutorFactory.loadInstance()
			.getExecutor(ChannelTypeEnum.detect)
			.removeChannel(ctx.channel());
	}


	/**
	 * 缓存会话通道
	 * @param ctx
	 * @param frameWrapper
	 */
	private void loadChannel(ChannelHandlerContext ctx, HexFrameWrapper frameWrapper) {
		ChannelExecutorFactory.loadInstance()
			.getExecutor(ChannelTypeEnum.detect)
			.loadChannel(frameWrapper.getMessage().getHeader().getDetectAddress(), ctx.channel(), frameWrapper.getMessage().getHeader());
	}


	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		removeChannel(ctx);
		ctx.close();
		//print();
	}


	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			HexFrameWrapper frameWrapper = (HexFrameWrapper)msg;
			//  自定义业务处理
			if (bizService != null && frameWrapper.isSuccess()) {
				bizService.handler(frameWrapper);
			}
			replyHexFrame(ctx, frameWrapper);
			loadChannel(ctx, frameWrapper);
			//print();
		} catch (Exception e) {
			log.error("netty tcp服务端处理失败", e);
		} finally {
			// 手动回收对象，结束流水线
			ReferenceCountUtil.release(msg);
		}


	}



	/**
	 * 下行报文响应
	 * @param ctx 上下文
	 * @param frameWrapper 消息
	 */
	private void replyHexFrame(ChannelHandlerContext ctx, HexFrameWrapper frameWrapper) {
		HexFrameMessage message = frameWrapper.getMessage();
		HexFrameHeaderMessage header = message.getHeader();
		// 查询实时报（37）不需要下行报文响应
		if (!"37".equals(header.getCommandCode())) {
			//  下行报文响应
			ByteBuf byteBuf = null;
			if (frameEncoder != null) {
				byteBuf = frameEncoder.encodeReply(frameWrapper);
			}
			if (byteBuf != null) {
				Channel channel = ctx.channel();
				if (channel.isOpen()) {
					ctx.writeAndFlush(byteBuf);
				}
			}
		}
	}

	/**
	 * 打印异常
	 * @param ctx
	 * @param cause
	 * @throws Exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.info("[netty异常信息]:{}", Throwables.getStackTraceAsString(cause));
		//cause.printStackTrace();
		removeChannel(ctx);
		ctx.close();
	}
}
