package com.harry.aifrontier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_content_event_link")
public class ContentEventLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private Long contentId;

    private LocalDateTime createdAt;
}
