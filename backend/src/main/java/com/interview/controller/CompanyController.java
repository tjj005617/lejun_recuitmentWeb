package com.interview.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.interview.common.Result;
import com.interview.domain.po.Company;
import com.interview.domain.po.Job;
import com.interview.service.CompanyService;
import com.interview.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公司管理控制器
 */
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /**
     * 保存公司信息
     * 若传 companyId → 选择已有公司（1:N关联）
     * 若不传 companyId → 新建公司并关联
     */
    @PostMapping
    public Result<?> saveCompany(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();

        Company company = new Company();
        // 有companyId说明是编辑已有公司
        Object companyIdObj = request.get("companyId");
        if (companyIdObj != null) {
            company.setId(((Number) companyIdObj).longValue());
        }
        company.setName((String) request.get("name"));
        company.setIndustry((String) request.get("industry"));
        company.setScale((String) request.get("scale"));
        company.setType((String) request.get("type"));
        Object regionIdObj = request.get("regionId");
        if (regionIdObj != null) {
            company.setRegionId(((Number) regionIdObj).longValue());
        }
        company.setCity((String) request.get("city"));
        company.setAddress((String) request.get("address"));
        company.setWebsite((String) request.get("website"));
        company.setDescription((String) request.get("description"));

        Company saved = companyService.saveCompany(userId, company);

        // 保存福利（中间表）
        Object benefitsObj = request.get("benefits");
        if (benefitsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> benefitNames = ((List<Object>) benefitsObj).stream()
                .map(Object::toString).collect(Collectors.toList());
            companyService.saveCompanyBenefits(saved.getId(), benefitNames);
        }

        return Result.ok(saved);
    }

    /**
     * 按名称模糊搜索公司（公开接口）
     */
    @GetMapping("/search")
    public Result<?> searchCompanies(@RequestParam(required = false) String keyword) {
        return Result.ok(companyService.searchCompanies(keyword));
    }

    /**
     * 分页获取公司列表（公开接口，支持关键词搜索）
     */
    @GetMapping("/list")
    public Result<?> listCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(companyService.listCompanies(keyword, page, size));
    }

    /**
     * 获取公司详情（公开接口，包含福利标签）
     */
    @GetMapping("/{id}")
    public Result<?> getCompany(@PathVariable Long id) {
        Company company = companyService.getCompanyById(id);
        if (company == null) {
            return Result.fail("公司不存在");
        }
        // 查询公司福利标签并填充到返回结果
        List<String> benefits = companyService.getCompanyBenefitNames(id);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", company.getId());
        result.put("userId", company.getUserId());
        result.put("name", company.getName());
        result.put("logo", company.getLogo());
        result.put("industry", company.getIndustry());
        result.put("scale", company.getScale());
        result.put("type", company.getType());
        result.put("city", company.getCity());
        result.put("regionId", company.getRegionId());
        result.put("address", company.getAddress());
        result.put("website", company.getWebsite());
        result.put("description", company.getDescription());
        result.put("viewCount", company.getViewCount());
        result.put("followCount", company.getFollowCount());
        result.put("benefits", benefits);
        return Result.ok(result);
    }

    /**
     * 获取公司下的职位列表（公开接口，分页+筛选）
     */
    @GetMapping("/{id}/jobs")
    public Result<?> getCompanyJobs(@PathVariable Long id,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String city,
                                     @RequestParam(required = false) String jobType,
                                     @RequestParam(required = false) String experience,
                                     @RequestParam(required = false) String education,
                                     @RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "8") int size) {
        List<Job> allJobs = companyService.getCompanyJobs(id, keyword, city, jobType, experience, education);
        // 内存分页（数据量不大，直接截取）
        int total = allJobs.size();
        int from = Math.min((page - 1) * size, total);
        int to = Math.min(from + size, total);
        List<Job> pageJobs = allJobs.subList(from, to);
        return Result.ok(Map.of("records", pageJobs, "total", total));
    }

    /**
     * 获取当前HR所属公司信息
     */
    @GetMapping("/my")
    public Result<?> getMyCompany() {
        Long userId = UserContext.getUserId();
        Company company = companyService.getCompanyByUserId(userId);
        return Result.ok(company);
    }

    /**
     * 获取热门公司（按职位数降序）
     */
    @GetMapping("/hot")
    public Result<?> getHotCompanies(
            @RequestParam(defaultValue = "6") int limit) {
        return Result.ok(companyService.getHotCompanies(limit));
    }

    /**
     * 增加公司浏览量（公开接口）
     */
    @PostMapping("/{id}/view")
    public Result<?> increaseViewCount(@PathVariable Long id) {
        companyService.increaseViewCount(id);
        return Result.ok();
    }

    /**
     * 关注/取消关注公司
     */
    @PostMapping("/{id}/follow")
    public Result<?> toggleFollow(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean followed = companyService.toggleFollow(userId, id);
        return Result.ok(followed);
    }

    /**
     * 检查是否已关注
     */
    @GetMapping("/{id}/followed")
    public Result<?> isFollowed(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        boolean followed = companyService.isFollowed(userId, id);
        return Result.ok(followed);
    }

    /**
     * 获取当前用户关注的公司列表
     */
    @GetMapping("/followed")
    public Result<?> getFollowedCompanies() {
        Long userId = UserContext.getUserId();
        return Result.ok(companyService.getUserFollowedCompanies(userId));
    }

}
