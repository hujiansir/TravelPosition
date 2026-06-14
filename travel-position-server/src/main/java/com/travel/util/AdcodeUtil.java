package com.travel.util;

/**
 * 行政区划码(adcode)换算工具
 * 中国行政区划码结构:省2 + 市2 + 县2(共6位)
 */
public class AdcodeUtil {

    private AdcodeUtil() {
    }

    /**
     * 区县级 adcode → 地级市 adcode(前4位 + "00")
     * 例:330106(西湖区) → 330100(杭州市)
     */
    public static long districtToCity(long districtAdcode) {
        return (districtAdcode / 100) * 100;
    }

}
