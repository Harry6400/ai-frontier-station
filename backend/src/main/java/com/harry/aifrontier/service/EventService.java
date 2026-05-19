package com.harry.aifrontier.service;

import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.EventSaveRequest;
import com.harry.aifrontier.vo.EventDetailVO;
import com.harry.aifrontier.vo.EventListItemVO;

public interface EventService {

    PageResult<EventListItemVO> listEvents(Integer pageNum, Integer pageSize);

    EventDetailVO getEvent(Long id);

    EventDetailVO createEvent(EventSaveRequest request);

    EventDetailVO updateEvent(Long id, EventSaveRequest request);

    void deleteEvent(Long id);

    int autoCluster();

    void approveEvent(Long id);

    void rejectEvent(Long id);
}
