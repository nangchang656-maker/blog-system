package cn.lzx.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 管理员常量类
 * 存放管理员账号ID
 *
 * @author lzx
 * @since 2025-11-06
 */
public class AdminConstants {

    /**
     * 管理员ID列表
     */
    public static final List<Long> ADMIN_IDS = Arrays.asList(1L, 2L);

    /**
     * 判断是否为管理员
     *
     * @param userId 用户ID
     * @return 是否为管理员
     */
    public static boolean isAdmin(Long userId) {
        return userId != null && ADMIN_IDS.contains(userId);
    }
}

