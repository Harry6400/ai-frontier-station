package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_arena_model")
public class ArenaModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_name")
    private String modelName;

    private String provider;

    @TableField("current_rank")
    private Integer currentRank;

    @TableField("current_elo")
    private BigDecimal currentElo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
