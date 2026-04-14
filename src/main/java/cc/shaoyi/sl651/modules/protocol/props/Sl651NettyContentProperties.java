package cc.shaoyi.sl651.modules.protocol.props;

import cc.shaoyi.sl651.common.enums.TransferProtocolTypeEnum;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description: Sl651属性
 * @author: Shao Yi
 * @createDate: 2022年07月28日 20:05
 **/
@Data
@ToString
@Validated
@ConfigurationProperties(prefix = "sl651.netty")
public class Sl651NettyContentProperties {

	/**
	 * 监听端口
	 */
	@NotNull(message = "监听端口不能为空")
	@Min(value = 1, message = "监听端口不能小于1")
	private Integer port;

	/**
	 * 消息帧最大体积
	 */
	@NotNull(message = "消息帧最大体积不能为空")
	@Min(value = 1, message = "消息帧最大体积不能小于1")
	private Integer maxFrameLength;


	/**
	 * 是否mq转发
	 */
	private Boolean mqForward = Boolean.TRUE;


	/**
	 *  消息的交换机名称
	 */
	@NotBlank(message = "消息的交换机名称不能为空")
	private String publisherExchange;

	/**
	 *  消息的路由键, 只对定时报、加时报， 其它报文根据功能码类型
	 */
	@NotBlank(message = "消息的路由键不能为空")
	private String publisherRoutingKey;


	/**
	 * 设备tcp连接状态是否通知
	 */
	private Boolean tcpDeviceNotice = Boolean.FALSE;


	/**
	 *  通知tcp连接的mq交换机名称
	 */
	@NotBlank(message = "通知tcp连接的mq交换机名称不能为空")
	private String tcpNoticeExchange;

	/**
	 *  通知tcp连接的路由键
	 */
	@NotBlank(message = "通知tcp连接的mq路由键不能为空")
	private String tcpNoticeRoutingKey;

	/**
	 * 过期时间
	 */
	private Long publisherDelay = 86400000L;


	/**
	 * 小时报默认禁止转发
	 */
	private Boolean hourReportDisplay = Boolean.FALSE;

	/**
	 * 定时报默认禁止转发
	 */
	private Boolean regularReportDisplay = Boolean.FALSE;

	/**
	 * 加时报默认可以转发
	 */
	private Boolean overtimeReportDisplay = Boolean.TRUE;

	/**
	 * 是否tcp转发
	 */
	private Boolean tcpForward = Boolean.FALSE;

	/**
	 * tcp转发IP
	 */
	private String forwardIp;

	/**
	 * tcp转发端口号
	 */
	private Integer forwardPort;

	/**
	 * 协议解析类型，默认公司自定义协议
	 */
	@NotNull(message = "协议解析类型不能为空")
	private TransferProtocolTypeEnum transferType = TransferProtocolTypeEnum.PRI_COMPANY;

	/**
	 * 自身IP
	 */
	private String hubIP;

	/**
	 * 自身web端口号
	 */
	private Integer hubWebPort;

	/**
	 * 分钟时差
	 */
	@NotNull(message = "分钟时差不能为空")
	private Long diffMinute;

	/**
	 * 消息过期时间(分钟)
	 */
	@NotNull(message = "消息过期时间(分钟)不能为空")
	private Long messageMinutesExpiration;

	/**
	 * 是否分发路由
	 */
	private Boolean distributeRoutes = Boolean.FALSE;

	/**
	 * 是否转发到旧数据路由
	 */
	private Boolean oldDataForwardRoutes = Boolean.FALSE;


}
