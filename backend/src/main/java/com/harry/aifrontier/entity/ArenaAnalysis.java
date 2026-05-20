package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_arena_analysis")
public class ArenaAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
