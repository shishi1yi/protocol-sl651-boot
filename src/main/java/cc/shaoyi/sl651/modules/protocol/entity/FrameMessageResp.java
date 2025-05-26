package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2024年04月20日 15:00
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class FrameMessageResp extends HexFrameWrapper {

	private String exceptionInfo;


	@Serial
	private static final long serialVersionUID = -2406652604673302159L;
}
