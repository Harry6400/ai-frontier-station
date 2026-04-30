package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.SourceSaveRequest;
import com.harry.aifrontier.service.SourceService;
import com.harry.aifrontier.vo.SourceVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sources")
@RequiredArgsConstructor
public class SourceAdminController {

    private final SourceService sourceService;

    @GetMapping
    public ApiResponse<List<SourceVO>> list() {
        return ApiResponse.success(sourceService.listAll());
    }

    @PostMapping
    public ApiResponse<SourceVO> create(@Valid @RequestBody SourceSaveRequest request) {
        return ApiResponse.success("来源创建成功", sourceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<SourceVO> update(@PathVariable Long id, @Valid @RequestBody SourceSaveRequest request) {
        return ApiResponse.success("来源更新成功", sourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sourceService.delete(id);
        return ApiResponse.success("来源删除成功", null);
    }
}
