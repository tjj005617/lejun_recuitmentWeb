package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.BenefitTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 福利标签控制器
 */
@RestController
@RequestMapping("/api/benefit-tag")
@RequiredArgsConstructor
public class BenefitTagController {

    private final BenefitTagService benefitTagService;

    /**
     * 获取所有启用的福利标签（按类型筛选）
     */
    @GetMapping("/list")
    public Result<?> listEnabled(@RequestParam(required = false) String type) {
        return Result.ok(benefitTagService.listEnabled(type));
    }

    /**
     * 获取全部福利标签（管理员用，按类型筛选）
     */
    @GetMapping("/all")
    public Result<?> listAll(@RequestParam(required = false) String type) {
        return Result.ok(benefitTagService.listAll(type));
    }

    /**
     * 新增福利标签
     */
    @PostMapping
    public Result<?> save(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String type = (String) body.get("type");
        Integer sortOrder = body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : null;
        return Result.ok(benefitTagService.save(name, type, sortOrder));
    }

    /**
     * 更新福利标签
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Integer sortOrder = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : null;
        Integer enabled = body.get("enabled") != null ? ((Number) body.get("enabled")).intValue() : null;
        benefitTagService.update(id, name, sortOrder, enabled);
        return Result.ok("更新成功");
    }

    /**
     * 删除福利标签
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        benefitTagService.delete(id);
        return Result.ok("删除成功");
    }
}
