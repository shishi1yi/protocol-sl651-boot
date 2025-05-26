package cc.shaoyi.sl651.modules.protocol.props;

import lombok.Data;
import lombok.ToString;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年12月22日 11:53
 */
@Data
@ToString
public class PropertyLimit {

	private String code;

	private Long open;

	private Long close;

}
