package com.easen.app.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import com.easen.app.auth.model.AppUserAuthConfig;
import com.easen.app.auth.model.AppUserRole;
import com.easen.app.service.AppUserService;
import com.easen.client.InnerUserService;
import com.easen.model.entity.App;
import com.easen.model.entity.AppUser;
import com.easen.model.entity.User;
import com.easen.model.enums.AppRoleEnum;
import com.easen.model.enums.AppTypeEnum;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 成员权限管理
 */
@Component
public class AppUserAuthManager {

    @Resource
    @Lazy
    private InnerUserService userService;

    @Resource
    private AppUserService appUserService;

    public static final AppUserAuthConfig APP_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/AppUserAuthConfig.json");
        APP_USER_AUTH_CONFIG = JSONUtil.toBean(json, AppUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     *
     * @param appUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String appUserRole) {
        if (StrUtil.isBlank(appUserRole)) {
            return new ArrayList<>();
        }
        AppUserRole role = APP_USER_AUTH_CONFIG.getRoles()
                .stream()
                .filter(r -> r.getKey().equals(appUserRole))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        }
        return role.getPermissions();
    }


    /**
     * 获取权限列表
     *
     * @param app
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(App app, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(AppRoleEnum.ADMIN.getValue());
        if (app == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
        }
        // 根据 App 类型判断权
        boolean isPublic = Objects.equals(app.getIsPublic(), AppTypeEnum.PUBLIC.getValue());
        //是否为团队管理员
        if (app.getUserId().equals(loginUser.getId())) {
            return ADMIN_PERMISSIONS;
        } else if (appUserService.isUserInApp(app.getId(), loginUser.getId())) { //是否为团队成员
            // 取出当前登录用户对应的 spaceUser
            AppUser loginSpaceUser = appUserService.getOne(new QueryWrapper().eq(AppUser::getAppId, app.getId())
                    .eq(AppUser::getUserId, loginUser.getId()));
            return getPermissionsByRole(loginSpaceUser.getAppRole());
        } else if (isPublic) {//是否有公开
            return getPermissionsByRole(AppRoleEnum.VIEWER.getValue());
        } else {
            return new ArrayList<>();
        }
    }
}
