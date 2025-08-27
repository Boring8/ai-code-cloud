package com.easen.model.dto.auth;

import lombok.Data;

import java.io.Serializable;

@Data
public class SendVerifyCodeRequest implements Serializable {

    private String email;

}



