package com.harry.aifrontier.controller.admin;

import com.harry.aifrontier.common.api.ApiResponse;
import com.harry.aifrontier.dto.request.TagSaveRequest;
import com.harry.aifrontier.service.TagService;
import com.harry.aifrontier.vo.TagVO;
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
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
public class TagAdminController {

    private final TagService tagService;

    @GetMapping
    public ApiResponse<List<TagVO>> list() {
        return ApiResponse.success(tagService.listAll());
    }

    @PostMapping
    public ApiResponse<TagVO> create(@Valid @RequestBody TagSaveRequest request) {
        return ApiResponse.success("标签创建成功", tagService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<TagVO> update(@PathVariable Long id, @Valid @RequestBody TagSaveRequest request) {
        return ApiResponse.success("标签更新成功", tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ApiResponse.success("标签删除成功", null);
    }
}
