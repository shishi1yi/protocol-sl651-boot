package cc.shaoyi.sl651.modules.protocol.transfer;

import cn.hutool.core.util.HexUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * @author ShaoYi
 * @Description Netty TCP转发客户端，带有时长计算功能
 * @createTime 2024年02月02日 16:13
 */
@Slf4j
public class NettyClientHandler {

	private final String host;
	private final int port;
	private final String message;

	public NettyClientHandler(String host, int port, String message) {
		this.host = host;
		this.port = port;
		this.message = message;
	}

	public void start() {
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();
		LocalDateTime startTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

		try {
			Bootstrap bootstrap = new Bootstrap();
			bootstrap.group(workerGroup);
			bootstrap.channel(NioSocketChannel.class);
			bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
			bootstrap.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) {
					ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
						@Override
						public void channelActive(ChannelHandlerContext ctx) throws Exception {
							ByteBuf buffer = Unpooled.copiedBuffer(HexUtil.decodeHex(message));
							ctx.writeAndFlush(buffer);
						}
					});
				}
			});

			log.info(">>> [tcp转发]开始转发设备数据报文,转发到指定tcp地址(ip:{},port:{}),发送时间:{}, 发送报文:{}"
				, host, port
				, startTime
				, message
			);

			ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
			channelFuture.channel().closeFuture().await(1, TimeUnit.SECONDS);

			LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
			Duration duration = Duration.between(startTime, endTime);

			log.info(">>> [tcp转发]设备数据报文转发结束,转发到指定tcp地址(ip:{},port:{}),结束时间:{}, 发送报文:{}, 耗时:{}毫秒"
				, host, port
				, endTime
				, message
				, duration.toMillis()
			);
		} catch (Exception e) {
			LocalDateTime errorTime = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
			Duration duration = Duration.between(startTime, errorTime);
			log.error(">>> [tcp转发]转发到指定tcp地址(ip:{},port:{})转发失败,耗时:{}毫秒, 报文:{}", host, port, duration.toMillis(), message, e);
		} finally {
			workerGroup.shutdownGracefully();
		}
	}
}
