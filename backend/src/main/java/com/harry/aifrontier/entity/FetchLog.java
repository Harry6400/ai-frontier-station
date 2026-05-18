package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_fetch_log")
public class FetchLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
    private String keyword;
    private Integer papersFound;
    private Integer papersImported;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
