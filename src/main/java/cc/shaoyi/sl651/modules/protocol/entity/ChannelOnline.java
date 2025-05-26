package cc.shaoyi.sl651.modules.protocol.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年06月02日 09:50
 */
@Data
@Accessors(chain = true)
public class ChannelOnline implements Serializable {

	/**
	 * 测站地址
	 */
	private String detectAdds;

	/**
	 * 1-在线，0-离线
	 */
	private int online;

	@Serial
	private static final long serialVersionUID = 5017320380238996640L;
}
