package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.entity.Course;
import com.qiye.entity.CourseChapter;
import com.qiye.entity.StudyRecord;
import com.qiye.mapper.CourseChapterMapper;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.StudyRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudyService {

    private final StudyRecordMapper studyRecordMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseMapper courseMapper;
    private final TrainingTaskService trainingTaskService;

    @Transactional
    public StudyRecord start(Long userId, Long courseId, Long chapterId) {
        requireChapter(courseId, chapterId);
        StudyRecord rec = get(userId, chapterId);
        if (rec == null) {
            rec = new StudyRecord();
            rec.setUserId(userId);
            rec.setCourseId(courseId);
            rec.setChapterId(chapterId);
            rec.setProgress(0);
            rec.setStudyDuration(0);
            rec.setStatus("IN_PROGRESS");
            rec.setStartedAt(LocalDateTime.now());
            rec.setUpdatedAt(LocalDateTime.now());
            studyRecordMapper.insert(rec);
        } else if ("NOT_STARTED".equals(rec.getStatus())) {
            rec.setStatus("IN_PROGRESS");
            rec.setStartedAt(LocalDateTime.now());
            rec.setUpdatedAt(LocalDateTime.now());
            studyRecordMapper.updateById(rec);
        }
        return rec;
    }

    @Transactional
    public StudyRecord updateProgress(Long userId, Long chapterId, int progress, int duration) {
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BizException("章节不存在");
        }
        StudyRecord rec = get(userId, chapterId);
        if (rec == null) {
            rec = start(userId, chapter.getCourseId(), chapterId);
        }
        rec.setProgress(Math.max(rec.getProgress(), Math.min(progress, 100)));
        rec.setStudyDuration(rec.getStudyDuration() + Math.max(duration, 0));
        if (progress >= 100) {
            rec.setStatus("COMPLETED");
            if (rec.getCompletedAt() == null) {
                rec.setCompletedAt(LocalDateTime.now());
            }
        }
        rec.setUpdatedAt(LocalDateTime.now());
        studyRecordMapper.updateById(rec);
        if ("COMPLETED".equals(rec.getStatus())) {
            checkCourseCompleted(userId, chapter.getCourseId());
        }
        return rec;
    }

    private void checkCourseCompleted(Long userId, Long courseId) {
        Long total = chapterMapper.selectCount(
                new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, courseId));
        if (total == 0) {
            return;
        }
        Long done = studyRecordMapper.selectCount(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getCourseId, courseId)
                .eq(StudyRecord::getStatus, "COMPLETED"));
        if (done.equals(total)) {
            trainingTaskService.markCourseCompleted(userId, courseId);
        }
    }

    private void requireChapter(Long courseId, Long chapterId) {
        CourseChapter ch = chapterMapper.selectById(chapterId);
        if (ch == null || !ch.getCourseId().equals(courseId)) {
            throw new BizException("章节不存在或不属于该课程");
        }
        if (courseMapper.selectById(courseId) == null) {
            throw new BizException("课程不存在");
        }
    }

    public StudyRecord get(Long userId, Long chapterId) {
        return studyRecordMapper.selectOne(new LambdaQueryWrapper<StudyRecord>()
                .eq(StudyRecord::getUserId, userId)
                .eq(StudyRecord::getChapterId, chapterId));
    }
}
