package cc.shaoyi.sl651.modules.protocol.props;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年12月22日 13:34
 */
@Data
@ToString
@Validated
@ConfigurationProperties(prefix = "sl651.property")
public class PropertyLimitProperties {


	private List<PropertyLimit> limits;
}
