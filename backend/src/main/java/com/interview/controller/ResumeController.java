package com.interview.controller;

import cn.hutool.core.bean.BeanUtil;
import com.interview.common.Result;
import com.interview.domain.po.Resume;
import com.interview.domain.vo.ResumeDetailVO;
import com.interview.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 简历控制器
 * 提供简历上传和查询接口
 * 简历存储在 MinIO，解析后的内容保存到数据库
 */
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public Result<?> uploadResume(@RequestParam("file") MultipartFile file) {
        Resume resume = resumeService.uploadResume(file);
        return Result.ok(resume);
    }

    @GetMapping("/{id}")
    public Result<?> getResume(@PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id);
        if (resume == null) {
            return Result.fail("简历不存在");
        }
        String fileUrl = resumeService.getResumeFileUrl(id);

        ResumeDetailVO vo = BeanUtil.copyProperties(resume, ResumeDetailVO.class);
        vo.setFileUrl(fileUrl != null ? fileUrl : "");
        return Result.ok(vo);
    }
}
