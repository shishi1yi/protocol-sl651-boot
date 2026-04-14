package cc.shaoyi.sl651.common.utils;

import cn.hutool.core.util.StrUtil;

/**
 * 通用工具类
 */
public class CommonUtil {


    public static String serialNoHex(Integer serialNo) {
        return StrUtil.padPre(Integer.toHexString(serialNo), 4, '0');
    }



    public static String createReqCode(String detectAddress, String commandCode, Integer serialNo) {
        return String.format("%s_%s_%s",
                detectAddress,
                commandCode,
                serialNo
        );
    }

}
