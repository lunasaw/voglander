package io.github.lunasaw.voglander.common.constant;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统常量类
 * 
 * @author weidian
 */
public class ExcelApiConstants {
    public static final Map<String, Map<String, String>> allConstAlias = new LinkedHashMap<String, Map<String, String>>();
    public static final String                           SYS_CODE      = "cloud-excel";
    // dic字典表中子系统的编码
    /**
     * 初始化所有常量
     */
    static {
        try {
            for (Class cls : ExcelApiConstants.class.getClasses()) {
                Map<String, String> constMap = new LinkedHashMap<String, String>();
                for (Field fd : cls.getDeclaredFields()) {
                    ConstAnnotation ca = fd.getAnnotation(ConstAnnotation.class);
                    if (ca != null) {
                        constMap.put(fd.get(cls).toString(), ca.value());
                    } else {
                        constMap.put(fd.get(cls).toString(), fd.getName());
                    }
                }
                allConstAlias.put(cls.getSimpleName(), constMap);
            }

        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * excel sheet的默认值
     */
    public static final class ExcelSheetDefault {

        @ConstAnnotation("sheet默认开始的序号")
        public static final int    START_FOR_SHEET_NO    = 0;
        @ConstAnnotation("sheet名称的前缀")
        public static final String PREFIX_FOR_SHEET_NAME = "sheet";
    }

}
