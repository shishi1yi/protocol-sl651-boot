package cc.shaoyi.sl651.modules.protocol.gateway;


import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;

/**
 * @description: 协议网关接口
 * @author: Shao Yi
 * @createDate: 2022年07月28日 20:05
 **/
public interface CommonGateway {

	/**
	 * 启动网关
	 * @throws Exception
	 */
	void startup() throws InterruptedException;

	/**
	 * 初始化属性
	 * @return
	 */
	Sl651NettyContentProperties initProperties();

}
