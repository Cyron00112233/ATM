package com.atm.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

//交易明细查询请求体
@Data
public class TransDetailRequest {

    @NotBlank(message = "会话令牌不能为空")
    private String sessionToken;

    //查最近几笔，不传默认10
    private Integer limit;
}
