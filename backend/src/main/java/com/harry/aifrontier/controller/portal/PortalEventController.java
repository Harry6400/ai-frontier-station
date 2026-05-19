package com.harry.aifrontier.controller.portal;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.service.EventService;
import com.harry.aifrontier.vo.EventDetailVO;
import com.harry.aifrontier.vo.EventListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/events")
@RequiredArgsConstructor
public class PortalEventController {

    private final EventService eventService;

    /**
     * List approved events (public portal).
     */
    @GetMapping
    public ApiResponse<PageResult<EventListItemVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.success(eventService.listEvents(pageNum, pageSize));
    }

    /**
     * Get event detail with linked contents (public portal).
     */
    @GetMapping("/{id}")
    public ApiResponse<EventDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(eventService.getEvent(id));
    }
}
