package com.harry.aifrontier.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class EventSaveRequest {

    @NotBlank(message = "事件标题不能为空")
    private String title;

    private String summary;

    private String description;

    private String eventDate;

    private String tags;

    @NotEmpty(message = "至少关联一条内容")
    private List<Long> contentIds;
}
