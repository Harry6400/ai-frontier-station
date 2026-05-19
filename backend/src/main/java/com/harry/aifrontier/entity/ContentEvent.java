package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_content_event")
public class ContentEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    private String description;

    private String eventDate;

    private String tags;

    private Integer contentCount;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
