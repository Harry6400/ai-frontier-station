package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_arena_history")
public class ArenaHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("model_id")
    private Long modelId;

    @TableField("elo_score")
    private BigDecimal eloScore;

    @TableField("rank_position")
    private Integer rankPosition;

    @TableField("recorded_at")
    private LocalDateTime recordedAt;
}
