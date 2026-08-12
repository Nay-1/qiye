package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.Course;
import com.qiye.entity.CourseChapter;
import com.qiye.mapper.CourseChapterMapper;
import com.qiye.mapper.CourseMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程章节管理
 */
@RestController
@RequestMapping("/chapter")
@RequiredArgsConstructor
public class CourseChapterController {

    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;

    @GetMapping("/course/{courseId}")
    public Result<List<CourseChapter>> listByCourse(@PathVariable Long courseId) {
        return Result.ok(chapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSeq)));
    }

    /** 全量保存章节（编辑后整体提交） */
    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> saveByCourse(@PathVariable Long courseId, @RequestBody List<CourseChapter> chapters) {
        if (courseMapper.selectById(courseId) == null) {
            throw new BizException("课程不存在");
        }
        chapterMapper.delete(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId));
        int seq = 1;
        for (CourseChapter ch : chapters) {
            if (!hasText(ch.getTitle())) {
                continue;
            }
            ch.setId(null);
            ch.setCourseId(courseId);
            ch.setSeq(seq++);
            chapterMapper.insert(ch);
        }
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> update(@RequestBody CourseChapter chapter) {
        if (chapter.getId() == null) {
            throw new BizException("章节ID不能为空");
        }
        chapterMapper.updateById(chapter);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        chapterMapper.deleteById(id);
        return Result.ok();
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
