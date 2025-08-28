package com.easen.app.service.impl;

import com.easen.app.auth.AppUserAuthManager;
import com.easen.app.mapper.AppUserMapper;
import com.easen.app.service.AppService;
import com.easen.app.service.AppUserService;
import com.easen.client.InnerUserService;
import com.easen.common.exception.BusinessException;
import com.easen.common.exception.ErrorCode;
import com.easen.model.entity.App;
import com.easen.model.entity.AppUser;
import com.easen.model.entity.User;
import com.easen.model.enums.AppRoleEnum;
import com.easen.model.vo.AppTeamMemberVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * 应用用户关联 服务层实现。
 *
 * @author <a>easen</a>
 */
@Slf4j
@Service
public class AppUserServiceImpl extends ServiceImpl<AppUserMapper, AppUser> implements AppUserService {

    @Resource
    @Lazy
    private AppService appService;

    @Resource
    @Lazy
    private InnerUserService userService;

    @Resource
    @Lazy
    private AppUserAuthManager appUserAuthManager;

    @Override
    public boolean inviteUserToApp(Long appId, Long userId) {
        // 检查应用是否存在且为团队应用
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        if (app.getIsTeam() == null || app.getIsTeam() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该应用不是团队应用");
        }
        // 检查用户是否已经是团队成员
        if (isUserInApp(appId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已经是团队成员");
        }
        // 创建关联记录
        AppUser appUser = new AppUser();
        appUser.setAppId(appId);
        appUser.setUserId(userId);
        appUser.setCreateTime(LocalDateTime.now());
        appUser.setUpdateTime(LocalDateTime.now());
        appUser.setAppRole(AppRoleEnum.EDITOR.getValue());
        return this.save(appUser);
    }

    @Override
    public boolean removeUserFromApp(Long appId, Long userId) {
        // 参数校验
        if (appId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 检查应用是否存在
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 不能移除创建者
        if (app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能移除应用创建者");
        }

        // 检查用户是否在团队中
        if (!isUserInApp(appId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不是团队成员");
        }

        // 删除关联记录
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("appId = ?", appId)
                .and("userId = ?", userId);

        return this.remove(queryWrapper);
    }

    @Override
    public boolean isUserInApp(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return false;
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .where("appId = ?", appId)
                .and("userId = ?", userId);

        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<AppTeamMemberVO> getAppTeamMembers(Long appId) {
        if (appId == null) {
            return new ArrayList<>();
        }

        // 查询应用信息
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 查询团队成员
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("u.id as userId, u.userAccount, u.userName, u.userAvatar, u.userProfile, u.userRole, au.createTime as joinTime, au.appRole")
                .from("app_user").as("au")
                .leftJoin("user").as("u").on("au.userId = u.id")
                .where("au.appId = ?", appId);

        List<AppTeamMemberVO> members = this.listAs(queryWrapper, AppTeamMemberVO.class);


        return members;
    }

    /**
     * 根据用户ID获取在应用中的角色
     *
     * @param appId  应用ID
     * @param userId 用户ID
     * @return 用户角色
     */
    private String getAppRoleByUserId(Long appId, Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("appRole")
                .where("appId = ?", appId)
                .and("userId = ?", userId);

        AppUser appUser = this.getOne(queryWrapper);
        return appUser != null ? appUser.getAppRole() : "viewer";
    }

    /**
     * 根据角色获取权限列表
     *
     * @param appUserRole 角色
     * @return 权限列表
     */
    private List<String> getPermissionsByRole(String appUserRole) {
        return appUserAuthManager.getPermissionsByRole(appUserRole);
    }

    @Override
    public Page<AppTeamMemberVO> getAppTeamMembersByPage(Long appId, Integer pageNum, Integer pageSize) {
        if (appId == null) {
            return new Page<>();
        }

        // 查询应用信息
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 查询团队成员
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("u.id as userId, u.userAccount, u.userName, u.userAvatar, u.userProfile, u.userRole, au.createTime as joinTime, au.appRole")
                .from("app_user").as("au")
                .leftJoin("user").as("u").on("au.userId = u.id")
                .where("au.appId = ?", appId)
                .orderBy("au.createTime asc");

        Page<AppTeamMemberVO> page = this.pageAs(new Page<>(pageNum, pageSize), queryWrapper, AppTeamMemberVO.class);

        return page;
    }

    @Override
    public boolean hasAppPermission(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return false;
        }
        // 查询应用信息
        App app = appService.getById(appId);
        if (app == null) {
            return false;
        }


        // 如果是创建者，直接有权限
        if (app.getUserId().equals(userId)) {
            return true;
        }

        // 如果是团队应用，检查是否为团队成员
        if (app.getIsTeam() != null && app.getIsTeam() == 1) {
            return isUserInApp(appId, userId);
        }

        return false;
    }

    @Override
    public Page<App> getUserTeamAppsByPage(Long userId, String appName, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return new Page<>();
        }

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select("DISTINCT a.*")
                .from("app").as("a")
                .leftJoin("app_user").as("au").on("a.id = au.appId")
                .where("(a.userId = ? OR au.userId = ?)", userId, userId)
                .and("a.isTeam = 1"); // 只查询团队应用

        // 如果提供了应用名称，添加模糊查询条件
        if (appName != null && !appName.trim().isEmpty()) {
            queryWrapper.and("a.appName LIKE ?", "%" + appName.trim() + "%");
        }

        // 按创建时间倒序排列
        queryWrapper.orderBy("a.createTime desc");

        // 执行分页查询
        Page<App> page = this.pageAs(new Page<>(pageNum, pageSize), queryWrapper, App.class);

        return page;
    }

    @Override
    public boolean removeAllUsersFromApp(Long appId) {
        // 参数校验
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效");
        }

        // 构建删除条件：删除指定应用的所有团队成员关联关系
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("appId", appId);

        // 执行删除操作
        int deletedCount = this.mapper.deleteByQuery(queryWrapper);
        log.info("删除应用 {} 的所有团队成员关联关系，删除数量：{}", appId, deletedCount);

        return deletedCount >= 0; // 删除成功或没有数据需要删除都返回true
    }

    @Override
    @Transactional
    public boolean createAppTeam(Long appId, Long userId) {
        // 参数校验
        if (appId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID和用户ID不能为空");
        }

        // 检查应用是否存在
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 检查用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }

        // 检查用户是否已经是团队成员
        if (isUserInApp(appId, userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已经是团队成员");
        }
        app.setIsTeam(1);

        appService.updateById(app);
        // 创建关联记录
        AppUser appUser = new AppUser();
        appUser.setAppId(appId);
        appUser.setUserId(userId);
        appUser.setCreateTime(LocalDateTime.now());
        appUser.setUpdateTime(LocalDateTime.now());
        appUser.setAppRole(AppRoleEnum.ADMIN.getValue()); // 默认设置为编辑者角色

        return this.save(appUser);
    }
}
