package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.common.api.PageResult;
import com.harry.aifrontier.dto.request.ContentQueryRequest;
import com.harry.aifrontier.dto.request.ContentExternalRefSaveRequest;
import com.harry.aifrontier.dto.request.ContentSaveRequest;
import com.harry.aifrontier.dto.request.ContentStatusUpdateRequest;
import com.harry.aifrontier.service.ContentService;
import com.harry.aifrontier.vo.ContentAdminListItemVO;
import com.harry.aifrontier.vo.ContentDetailVO;
import com.harry.aifrontier.vo.ContentExternalRefVO;
import com.harry.aifrontier.vo.ContentOptionsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/contents")
@RequiredArgsConstructor
public class ContentAdminController {

    private final ContentService contentService;

    @GetMapping
    public ApiResponse<PageResult<ContentAdminListItemVO>> page(ContentQueryRequest request) {
        return ApiResponse.success(contentService.adminPage(request));
    }

    @GetMapping("/options")
    public ApiResponse<ContentOptionsVO> options() {
        return ApiResponse.success(contentService.options());
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(contentService.adminDetail(id));
    }

    @PostMapping
    public ApiResponse<ContentDetailVO> create(@Valid @RequestBody ContentSaveRequest request) {
        return ApiResponse.success("内容创建成功", contentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ContentDetailVO> update(@PathVariable Long id, @Valid @RequestBody ContentSaveRequest request) {
        return ApiResponse.success("内容更新成功", contentService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<ContentDetailVO> updateStatus(@PathVariable Long id, @Valid @RequestBody ContentStatusUpdateRequest request) {
        return ApiResponse.success("状态更新成功", contentService.updateStatus(id, request.getPublishStatus()));
    }

    @PostMapping("/{id}/external-refs")
    public ApiResponse<ContentExternalRefVO> createExternalRef(
            @PathVariable Long id,
            @Valid @RequestBody ContentExternalRefSaveRequest request
    ) {
        return ApiResponse.success("外部引用创建成功", contentService.createExternalRef(id, request));
    }

    @PutMapping("/{id}/external-refs/{refId}")
    public ApiResponse<ContentExternalRefVO> updateExternalRef(
            @PathVariable Long id,
            @PathVariable Long refId,
            @Valid @RequestBody ContentExternalRefSaveRequest request
    ) {
        return ApiResponse.success("外部引用更新成功", contentService.updateExternalRef(id, refId, request));
    }

    @DeleteMapping("/{id}/external-refs/{refId}")
    public ApiResponse<Void> deleteExternalRef(@PathVariable Long id, @PathVariable Long refId) {
        contentService.deleteExternalRef(id, refId);
        return ApiResponse.success("外部引用删除成功", null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contentService.delete(id);
        return ApiResponse.success("内容删除成功", null);
    }
}
