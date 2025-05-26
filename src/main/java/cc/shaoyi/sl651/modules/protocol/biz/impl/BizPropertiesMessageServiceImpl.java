package cc.shaoyi.sl651.modules.protocol.biz.impl;

import cc.shaoyi.sl651.common.enums.PropertiesTypeCodeEnum;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.biz.IBizPropertiesMessageService;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameBodyPropertiesMessage;
import cc.shaoyi.sl651.modules.protocol.entity.HexFrameWrapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2025年05月24日 15:19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizPropertiesMessageServiceImpl implements IBizPropertiesMessageService {


	@Override
	public void handleJoinPropertiesMessage(HexFrameWrapper hexFrameWrapper) {
		if (verify(hexFrameWrapper)) {
			return;
		}
		// TODO : 处理补充属性消息逻辑
	}


	@Override
	public void handleHourReportJoinPropertiesMessage(HexFrameWrapper hexFrameWrapper) {
		if (verify(hexFrameWrapper)) {
			return;
		}

		LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> propertiesMessage = hexFrameWrapper.getMessage().getBody().getPropertiesMessage();
		List<HexFrameBodyPropertiesMessage> hex_1a_properties = propertiesMessage.get(PropertiesTypeCodeEnum.hex_1a.getCode());
		List<HexFrameBodyPropertiesMessage> hex_f4_properties = propertiesMessage.get(PropertiesTypeCodeEnum.hex_f4.getCode());

		// 若 hex_1a 为空，hex_f4 不为空，则通过 hex_f4 计算 hex_1a
		if (CollUtil.isEmpty(hex_1a_properties) &&
			CollUtil.isNotEmpty(hex_f4_properties) &&
			hex_f4_properties.stream().anyMatch(hexFrameBodyPropertiesMessage -> Objects.nonNull(hexFrameBodyPropertiesMessage.getVal()))) {
			// 计算 hex_1a 的值
			BigDecimal[] val_array = hex_f4_properties.stream().map(HexFrameBodyPropertiesMessage::getVal).filter(Objects::nonNull).toArray(BigDecimal[]::new);
			BigDecimal hex1aVal = NumberUtil.add(val_array);
			// 将计算结果添加到 hex_1a_properties 中
			HexFrameBodyPropertiesMessage hex1aBodyPropertiesMessage = new HexFrameBodyPropertiesMessage();
			hex1aBodyPropertiesMessage.setTypeCode(PropertiesTypeCodeEnum.hex_1a.getCode()).setVal(hex1aVal);
			// 将 hex1aBodyPropertiesMessage 添加到 propertiesMessage 中
			propertiesMessage.put(PropertiesTypeCodeEnum.hex_1a.getCode(), Lists.newArrayList(hex1aBodyPropertiesMessage));
			LogUtil.logJsonMessage("sl651报文-小时报缺少小时雨量而进行推算补充", hexFrameWrapper.getMessage().getHeader().getDetectAddress(), hexFrameWrapper);
		}


	}

	/**
	 * 验证 HexFrameWrapper 是否有效
	 * 如果无效则返回 true，否则返回 false
	 */
	private static boolean verify(HexFrameWrapper hexFrameWrapper) {
		return Objects.isNull(hexFrameWrapper) ||
			Objects.isNull(hexFrameWrapper.getMessage()) ||
			Objects.isNull(hexFrameWrapper.getMessage().getBody()) ||
			CollUtil.isEmpty(hexFrameWrapper.getMessage().getBody().getPropertiesMessage());
	}

}
