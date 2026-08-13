package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiye.ai.LlmClient;
import com.qiye.common.BizException;
import com.qiye.entity.Course;
import com.qiye.entity.CourseSkill;
import com.qiye.entity.Question;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.CourseSkillMapper;
import com.qiye.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 智能培训助手
 * 功能1：RAG 问答（检索 → LLM → 回答+来源）
 * 功能2：AI 生成试题（草稿，入库需人工审核 + 强制绑定 question_skill）
 * 功能3：AI 学习建议（业务规则判定薄弱技能 → 推荐课程 → LLM 文案）
 * 所有功能：LLM 不可用时降级为规则/检索结果，不阻塞业务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final LlmClient llmClient;
    private final KnowledgeService knowledgeService;
    private final UserSkillService userSkillService;
    private final CourseSkillMapper courseSkillMapper;
    private final CourseMapper courseMapper;
    private final ObjectMapper objectMapper;

    // ========== 功能1：RAG 问答 ==========

    public Map<String, Object> chat(String question, LoginUser user) {
        boolean adminOrTrainer = "ADMIN".equals(user.getRoleCode()) || "TRAINER".equals(user.getRoleCode());
        List<Map<String, Object>> hits = knowledgeService.search(question, adminOrTrainer, user.getDeptId(), 5);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("question", question);

        if (hits.isEmpty()) {
            resp.put("answer", "知识库暂未收录相关内容，请联系培训管理员完善知识库后再咨询。");
            resp.put("sources", List.of());
            resp.put("fromLlm", false);
            return resp;
        }

        StringBuilder context = new StringBuilder();
        Set<String> sources = new LinkedHashSet<>();
        int i = 1;
        for (Map<String, Object> hit : hits) {
            String name = String.valueOf(hit.getOrDefault("fileName", "未知文档"));
            String content = String.valueOf(hit.getOrDefault("content", ""));
            context.append('[').append(i++).append("] 来源《").append(name).append("》：").append(content).append('\n');
            sources.add(name);
        }

        String system = "你是企业智能培训助手。只能依据给定的知识库资料回答问题。"
                + "如果资料与问题无关或资料不足以回答，必须回复：知识库暂未收录相关内容，请联系培训管理员完善知识库。"
                + "回答请简洁、有条理，可用序号。";
        String userPrompt = "知识库资料：\n" + context + "\n问题：" + question;

        try {
            String answer = llmClient.chat(system, userPrompt, 0.3);
            if (!StringUtils.hasText(answer)) {
                answer = "知识库暂未收录相关内容，请联系培训管理员完善知识库。";
            }
            resp.put("answer", answer);
            resp.put("sources", new ArrayList<>(sources));
            resp.put("fromLlm", true);
        } catch (Exception e) {
            log.warn("AI 对话不可用，降级为检索结果：{}", e.getMessage());
            resp.put("answer", "AI 服务暂不可用（未配置大模型 API Key）。以下是根据知识库检索到的相关内容：\n"
                    + hits.stream().map(h -> "- " + h.getOrDefault("content", "")).reduce("", (a, b) -> a + b + "\n").strip());
            resp.put("sources", new ArrayList<>(sources));
            resp.put("fromLlm", false);
        }
        return resp;
    }

    // ========== 功能2：AI 生成试题（草稿） ==========

    public List<Question> generateQuestions(String topic, Integer count, String type) {
        int n = count == null || count <= 0 ? 5 : Math.min(count, 20);
        String types = StringUtils.hasText(type) ? type
                : (n == 1 ? "单选题" : "单选/多选/判断题");
        String system = "你是企业培训平台的出题专家。请严格按照 JSON 数组格式输出题目，不要输出任何其他文字。"
                + "每个题目对象包含字段：type(SINGLE/MULTIPLE/JUDGE)、content(题干)、options(选项数组，每题约4项，元素为{key,text}，判断题留空数组)、answer(正确答案，多选用逗号分隔如\"A,B\")、analysis(答案解析)。";
        String userPrompt = "请围绕主题《" + topic + "》生成 " + n + " 道" + types + "，题目难度适中，适合企业员工技能考核。";

        try {
            String content = llmClient.chat(system, userPrompt, 0.7);
            return parseQuestionsJson(content);
        } catch (Exception e) {
            log.warn("AI 出题不可用：{}", e.getMessage());
            throw new BizException("AI 服务暂不可用（未配置大模型 API Key），无法生成试题");
        }
    }

    /** 解析 LLM 返回的 JSON 数组文本为题目列表（AI 出题 / 批量导入 AI 识别复用） */
    public List<Question> parseQuestionsJson(String content) {
        try {
            String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode arr = objectMapper.readTree(json);
            List<Question> list = new ArrayList<>();
            for (JsonNode node : arr) {
                Question q = new Question();
                q.setType(node.path("type").asText("SINGLE"));
                q.setContent(node.path("content").asText());
                q.setAnswer(node.path("answer").asText());
                q.setAnalysis(node.path("analysis").asText(""));
                List<Question.OptionItem> options = new ArrayList<>();
                for (JsonNode opt : node.path("options")) {
                    options.add(new Question.OptionItem(opt.path("key").asText(), opt.path("text").asText()));
                }
                q.setOptions(options);
                q.setSource("AI");
                list.add(q);
            }
            return list;
        } catch (Exception e) {
            log.error("解析 AI 生成试题失败", e);
            throw new BizException("AI 返回的题目格式无法解析，请重试");
        }
    }

    // ========== 功能3：AI 学习建议（规则 + LLM） ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> studyAdvice(Long userId, String userName) {
        Map<String, Object> profile = userSkillService.profile(userId);
        List<Map<String, Object>> skills = (List<Map<String, Object>>) profile.get("skills");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> s : skills) {
            if (!Boolean.TRUE.equals(s.get("weak"))) {
                continue;   // 仅薄弱技能
            }
            Long skillId = ((Number) s.get("skillId")).longValue();
            String skillName = String.valueOf(s.get("skillName"));
            BigDecimal score = (BigDecimal) s.get("score");
            String target = String.valueOf(s.get("targetLevel"));
            BigDecimal rate = (BigDecimal) s.get("rate");

            List<Course> courses = recommendCourses(skillId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("skillName", skillName);
            item.put("score", score);
            item.put("targetLevel", target);
            item.put("rate", rate);
            item.put("courses", courses);

            String advice;
            try {
                advice = llmClient.chat(
                        "你是企业培训教练，基于员工技能画像给出针对性学习建议。语气专业、简洁、可执行。",
                        buildAdvicePrompt(userName, skillName, score, target, rate, courses), 0.6);
            } catch (Exception e) {
                log.warn("AI 建议不可用，使用规则模板：{}", e.getMessage());
                advice = buildRuleAdvice(skillName, score, target, rate, courses);
            }
            item.put("advice", advice);
            result.add(item);
        }
        return result;
    }

    private String buildAdvicePrompt(String userName, String skill, BigDecimal score,
                                     String target, BigDecimal rate, List<Course> courses) {
        String courseNames = courses.isEmpty() ? "暂无推荐课程"
                : String.join("、", courses.stream().map(Course::getName).toList());
        return "员工" + userName + "在技能【" + skill + "】上的考试得分为 " + score
                + "，岗位目标等级为" + target + "，达成率 " + rate + "%（未达标，判定为薄弱技能）。"
                + "推荐学习课程：" + courseNames + "。请给出 2-3 条具体可执行的学习建议，并说明差距原因。";
    }

    private String buildRuleAdvice(String skill, BigDecimal score, String target,
                                   BigDecimal rate, List<Course> courses) {
        String courseNames = courses.isEmpty() ? "相关课程"
                : String.join("、", courses.stream().map(Course::getName).toList());
        return "你的【" + skill + "】技能当前得分 " + score + "，岗位目标等级为" + target
                + "，达成率 " + rate + "% 未达标。建议优先学习《" + courseNames + "》，"
                + "重点加强薄弱知识点练习，并参加对应岗位考核验证掌握程度。";
    }

    private List<Course> recommendCourses(Long skillId) {
        List<Long> courseIds = courseSkillMapper.selectList(
                        new LambdaQueryWrapper<CourseSkill>().eq(CourseSkill::getSkillId, skillId))
                .stream().map(CourseSkill::getCourseId).toList();
        return courseIds.isEmpty() ? List.of() : courseMapper.selectBatchIds(courseIds);
    }
}
