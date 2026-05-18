package com.harry.aifrontier.controller.portal;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.PortalContentQueryRequest;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.vo.ContentAdminListItemVO;
import com.harry.aifrontier.vo.ContentDetailVO;
import com.harry.aifrontier.vo.HomeOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
public class PortalContentController {

    private final ContentService contentService;

    @GetMapping("/home")
    public ApiResponse<HomeOverviewVO> home() {
        return ApiResponse.success(contentService.portalHome());
    }

    @GetMapping("/contents")
    public ApiResponse<PageResult<ContentAdminListItemVO>> page(PortalContentQueryRequest request) {
        return ApiResponse.success(contentService.portalPage(request));
    }

    @GetMapping("/contents/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(contentService.portalDetail(id));
    }
}
