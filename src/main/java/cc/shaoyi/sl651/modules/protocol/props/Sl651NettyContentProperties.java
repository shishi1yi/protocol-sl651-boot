package cc.shaoyi.sl651.modules.protocol.props;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;



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
	 * 湖南协议解析
	 */
	private Boolean hunanTransfer = Boolean.FALSE;

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
