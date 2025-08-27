package com.easen.model.dto.auth;

import lombok.Data;

import java.io.Serializable;

@Data
public class VerifyCodeRequest implements Serializable {
    private Long userId;
    private String code;
}



