package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.EventSaveRequest;
import com.harry.aifrontier.service.EventService;
import com.harry.aifrontier.vo.EventDetailVO;
import com.harry.aifrontier.vo.EventListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@Validated
public class EventAdminController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<PageResult<EventListItemVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(eventService.listEvents(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(eventService.getEvent(id));
    }

    @PostMapping
    public ApiResponse<EventDetailVO> create(@Valid @RequestBody EventSaveRequest request) {
        return ApiResponse.success("事件创建成功", eventService.createEvent(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EventDetailVO> update(@PathVariable Long id, @Valid @RequestBody EventSaveRequest request) {
        return ApiResponse.success("事件更新成功", eventService.updateEvent(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ApiResponse.success("事件删除成功", null);
    }

    @PostMapping("/auto-cluster")
    public ApiResponse<Integer> autoCluster() {
        int count = eventService.autoCluster();
        return ApiResponse.success("自动聚类完成，新增 " + count + " 个事件", count);
    }
}
