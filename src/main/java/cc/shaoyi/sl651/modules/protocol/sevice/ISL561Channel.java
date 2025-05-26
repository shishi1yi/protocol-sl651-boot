package cc.shaoyi.sl651.modules.protocol.sevice;


import cc.shaoyi.sl651.modules.protocol.entity.ChannelOnline;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageReq;
import cc.shaoyi.sl651.modules.protocol.entity.FrameMessageResp;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;
import java.util.Set;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2023年06月02日 09:37
 */
@Validated
public interface ISL561Channel {


	String API_PREFIX = "/channel";

	String CHANNEL_ONLINE = API_PREFIX + "/online";

	String CHANNEL_ONLINE_INTENT = API_PREFIX + "/online/intent";

	String DETECT_DATA = API_PREFIX + "/detect/data";

	String PARSE_FRAME = API_PREFIX + "/parse/frame";



	/**
	 * 在线的测站列表
	 * @return
	 */
	@GetMapping(CHANNEL_ONLINE)
	List<ChannelOnline> detectChannelOnline();

	/**
	 * 指定测站的在线情况
	 * @param detectSet
	 * @return
	 */
	@GetMapping(CHANNEL_ONLINE_INTENT)
	List<ChannelOnline> detectChannelOnline(@RequestParam("detectSet") Set<String> detectSet);


	/**
	 * 获取测站数据
	 * @param detectAddr
	 * @return
	 */
	@GetMapping(DETECT_DATA)
	Boolean fetchDetectData(@RequestParam("detectAddr") @NotBlank(message = "测站地址不能为空") String detectAddr);


	/**
	 * 解析帧报文
	 * @param message
	 * @return
	 */
	@PostMapping(PARSE_FRAME)
	FrameMessageResp parseFrame(@RequestBody FrameMessageReq message);

}
