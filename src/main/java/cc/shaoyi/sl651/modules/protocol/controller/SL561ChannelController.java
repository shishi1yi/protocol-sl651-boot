package cc.shaoyi.sl651.modules.protocol.controller;

import cc.shaoyi.sl651.common.enums.ResultCode;
import cc.shaoyi.sl651.modules.protocol.biz.IBizService;
import cc.shaoyi.sl651.modules.protocol.entity.EncodeMessage;
import cc.shaoyi.sl651.modules.protocol.entity.R;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ShaoYi
 * @Description
 * @createTime 2024年08月28日 14:35
 */
@Validated
@RestController
@RequestMapping("sl651/channel")
@RequiredArgsConstructor
public class SL561ChannelController {

    private final IBizService bizService;


    /**
     * 升级固件版本
     */
    @PostMapping("/op-e1")
    public R<EncodeMessage> upgradeFirmware(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        EncodeMessage message =  bizService.upgrade(detectAddr, jsonObject.getJSONArray("params").toList(JSONObject.class));
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 查询实时数据-报文
     */
    @PostMapping("/op-37")
    public R<EncodeMessage> op37(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        EncodeMessage message = bizService.op37(detectAddr);
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 远程重启-报文
     */
    @PostMapping("/op-e0")
    public R<EncodeMessage> ope0(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        EncodeMessage message = bizService.opE0(detectAddr);
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 召测-报文
     */
    @PostMapping("/op-e2")
    public R<EncodeMessage> ope2(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        EncodeMessage message = bizService.opE2(detectAddr);
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 查询当前软件版本报文-报文
     */
    @PostMapping("/op-45")
    public R<EncodeMessage> op45(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        EncodeMessage message = bizService.op45(detectAddr);
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 修改基本参数配置-报文
     */
    @PostMapping("/op-40")
    public R<EncodeMessage> op40(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        JSONArray params = jsonObject.getJSONArray("params");
        if (params == null || params.isEmpty()) {
            return R.fail(ResultCode.FAILURE, "参数列表不能为空");
        }
        EncodeMessage message = bizService.op40(detectAddr, params.toList(JSONObject.class));
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }

    /**
     * 查询基本参数配置-报文
     */
    @PostMapping("/op-41")
    public R<EncodeMessage> op41(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }

        JSONArray params = jsonObject.getJSONArray("params");
        if (params == null || params.isEmpty()) {
            return R.fail(ResultCode.FAILURE, "参数列表不能为空");
        }
        EncodeMessage message = bizService.op41(detectAddr, params.toList(JSONObject.class));
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }


    /**
     * 修改运行参数配置-报文
     */
    @PostMapping("/op-42")
    public R<EncodeMessage> op42(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }
        JSONArray params = jsonObject.getJSONArray("params");
        if (params == null || params.isEmpty()) {
            return R.fail(ResultCode.FAILURE, "参数列表不能为空");
        }
        EncodeMessage message = bizService.op42(detectAddr, params.toList(JSONObject.class));
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }


    /**
     * 查询运行参数配置-报文
     */
    @PostMapping("/op-43")
    public R<EncodeMessage> op43(@RequestBody JSONObject jsonObject) {
        String detectAddr = jsonObject.getStr("detectAddress");
        if (!StringUtils.hasText(detectAddr)) {
            return R.fail(ResultCode.FAILURE, "遥测站地址不能为空");
        }

        JSONArray params = jsonObject.getJSONArray("params");
        if (params == null || params.isEmpty()) {
            return R.fail(ResultCode.FAILURE, "参数列表不能为空");
        }
        EncodeMessage message = bizService.op43(detectAddr, params.toList(JSONObject.class));
        if (message == null) {
            return R.fail("无法下发");
        }
        return R.data(message);
    }
}

