package com.stephen.cloud.common.mysql.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * SQL 工具
 *
 * @author StephenQiu30
 */
public class SqlUtils {

    /**
     * 驼峰转下划线
     *
     * @param camelStr 驼峰字符串
     * @return 下划线字符串
     */
    public static String camelToUnderline(String camelStr) {
        if (StringUtils.isBlank(camelStr)) {
            return camelStr;
        }
        return camelStr.replaceAll("([A-Z])", "_$1").toLowerCase();
    }

    /**
     * 校验排序字段是否合法（防止 SQL 注入）
     *
     * @param sortField sortField
     * @return boolean
     */
    public static boolean validSortField(String sortField) {
        if (StringUtils.isBlank(sortField)) {
            return false;
        }
        return !StringUtils.containsAny(sortField, "=", "(", ")", " ");
    }
}
