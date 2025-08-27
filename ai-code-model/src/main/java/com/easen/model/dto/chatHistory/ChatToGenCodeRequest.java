package com.easen.model.dto.chatHistory;

import lombok.Data;

import java.util.List;

@Data
public class ChatToGenCodeRequest {
    /**
     * appId
     */
    private Long appId;

    /**
     * 提示词
     */
    private String message;

    /**
     * 图片
     */
    private List<String> image;
}
