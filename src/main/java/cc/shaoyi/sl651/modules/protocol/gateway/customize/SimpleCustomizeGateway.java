package cc.shaoyi.sl651.modules.protocol.gateway.customize;

import cc.shaoyi.sl651.modules.protocol.gateway.AbstractGateway;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * @description: 自定义网关
 * @author: Shao Yi
 * @createDate: 2022年07月28日 20:05
 **/
@Component
@RequiredArgsConstructor
public class SimpleCustomizeGateway extends AbstractGateway {

	private final Sl651NettyContentProperties sl651Properties;

	/**
	 * 初始化属性
	 * @return
	 */
	@Override
	public Sl651NettyContentProperties initProperties() {
		return sl651Properties;
	}



		/**
		 * 下行报文：7E 7E 00 10 10 00 01 01 A0 00 32 80 08  02 00 0C 20 07 24 17 23 49 06 DB 17
		 * 报头：
		 *    起始符   7E 7E
		 *    遥测站地址   00 10 10 00 01
		 *    中心站地址   01
		 *    密码   A0 00
		 *    功能码   32
		 *    报文下行标志及长度   80 08
		 *
		 * 报文起始符   02
		 * 报文正文
		 *    流水号    00 0C
		 *    发报时间  20 07 24 17 23 49   2020年7月24日17点23分49秒
		 * 报文结束符号   06
		 *
		 * 校验码   DB 17
		 */



		/**
		 *
		 * 7E7E 01 00 10 10 00 01 A0 00 32 00 2B  02 00 0C 20 07 24 17 23 49 F1 F1 00 10  10 00 01 48 F0 F0 20 07 24 17 23 20 19  00 01 30 22 19 00 00 40 39 23 00 00 05  50 38 12 12 68 03 83 97
		 *
		 * 报头：
		 *     起始符   7E7E
		 *     中心站地址   01
		 *     遥测站地址  00 10 10 00 01
		 *     密码   A000
		 *     功能码   32
		 *    上行标志及正文长度   00 2B
		 *
		 * 报文起始符   02
		 * 报文正文：
		 *    流水号   00 0C
		 *    发报时间   20 07 24 17 23 49   2020年07月24日17点23分49秒
		 *    地址标识符   F1 F1
		 *    遥测站地址   00 10 10 00 01
		 *    遥测站分类码   48
		 *    观测时间符   F0 F0
		 *    观测时间   20 07 24 17 23    2020年07月24日17点23分
		 *    当前雨量  20 19 00 01 30      当前雨量为13mm
		 *    五分钟雨量 22 19 00 00 40     五分钟雨量为4mm
		 *    水位   39 23 00 00 05  50     水位为0.550m
		 *    电压   38 12 12 68   电压为12.68V
		 * 报文结束符   03
		 * 校验码   8397
		 */


}
