package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_api_credential")
public class ApiCredential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String provider;

    private String encryptedKey;

    private String iv;

    private String keySuffix;

    private String remark;

    private Integer isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
