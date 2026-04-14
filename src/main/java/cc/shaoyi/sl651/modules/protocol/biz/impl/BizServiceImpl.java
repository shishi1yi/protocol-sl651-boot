package cc.shaoyi.sl651.modules.protocol.biz.impl;

import cc.shaoyi.sl651.common.enums.ChannelTypeEnum;
import cc.shaoyi.sl651.common.enums.FrameCommandCodeEnum;
import cc.shaoyi.sl651.common.enums.TransferProtocolTypeEnum;
import cc.shaoyi.sl651.common.utils.CommonUtil;
import cc.shaoyi.sl651.common.utils.FrameUtil;
import cc.shaoyi.sl651.common.utils.HexStringUtil;
import cc.shaoyi.sl651.common.utils.LogUtil;
import cc.shaoyi.sl651.modules.protocol.biz.IBizPropertiesMessageService;
import cc.shaoyi.sl651.modules.protocol.biz.IBizService;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutor;
import cc.shaoyi.sl651.modules.protocol.channel.executor.ChannelExecutorFactory;
import cc.shaoyi.sl651.modules.protocol.codec.FrameBodyDecoder;
import cc.shaoyi.sl651.modules.protocol.codec.FrameEncoder;
import cc.shaoyi.sl651.modules.protocol.codec.FrameHeaderDecoder;
import cc.shaoyi.sl651.modules.protocol.entity.*;
import cc.shaoyi.sl651.modules.protocol.props.PropertyLimit;
import cc.shaoyi.sl651.modules.protocol.props.PropertyLimitProperties;
import cc.shaoyi.sl651.modules.protocol.props.Sl651NettyContentProperties;
import cc.shaoyi.sl651.modules.protocol.transfer.NettyClientHandler;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * @Author ShaoYi
 * @Description 业务处理实现类
 * @createDate 2022年08月05日 14:34
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class BizServiceImpl implements IBizService{

    private final RabbitTemplate rabbitTemplate;

    private final Sl651NettyContentProperties sl651Properties;

    private final ApplicationContext applicationContext;

    private final PropertyLimitProperties limitProperties;

    private final FrameHeaderDecoder frameHeaderDecoder;

    private final FrameBodyDecoder frameBodyDecoder;

    private final IBizPropertiesMessageService bizPropertiesMessageService;

    private final FrameEncoder frameEncoder;

    private final Sl651NettyContentProperties sl651NettyContentProperties;


    /**
     * 处理业务
     */
    @Override
    public boolean handler(HexFrameWrapper hexFrameWrapper) {
        applicationContext.publishEvent(new EventMessage(this, hexFrameWrapper, LocalDateTime.now(ZoneId.of("Asia/Shanghai"))));
        return true;
    }


    /**
     * 消息处理监听
     */
    @Async
    @EventListener
    public void onApplicationEvent(EventMessage eventMessage) {
        HexFrameWrapper wrapper = eventMessage.getWrapper();
        if (Objects.isNull(wrapper)) {
            log.warn("监听处理时间:{},接收到空数据,不做处理", LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            return;
        }
        HexFrameMessage message = wrapper.getMessage();
        if (Objects.isNull(message) ||
                Objects.isNull(message.getBody()) ||
                Objects.isNull(message.getBody().getPropertiesMessage())) {
            log.warn("未解析到设备数据报文,不做处理");
            return;
        }

        handleMqForward(wrapper);
        handleTcpForward(wrapper);
    }

    /**
     * 处理mq转发
     */
    private void handleMqForward(HexFrameWrapper hexFrameWrapper) {


        if (!sl651Properties.getMqForward()) {
            log.info("[mq转发禁用]设备数据报文,转发到mq, 数据:{}", JSONUtil.toJsonStr(hexFrameWrapper));
            return;
        }

        try {
            // ========================判断是否放行==============================
            if (CollUtil.isNotEmpty(limitProperties.getLimits())) {
                HexFrameMessage hexFrameMessage = hexFrameWrapper.getMessage();
                LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> propertiesMessage = hexFrameMessage.getBody().getPropertiesMessage();

                final Map<String, PropertyLimit> propertyCode_limit_kv = CollUtil.toMap(limitProperties.getLimits(), Maps.newLinkedHashMap(), PropertyLimit::getCode, propertyLimit -> propertyLimit);

                LinkedHashSet<Map.Entry<String, List<HexFrameBodyPropertiesMessage>>> forkPropertiesMessage = Sets.newLinkedHashSet(propertiesMessage.entrySet());

                for (Map.Entry<String, List<HexFrameBodyPropertiesMessage>> propertiesMessageEntry : forkPropertiesMessage) {
                    PropertyLimit propertyLimit = propertyCode_limit_kv.get(propertiesMessageEntry.getKey());
                    Boolean accord = isAccord(propertiesMessageEntry.getValue(), propertyLimit);
                    if (!accord) {
                        propertiesMessage.remove(propertiesMessageEntry.getKey());
                    }
                }
                if (CollUtil.isEmpty(propertiesMessage)) {
                    log.info("[mq转发]不符合放行规则,禁止转发处理");
                    LogUtil.logJsonMessage("sl651报文-MQ转发-不符合放行规则", hexFrameWrapper.getMessage().getHeader().getDetectAddress(), hexFrameWrapper);
                    return;
                }
            }
            // ======================================================
            // 转发消息
            forward(hexFrameWrapper);
        } catch (Exception e) {
            log.error("[mq转发失败]设备数据报文,转发到mq, 数据:{}", JSONUtil.toJsonStr(hexFrameWrapper), e);
            LogUtil.logJsonMessage("sl651报文-MQ转发-异常", hexFrameWrapper.getMessage().getHeader().getDetectAddress(), hexFrameWrapper);
        }
    }

    /**
     * 处理tcp转发
     */
    private void handleTcpForward(HexFrameWrapper hexFrameWrapper) {
        if (sl651Properties.getTcpForward()) {
            try {
                log.info("[tcp转发]设备数据报文,转发到指定tcp地址(ip:{},port:{}),发送时间:{}, 发送报文:{}"
                        , sl651Properties.getForwardIp(), sl651Properties.getForwardPort()
                        , LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                        , hexFrameWrapper.getOriginalFrame()
                );
                String description = StrFormatter.format("sl651报文-TCP转发(ip:{},port:{})",
                        sl651Properties.getForwardIp(), sl651Properties.getForwardPort()
                );
                LogUtil.logJsonMessage(description, hexFrameWrapper.getMessage().getHeader().getDetectAddress(), hexFrameWrapper);
                NettyClientHandler clientHandler = new NettyClientHandler(sl651Properties.getForwardIp(), sl651Properties.getForwardPort(), hexFrameWrapper.getOriginalFrame());
                clientHandler.start();
            } catch (Exception e) {
                log.error("[tcp转发异常]设备数据报文,转发到指定tcp地址(ip:{},port:{}), 发送报文:{}"
                        , sl651Properties.getForwardIp(), sl651Properties.getForwardPort()
                        , hexFrameWrapper.getOriginalFrame()
                        , e
                );
            }
        }
    }


    /**
     * 是否符合条件
     */
    private Boolean isAccord(List<HexFrameBodyPropertiesMessage> propertiesMessageList, PropertyLimit limit) {
        if (CollUtil.isNotEmpty(propertiesMessageList) && Objects.nonNull(limit)) {
            // 删除空值
            propertiesMessageList.removeIf(hexFrameBodyPropertiesMessage -> Objects.isNull(hexFrameBodyPropertiesMessage.getVal()));
            // 判断是否符合区间
            if (Objects.nonNull(limit.getOpen()) && Objects.nonNull(limit.getClose())) {
                return propertiesMessageList.stream()
                        .allMatch(hexFrameBodyPropertiesMessage -> {
                            return NumberUtil.compare(hexFrameBodyPropertiesMessage.getVal().doubleValue(), limit.getOpen()) >= 0 &&
                                    NumberUtil.compare(hexFrameBodyPropertiesMessage.getVal().doubleValue(), limit.getClose()) <= 0;
                        });
            } else if (Objects.nonNull(limit.getOpen())) {
                return propertiesMessageList.stream()
                        .allMatch(hexFrameBodyPropertiesMessage -> {
                            return NumberUtil.compare(hexFrameBodyPropertiesMessage.getVal().doubleValue(), limit.getOpen()) >= 0;
                        });
            } else if (Objects.nonNull(limit.getClose())) {
                return propertiesMessageList.stream()
                        .allMatch(hexFrameBodyPropertiesMessage -> {
                            return NumberUtil.compare(hexFrameBodyPropertiesMessage.getVal().doubleValue(), limit.getClose()) <= 0;
                        });
            }
        }
        return Boolean.TRUE;
    }


    /**
     * 转发
     */
    private void forward(HexFrameWrapper wrapper) {
        HexFrameHeaderMessage header = wrapper.getMessage().getHeader();
        String commandCode = header.getCommandCode();
        FrameCommandCodeEnum commandEnum = FrameCommandCodeEnum.getFrameFuncEnum(commandCode);
        switch (commandEnum) {
            case TEST_REPORT: {
                log.info(">>>测试报(30)不做转发, 测站编码(测站地址):{}", header.getDetectAddress());
                break;
            }
            case REGULAR_REPORT: {
                // 定时报是否转发
                Boolean reportDisplay = sl651Properties.getRegularReportDisplay();
                sendHexFrameWrapperToMQ(wrapper, reportDisplay, commandEnum);
                break;
            }
            case OVERTIME_REPORT: {
                // 加时报是否转发
                Boolean reportDisplay = sl651Properties.getOvertimeReportDisplay();
                sendHexFrameWrapperToMQ(wrapper, reportDisplay, commandEnum);
                break;
            }
            case HOUR_REPORT: {
                // 小时报是否转发
                Boolean reportDisplay = sl651Properties.getHourReportDisplay();
                // 小时报文 处理补充属性消息的逻辑
                bizPropertiesMessageService.handleHourReportJoinPropertiesMessage(wrapper);
                sendHexFrameWrapperToMQ(wrapper, reportDisplay, commandEnum);
                break;
            }
            case CURRENT_REPORT:
            case BASE_PARAM:
            case REVISE_BASE_PARAM:
            case RUNNING_PARAM:
            case REVISE_RUNNING_PARAM:
            case SOFTWARE_VERSION: {
                sendHexFrameWrapperToMQ(wrapper, true, commandEnum);
                break;
            }
            case PRI_COMMAND: {
                // 在这里判断企业传输条件
                if (sl651NettyContentProperties.getTransferType() == TransferProtocolTypeEnum.PRI_COMPANY) {
                    // 私有命令，根据原始码判断是否需要转发
                    FrameCommandCodeEnum.PRI_COMPANY priCompany = FrameCommandCodeEnum.PRI_COMPANY.getFrameFuncEnum(commandCode);
                    if (priCompany == null) {
                        log.info(">>>私有命令报文-企业协议-未定义的功能码({}),不做转发, 测站编码(测站地址):{}", commandCode, header.getDetectAddress());
                        return;
                    }
                    sendHexPriCompanyWrapperToMQ(wrapper, priCompany);
                }
                break;
            }
            default: {
                LogUtil.logJsonMessage(
                        StrFormatter.format("测站码({}),跳过转发(功能码{})步骤, ", header.getDetectAddress(), commandCode),
                        header.getDetectAddress(),
                        wrapper
                );
                break;
            }
        }
    }


    /**
     * 获取mq的路由key
     */
    private String getMQRoutingKey(FrameCommandCodeEnum commandEnum, HexFrameWrapper wrapper) {
        HexFrameMessage message = wrapper.getMessage();
        HexFrameBodyMessage body = message.getBody();

        String special = "";

        // 是否分发路由
        if (sl651Properties.getDistributeRoutes()) {
            // 定义特殊因子（水位 + 雨量）
            List<String> waterRainFactors = List.of(
                    "39", "3a", "3b",          // 水位因子：河道水位、库下水位、库上水位
                    "1a", "1b", "1c", "1d", "1e", "1f", "20", "21", "22", "23", "24", "25", "26"  // 雨量因子
            );

            // 定义特殊因子（位移）
            List<String> gnssFactors = List.of(
                    "76", "77", "78"         // 位移因子：水平切向位移(x)、水平径向位移(y)、垂向位移(z)
            );


            // 获取并修改 propertiesMessage
            LinkedHashMap<String, List<HexFrameBodyPropertiesMessage>> properties = body.getPropertiesMessage();
            if (CollUtil.isNotEmpty(properties)) {
                boolean containsWaterRainFactors = CollUtil.containsAny(properties.keySet(), waterRainFactors);
                if (containsWaterRainFactors) {
                    special = ".water.rain";
                }
                boolean containsGnssFactors = CollUtil.containsAny(properties.keySet(), gnssFactors);
                if (containsGnssFactors) {
                    special = ".gnss";
                }
            }
        }
        switch (commandEnum) {
            case REGULAR_REPORT:
            case OVERTIME_REPORT:
            case CURRENT_REPORT: {
                return StrFormatter.format("{}{}", sl651Properties.getPublisherRoutingKey(), special);
            }
            case HOUR_REPORT: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.HOUR_REPORT.name().toLowerCase(Locale.ROOT), special);
            }
            case BASE_PARAM: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.BASE_PARAM.name().toLowerCase(Locale.ROOT), special);
            }
            case REVISE_BASE_PARAM: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.REVISE_BASE_PARAM.name().toLowerCase(Locale.ROOT), special);
            }
            case RUNNING_PARAM: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.RUNNING_PARAM.name().toLowerCase(Locale.ROOT), special);
            }
            case REVISE_RUNNING_PARAM: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.REVISE_RUNNING_PARAM.name().toLowerCase(Locale.ROOT), special);
            }
            case SOFTWARE_VERSION: {
                return StrFormatter.format("{}{}", FrameCommandCodeEnum.SOFTWARE_VERSION.name().toLowerCase(Locale.ROOT), special);
            }
            default: {
                LogUtil.logJsonMessage("sl651报文-获取mq路由key失败", wrapper.getMessage().getHeader().getDetectAddress(), wrapper);
                return null;
            }
        }
    }

    /**
     * 旧数据的路由key
     */
    private String getOldDataMQRoutingKey(FrameCommandCodeEnum commandEnum) {
        switch (commandEnum) {
            case REGULAR_REPORT:
            case CURRENT_REPORT:
            case OVERTIME_REPORT: {
                return sl651Properties.getPublisherRoutingKey() + "_old";
            }
            case HOUR_REPORT: {
                return FrameCommandCodeEnum.HOUR_REPORT.name().toLowerCase(Locale.ROOT) + "_old";
            }
            default: {
                throw new IllegalArgumentException(Opt.ofNullable(commandEnum).map(FrameCommandCodeEnum::getDesc).orElse("") + "未定义旧数据的mq转发的路由key");
            }
        }
    }

    /**
     * 发送消息到mq
     */
    private void sendHexFrameWrapperToMQ(HexFrameWrapper wrapper, Boolean reportDisplay, FrameCommandCodeEnum commandEnum) {

        HexFrameHeaderMessage header = wrapper.getMessage().getHeader();

        if (!reportDisplay) {
            log.info(">>>[mq转发]禁止转发[{}], 测站编码(测站地址):{}, 原因:处于禁用状态", commandEnum.getDesc(), header.getDetectAddress());
            return;
        }

        String routingKey = getMQRoutingKey(commandEnum, wrapper);
        // 没有配置的路由就结束
        if (routingKey == null || "unknown".equals(routingKey)) {
            log.info(">>>[mq转发]禁止转发[{}], 测站编码(测站地址):{}, 原因:未获取或没有配置路由key", commandEnum.getDesc(), header.getDetectAddress());
            return;
        }

        // 过早的数据转发到旧数据队列
        HexFrameBodyMessage body = wrapper.getMessage().getBody();
        if (StrUtil.isNotBlank(body.getObserveTime())) {
            DateTime observeTS = DateUtil.parse(body.getObserveTime(), "yyyyMMddHHmm");
            Long diffMinute = sl651Properties.getDiffMinute();
            if (sl651Properties.getOldDataForwardRoutes() && DateUtil.compare(observeTS, DateUtil.date()) < 0 && DateUtil.betweenMs(observeTS, DateUtil.date()) > (diffMinute * 60 * 1000)) {
                routingKey = getOldDataMQRoutingKey(commandEnum);
                log.info(">>>[mq转发]开始转发[{}], 测站编码(测站地址、网关编码):{}, 报文中发送时间{}、报文中观测时间:{}, 根据观测超过{}分钟,转发到旧数据队列"
                        , commandEnum.getDesc(), header.getDetectAddress(), body.getSendTime(), observeTS, diffMinute);
                LogUtil.logJsonMessage("sl651报文-旧数据(设备上报历史数据)", header.getDetectAddress(), wrapper);
            }
        }


//			LogUtil.logJsonMessage("sl651报文-MQ放行数据", wrapper.getMessage().getHeader().getDetectAddress(), wrapper);
        rabbitTemplate.convertAndSend(sl651Properties.getPublisherExchange(),
                routingKey,
                JSONUtil.toJsonStr(wrapper),
                message -> {
                    message.getMessageProperties()
                            //消息过期时间
                            .setExpiration(Convert.toStr(sl651Properties.getPublisherDelay(), "86400000"));
                    return message;
                }
        );

    }


    /**
     * 发送企业私有协议的消息到mq
     */
    private void sendHexPriCompanyWrapperToMQ(HexFrameWrapper wrapper, FrameCommandCodeEnum.PRI_COMPANY priCompany) {
        String routingKey = StrFormatter.format("{}.{}", sl651Properties.getPublisherRoutingKey(), priCompany.getCode().toLowerCase(Locale.ROOT));
        rabbitTemplate.convertAndSend(sl651Properties.getPublisherExchange()
                , routingKey
                , JSONUtil.toJsonStr(wrapper)
                , message -> {
                    message.getMessageProperties()
                            //消息过期时间
                            .setExpiration(Convert.toStr(sl651Properties.getPublisherDelay(), "86400000"));
                    return message;
                });
    }
    /**
     * 解析帧
     */
    public FrameMessageResp parseFrame(FrameMessageReq message) {
        String messageStr = JSONUtil.toJsonStr(message);
        log.info(">>> 验证帧报文，数据:{}", JSONUtil.toJsonStr(message));
        FrameMessageResp resp = new FrameMessageResp();
        try {
            if (!FrameMessageReq.FrameType.HEX.equals(message.getFrameType())) {
                resp.setExceptionInfo("帧类型必须指定类型，仅支持hex报文");
                resp.setSuccess(false);
                return resp;
            }
            if (StrUtil.isBlank(message.getOriginalFrame())) {
                resp.setExceptionInfo("报文不能为空");
                resp.setSuccess(false);
                return resp;
            }
            HexFrameMessage frameMessage = new HexFrameMessage();
            resp.setMessage(frameMessage)
                    .setOriginalFrame(message.getOriginalFrame());
            if (FrameUtil.verifyCRC16Code(message.getOriginalFrame())) {
                char[] frame = HexStringUtil.hexStr2CharArray(message.getOriginalFrame());
                HexFrameHeaderMessage headerMessage = frameHeaderDecoder.decodeHexHeader(frame);
                frameMessage.setHeader(headerMessage);
                int bodyLength = headerMessage.getBodyLength().intValue();
                if (headerMessage.isM3Mode()) {
                    frameMessage.setBody(
                            frameBodyDecoder.decodeM3Body(
                                    FrameUtil.getM3Body(frame, bodyLength)
                            )
                    );
                } else {
                    frameMessage.setBody(
                            frameBodyDecoder.decodeM124Body(
                                    FrameUtil.getM124Body(frame, bodyLength),
                                    FrameCommandCodeEnum.getFrameFuncEnum(headerMessage.getCommandCode()),
                                    headerMessage.getCommandCode()
                            )
                    );
                }
                frameMessage.setBodyEndFrameMark(FrameUtil.getBodyEndSymbol(frame));
                frameMessage.setCrcCode(FrameUtil.getCRC16Code(frame));
                resp.setSuccess(true);
            } else {
                resp.setExceptionInfo("CRC16校验不通过");
                resp.setSuccess(false);
            }
        } catch (Exception e) {
            log.error("帧消息编码错误", e);
            resp.setExceptionInfo("帧消息编码错误");
            resp.setSuccess(false);
        }
        log.info(">>> [解析结果]验证帧报文,数据:{},结果:{}", messageStr, JSONUtil.toJsonStr(resp));
        return resp;
    }



    @Override
    public EncodeMessage upgrade(String detectAddr, List<JSONObject> params) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeUpgrade(serialNo, channelExecutor.getHeaderMessage(detectAddr), params);
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                // 因为固件升级回文功能是e3 所以这里写死为e3
                String reqCode = CommonUtil.createReqCode(detectAddr, "e3", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op37(String detectAddr) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeAsk37(serialNo, channelExecutor.getHeaderMessage(detectAddr));
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "37", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage opE0(String detectAddr) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeAskE0(serialNo, channelExecutor.getHeaderMessage(detectAddr));
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "e0", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage opE2(String detectAddr) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeAskE2(serialNo, channelExecutor.getHeaderMessage(detectAddr));
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "e2", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op41(String detectAddr, List<JSONObject> params) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeCommand41(serialNo, channelExecutor.getHeaderMessage(detectAddr), params);
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "41", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op40(String detectAddr, List<JSONObject> params) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeCommand40(serialNo, channelExecutor.getHeaderMessage(detectAddr), params);
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "40", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op42(String detectAddr, List<JSONObject> params) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeCommand42(serialNo, channelExecutor.getHeaderMessage(detectAddr), params);
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "42", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op43(String detectAddr, List<JSONObject> params) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {
            ByteBuf byteBuf = frameEncoder.encodeCommand43(serialNo, channelExecutor.getHeaderMessage(detectAddr), params);
            if (Objects.nonNull(byteBuf)) {
                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "43", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EncodeMessage op45(String detectAddr) {
        ChannelExecutor channelExecutor = ChannelExecutorFactory.loadInstance().getExecutor(ChannelTypeEnum.detect);
        Channel channel = channelExecutor.getChannel(detectAddr);
        // 生成随机流水号（1-65535）
        int serialNo = RandomUtil.randomInt(1, 65536);
        if (Objects.nonNull(channel)) {


            ByteBuf byteBuf = frameEncoder.encodeAsk45(serialNo, channelExecutor.getHeaderMessage(detectAddr));

            if (Objects.nonNull(byteBuf)) {

                channel.writeAndFlush(byteBuf);

                String serialNoHex = CommonUtil.serialNoHex(serialNo);
                String frameHexStr = ByteBufUtil.hexDump(byteBuf);

                String reqCode = CommonUtil.createReqCode(detectAddr, "45", serialNo);

                return EncodeMessage.builder()
                        .serialNoHex(serialNoHex)
                        .serialNo(serialNo)
                        .detectAddress(detectAddr)
                        .frameHex(frameHexStr)
                        .reqCode(reqCode)
                        .build();
            }
        }
        return null;
    }


}
