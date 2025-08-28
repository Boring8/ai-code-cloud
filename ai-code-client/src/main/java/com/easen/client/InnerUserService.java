package com.easen.client;

import cn.dev33.satoken.stp.StpUtil;
import com.easen.common.exception.BusinessException;
import com.easen.common.exception.ErrorCode;
import com.easen.model.entity.User;
import com.easen.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.easen.common.constant.UserConstant.USER_LOGIN_STATE;

public interface InnerUserService {

    List<User> listByIds(Collection<? extends Serializable> ids);

    User getById(Serializable id);

    UserVO getUserVO(User user);
    boolean isAdmin(User user);
    // 静态方法，避免跨服务调用
    static User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object loginId = StpUtil.getLoginIdDefaultNull();
        if (Objects.isNull(loginId)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return (User) StpUtil.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
    }
}
