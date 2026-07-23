package com.secondbrain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** 转让所有权请求DTO. <p>用于工作区所有权转让请求</p> */
@Getter
@Setter
public class TransferOwnershipRequest {

    /**
     * 新所有者的用户ID
     */
    @NotNull(message = "新所有者用户ID不能为空")
    private Long newOwnerUserId;
}
