package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.entity.CourseSkill;
import com.qiye.entity.JobSkill;
import com.qiye.entity.TrainingTask;
import com.qiye.entity.UserJob;
import com.qiye.mapper.CourseSkillMapper;
import com.qiye.mapper.JobSkillMapper;
import com.qiye.mapper.TrainingTaskMapper;
import com.qiye.mapper.UserJobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 培训任务生成：岗位 → 技能 → 课程 展开
 */
@Service
@RequiredArgsConstructor
public class TrainingTaskService {

    private final TrainingTaskMapper taskMapper;
    private final UserJobMapper userJobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final CourseSkillMapper courseSkillMapper;

    /**
     * 按员工当前岗位重新同步任务（幂等）
     * 触发时机：员工分配岗位后调用
     */
    @Transactional
    public void syncForUser(Long userId) {
        List<UserJob> userJobs = userJobMapper.selectList(
                new LambdaQueryWrapper<UserJob>().eq(UserJob::getUserId, userId));
        for (UserJob uj : userJobs) {
            List<JobSkill> jobSkills = jobSkillMapper.selectList(
                    new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, uj.getJobId()));
            for (JobSkill js : jobSkills) {
                List<CourseSkill> courseSkills = courseSkillMapper.selectList(
                        new LambdaQueryWrapper<CourseSkill>().eq(CourseSkill::getSkillId, js.getSkillId()));
                for (CourseSkill cs : courseSkills) {
                    upsertTask(userId, uj.getJobId(), js.getSkillId(), cs.getCourseId());
                }
            }
        }
    }

    /**
     * 员工开始学习某课程时，把该课程的「待学」任务标记为「学习中」
     */
    public void markCourseInProgress(Long userId, Long courseId) {
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getUserId, userId)
                        .eq(TrainingTask::getCourseId, courseId));
        for (TrainingTask t : tasks) {
            if ("PENDING".equals(t.getStatus())) {
                t.setStatus("IN_PROGRESS");
                taskMapper.updateById(t);
            }
        }
    }

    /**
     * 课程章节全部学完后，把该员工该课程的 SYTEM 任务标记为完成
     */
    public void markCourseCompleted(Long userId, Long courseId) {
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getUserId, userId)
                        .eq(TrainingTask::getCourseId, courseId));
        for (TrainingTask t : tasks) {
            if (!"COMPLETED".equals(t.getStatus())) {
                t.setStatus("COMPLETED");
                taskMapper.updateById(t);
            }
        }
    }

    private void upsertTask(Long userId, Long jobId, Long skillId, Long courseId) {
        Long exist = taskMapper.selectCount(new LambdaQueryWrapper<TrainingTask>()
                .eq(TrainingTask::getUserId, userId)
                .eq(TrainingTask::getJobId, jobId)
                .eq(TrainingTask::getSkillId, skillId)
                .eq(TrainingTask::getCourseId, courseId));
        if (exist > 0) {
            return;
        }
        TrainingTask task = new TrainingTask();
        task.setUserId(userId);
        task.setJobId(jobId);
        task.setSkillId(skillId);
        task.setCourseId(courseId);
        task.setStatus("PENDING");
        task.setSource("SYSTEM");
        taskMapper.insert(task);
    }
}
