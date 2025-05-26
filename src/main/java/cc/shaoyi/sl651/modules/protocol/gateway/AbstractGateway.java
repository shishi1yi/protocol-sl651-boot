package cc.shaoyi.sl651.modules.protocol.gateway;


import cc.shaoyi.sl651.modules.protocol.biz.IBizService;
import cc.shaoyi.sl651.modules.protocol.codec.*;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import cn.hutool.extra.spring.SpringUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractGateway implements CommonGateway{

	/**
	 * 启动网关
	 */
	@Override
	public void startup() {
		//负责接收客户端的连接
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		//负责处理消息I/O
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		Sl651NettyContentProperties sl651Properties = initProperties();
		FrameHeaderDecoder frameHeaderDecoder = SpringUtil.getBean(FrameHeaderDecoder.class);
		FrameBodyDecoder frameBodyDecoder = SpringUtil.getBean(FrameBodyDecoder.class);
		FrameEncoder frameEncoder = SpringUtil.getBean(FrameEncoder.class);
		IBizService bizService = SpringUtil.getBean(IBizService.class);
		LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
		try {
			ServerBootstrap serverBootstrap = new ServerBootstrap();//用于启动NIO服务
			ChannelFuture channelFuture = serverBootstrap.group(bossGroup, workerGroup)
				//对应的是   netty                NIO                    BIO
				//NioServerSocketChannel  <== ServerSocketChannel <== ServerSocke
				//通过工厂方法设计模式实例化一个channel
				.channel(NioServerSocketChannel.class)
				//设置当前通道的处理器，使用Netty提供的日志打印处理器
//				.handler(new LoggingHandler(LogLevel.INFO))
				.localAddress(new InetSocketAddress(sl651Properties.getPort()))//设置监听端口
				.option(ChannelOption.SO_BACKLOG, 1000)//最大保持连接数1000，option主要是针对boss线程组
				.childOption(ChannelOption.SO_KEEPALIVE, true)//启用心跳保活机制，childOption主要是针对worker线程组
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel socketChannel) throws Exception {
						ExtractLengthFieldBasedFrameDecoder extractLengthFieldBasedFrameDecoder = new ExtractLengthFieldBasedFrameDecoder(4095, 11, 2, 0, 0, 4);
						socketChannel.pipeline().addLast(extractLengthFieldBasedFrameDecoder)
							.addLast(loggingHandler)
							// 2、数据包byteBuff转换成报文对象
							.addLast(new HexFrameDecoder(frameHeaderDecoder, frameBodyDecoder))
							// 3、处理报文对象
							.addLast(new FrameMessageHandler(frameEncoder, bizService))
							/*
							 * 使用心跳检测处理器
							 * 读空闲 写空闲 读写空闲 的超时时间
							 * 最后一个参数是 时间的单位
							 * IdleStateHandler发现有空闲的时候 会触发 IdleStateEvent时间
							 * 他会把事件推送给下一个 handler的指定方法 userEventTriggered 去处理
							 * */
							.addLast(new IdleStateHandler(65, 70, 72, TimeUnit.MINUTES))
							.addLast(new HeartbeatServerHandler());
					}
				})
				.bind()
				.sync();
			log.info("netty在{}上开启监听", channelFuture.channel().localAddress());
			if (channelFuture.isSuccess()) {
				log.info("tcp netty服务端启动成功");
			}
			//阻塞操作，closeFuture()开启了一个channel的监听器（这期间channel在进行各项工作），直到链路断开
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			log.error("处理异常", e);
		} finally {
			bossGroup.shutdownGracefully();//关闭EventLoopGroup并释放所有资源，包括所有创建的线程
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * 初始化属性
	 * @return
	 */
	public abstract Sl651NettyContentProperties initProperties();

}
