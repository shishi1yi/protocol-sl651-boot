package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * @Author ShaoYi
 * @Description 事件消息
 * @createDate 2022年08月05日 16:14
 **/
@Getter
@ToString
@Accessors(chain = true)
public class EventMessage extends ApplicationEvent {

	@Serial
	private static final long serialVersionUID = -4684606228228352216L;

	private HexFrameWrapper wrapper;

	private LocalDateTime localDateTime;


	public EventMessage(Object source, HexFrameWrapper wrapper, LocalDateTime localDateTime) {
		super(source);
		this.wrapper = wrapper;
		this.localDateTime = localDateTime;
	}
}
