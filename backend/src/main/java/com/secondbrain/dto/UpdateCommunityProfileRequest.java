package com.secondbrain.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 更新社区公开资料请求。 */
@Getter
@Setter
public class UpdateCommunityProfileRequest {

    /** 社区公开简介。 */
    @Size(max = 500, message = "社区简介不能超过500个字符")
    private String introduction;

    /** 擅长领域标签，最多8个。 */
    @Size(max = 8, message = "最多设置8个擅长领域")
    private List<@Size(max = 30, message = "单个领域标签不能超过30个字符") String> expertiseTags;
}
