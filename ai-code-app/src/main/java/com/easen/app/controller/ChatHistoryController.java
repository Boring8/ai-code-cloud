package com.easen.app.controller;

import cn.dev33.satoken.annotation.SaCheckRole;

import com.easen.app.auth.annotation.SaSpaceCheckPermission;
import com.easen.app.auth.model.AppUserPermissionConstant;
import com.easen.app.service.ChatHistoryService;
import com.easen.common.common.BaseResponse;
import com.easen.common.common.ResultUtils;
import com.easen.common.constant.UserConstant;
import com.easen.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.easen.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 对话历史 控制层。
 *
 * @author <a>easen</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;


    /**
     * 分页查询某个应用的对话历史（游标查询）
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后一条记录的创建时间
     * @return 对话历史分页
     */
    @GetMapping("/app")
    @SaSpaceCheckPermission(value = AppUserPermissionConstant.APP_VIEW)
    public BaseResponse<Page<ChatHistoryVO>> listAppChatHistory(@RequestParam Long appId,
                                                                @RequestParam(defaultValue = "10") int pageSize,
                                                                @RequestParam(required = false) LocalDateTime lastCreateTime
    ) {
        Page<ChatHistoryVO> result = chatHistoryService.listAppChatHistoryVOByPage(appId, pageSize, lastCreateTime);
        return ResultUtils.success(result);
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 对话历史分页
     */
    @PostMapping("/admin/list/page/vo")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        Page<ChatHistoryVO> result = chatHistoryService.listAllChatHistoryVOByPageForAdmin(chatHistoryQueryRequest);
        return ResultUtils.success(result);
    }

}
