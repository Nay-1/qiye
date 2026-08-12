package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.Result;
import com.qiye.entity.CourseChapter;
import com.qiye.entity.StudyRecord;
import com.qiye.mapper.CourseChapterMapper;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.StudyRecordMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.security.SecurityUtils;
import com.qiye.service.StudyService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习过程管理（章节级记录）
 */
@RestController
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final StudyRecordMapper studyRecordMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper sysUserMapper;

    @PostMapping("/start")
    public Result<StudyRecord> start(@RequestBody StartReq req) {
        return Result.ok(studyService.start(SecurityUtils.getUserId(), req.getCourseId(), req.getChapterId()));
    }

    @PostMapping("/progress")
    public Result<StudyRecord> progress(@RequestBody ProgressReq req) {
        return Result.ok(studyService.updateProgress(
                SecurityUtils.getUserId(), req.getChapterId(), req.getProgress(), req.getDuration()));
    }

    /** 我的某课程学习进度 */
    @GetMapping("/course/{courseId}")
    public Result<List<StudyRecord>> myCourse(@PathVariable Long courseId) {
        List<StudyRecord> list = studyRecordMapper.selectList(
                new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, SecurityUtils.getUserId())
                        .eq(StudyRecord::getCourseId, courseId));
        fillChapterTitle(list);
        return Result.ok(list);
    }

    /** 我的全部学习记录 */
    @GetMapping("/my")
    public Result<List<StudyRecord>> my() {
        List<StudyRecord> list = studyRecordMapper.selectList(
                new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, SecurityUtils.getUserId())
                        .orderByAsc(StudyRecord::getId));
        fillExt(list);
        return Result.ok(list);
    }

    /** 培训负责人查看某员工学习记录 */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<StudyRecord>> byUser(@PathVariable Long userId) {
        List<StudyRecord> list = studyRecordMapper.selectList(
                new LambdaQueryWrapper<StudyRecord>()
                        .eq(StudyRecord::getUserId, userId)
                        .orderByAsc(StudyRecord::getId));
        fillExt(list);
        return Result.ok(list);
    }

    private void fillChapterTitle(List<StudyRecord> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Long> chapterIds = list.stream().map(StudyRecord::getChapterId).distinct().toList();
        Map<Long, CourseChapter> chMap = chapterMapper.selectBatchIds(chapterIds).stream()
                .collect(Collectors.toMap(CourseChapter::getId, Function.identity()));
        list.forEach(r -> {
            CourseChapter ch = chMap.get(r.getChapterId());
            r.setChapterTitle(ch == null ? "" : ch.getTitle());
        });
    }

    private void fillExt(List<StudyRecord> list) {
        fillChapterTitle(list);
        Map<Long, String> courseNames = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(com.qiye.entity.Course::getId, com.qiye.entity.Course::getName));
        Map<Long, String> userNames = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(com.qiye.entity.SysUser::getId, com.qiye.entity.SysUser::getName));
        list.forEach(r -> {
            r.setCourseName(courseNames.get(r.getCourseId()));
            r.setUserName(userNames.get(r.getUserId()));
        });
    }

    @Data
    public static class StartReq {
        @NotNull(message = "课程ID不能为空")
        private Long courseId;
        @NotNull(message = "章节ID不能为空")
        private Long chapterId;
    }

    @Data
    public static class ProgressReq {
        @NotNull(message = "章节ID不能为空")
        private Long chapterId;
        @NotNull(message = "进度不能为空")
        private Integer progress;          // 0~100
        private Integer duration;          // 本次学习时长（秒）
    }
}
