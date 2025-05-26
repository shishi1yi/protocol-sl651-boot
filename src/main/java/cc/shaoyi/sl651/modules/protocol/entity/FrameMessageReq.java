package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author ShaoYi
 * @Description 帧消息
 * @createTime 2024年04月20日 14:32
 */
@Data
@Accessors(chain = true)
public class FrameMessageReq implements Serializable {

	@Getter
	@AllArgsConstructor
	public enum FrameType {

		HEX("hex","16进制");

		private final String type;

		private final String describe;
	}


	private String originalFrame;

	private FrameType frameType;





	@Serial
	private static final long serialVersionUID = 7603877700078585188L;
}
