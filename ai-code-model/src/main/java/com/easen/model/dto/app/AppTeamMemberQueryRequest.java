package com.easen.model.dto.app;


import com.easen.common.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用团队成员查询请求
 *
 * @author <a>easen</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppTeamMemberQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用ID
     */
    private Long appId;

    private static final long serialVersionUID = 1L;
}
