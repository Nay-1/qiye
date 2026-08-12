# 企业岗位技能培训与智能考核系统

基于 **Spring Boot 3 + Vue 3 + PostgreSQL(pgvector)** 的企业内部岗位能力培养闭环系统。
从"传课程 + 考试"升级为以**岗位技能为导向**的精准培养：岗位需求 → 技能体系 → 课程学习 → 在线考核 → 技能画像 → 薄弱技能识别 → AI 学习建议。

---

## 一、技术栈

| 环节 | 方案 |
|---|---|
| 后端 | Spring Boot 3.5 + MyBatis-Plus + Spring Security + JWT |
| 前端 | Vue 3 + Vite + Element Plus + ECharts + Pinia |
| 数据库 | PostgreSQL 16 + pgvector（单库：业务表 + 知识库向量） |
| 文档解析 | Apache PDFBox / POI |
| AI | OpenCode Go（对话/出题）+ 通义 Qwen text-embedding-v3（向量化），OpenAI 兼容协议，可切换可降级 |

> **实现说明**：AI 层用 Spring Boot 内置 RestClient 手写 OpenAI 兼容调用（`com.qiye.ai` 包），
> 未引入 Spring AI 框架——避免其与 Spring Boot 的版本强绑定及国产模型的兼容坑，同时可完全控制降级逻辑。
> PRD 中 `user` 表为 PostgreSQL 保留字，实际建表命名为 `sys_user`，其余表结构完全按 PRD。

## 二、核心业务链

```
岗位(job) → 岗位技能(job_skill, target_level)
         → 课程技能(course_skill) → 培训任务(training_task 自动展开)
         → 学习记录(study_record) → 在线考试(exam/question/exam_attempt)
         → 技能画像(user_skill, 按题目-技能聚合得分)
         → 薄弱技能识别 → 推荐课程 → AI 学习建议
```

- **技能得分**：该技能最近一次考核提交的实得分 ÷ 满分 × 100（按 `question_skill` 绑定聚合）。
- **等级换算**：score≥80 高级；60≤score<80 中级；<60 初级。
- **达成率**：score ÷ 目标等级达标线（初/中级 60、高级 80）× 100%，封顶 100%，<100% 判定薄弱。
- **多岗位合并**：员工挂多个岗位时，`target_level` 取各岗位最高等级。

## 三、目录结构

```
├── docker-compose.yml      # PostgreSQL + pgvector（含初始化 SQL）
├── sql/
│   ├── schema.sql          # 21 张表 + 向量索引
│   └── data.sql            # 演示数据（角色/部门/用户/岗位/技能/课程/题目/画像）
├── backend/                # Spring Boot 后端（端口 8080，context-path /api）
│   └── src/main/java/com/qiye/
│       ├── controller/     # REST 接口
│       ├── service/        # 业务逻辑（任务生成/评分/画像聚合/统计/AI）
│       ├── ai/             # LlmClient / EmbeddingClient（OpenAI 兼容）
│       ├── security/       # JWT + RBAC
│       └── common/         # 统一响应 / 异常处理
└── frontend/               # Vue3 前端（端口 5173，/api 代理到 8080）
```

## 四、快速启动

```bash
# 1. 启动数据库（首次自动建表 + 写入演示数据）
docker compose up -d

# 2. 启动后端
cd backend
./start-backend.sh        # 已注入 JVM 代理（OpenCode Go 在海外，需代理访问）
# 或手动：mvn spring-boot:run（需自行设置 JAVA_TOOL_OPTIONS 代理）

# 3. 启动前端
cd frontend
npm install
npm run dev

# 访问 http://localhost:5173
```

**演示账号（密码均为 123456）**
| 账号 | 角色 | 说明 |
|---|---|---|
| admin | 系统管理员 | 用户/部门/岗位/课程/考试全部管理 + 数据看板 |
| trainer | 培训负责人 | 岗位技能/课程/题库/考试/知识库/AI 出题 |
| zhangsan | 企业员工 | 预置一次考试与技能画像（Spring Boot 62.5% 薄弱），可直接演示 AI 建议 |

## 五、AI 配置（已接入真实模型，默认 mock 降级）

> 本机已配置好两个 Key，`ai.provider=opencode`（对话/出题/建议走 OpenCode Go，向量化走通义 Qwen）。
> **关键点：OpenCode Go 服务器在海外，后端启动时必须注入 JVM 代理**（见 `backend/start-backend.sh`），
> 否则调用会超时。若本机代理端口不是 7890，修改脚本即可。

AI 配置位于 `backend/src/main/resources/application.yml`：

```yaml
ai:
  provider: opencode          # mock | opencode | dashscope
  opencode:                   # OpenCode Go：对话 / 出题 / 建议文案
    base-url: https://opencode.ai/zen/go/v1   # OpenAI 兼容端点（/v1/chat/completions）
    api-key: ${OPENCODE_API_KEY}
    chat-model: deepseek-v4-flash
  dashscope:                  # Qwen text-embedding：知识库向量化
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    api-key: ${DASHSCOPE_API_KEY}
    embedding-model: text-embedding-v3
```

- `provider=opencode`：对话走 OpenCode Go，知识库向量化走 DashScope embedding（无 DashScope Key 时检索自动降级为关键词匹配）。
- `provider=mock`：AI 问答返回检索结果 + "AI 服务暂不可用"提示；AI 出题明确提示不可用；学习建议用规则模板生成。

## 六、模块清单

1. **用户权限**：部门/用户/角色管理，JWT + RBAC（管理员/培训负责人/员工三角色）
2. **岗位技能体系**：岗位、技能、岗位技能配置（目标等级+权重）、员工岗位分配、培训任务自动生成
3. **课程学习**：课程/章节/技能关联、章节级学习进度、学完联动任务完成
4. **在线考试**：题库（单选/多选/判断 + 题目-技能绑定）、组卷、在线答题、自动评分
5. **技能画像**：题目-技能得分聚合、达成率、薄弱技能识别、雷达图
6. **知识库 RAG**：PDF/Word/TXT 上传 → 解析 → 切片 → 向量化(pgvector) → 部门隔离检索
7. **AI 助手**：RAG 问答（可溯源）、AI 生成试题（人工审核 + 强制绑定 question_skill）、AI 学习建议（规则判定 + LLM 文案）
8. **数据统计**：学习/考试/技能达成/部门完成率/员工排名（ECharts 看板）

## 七、答辩亮点

1. **"岗位—技能—课程—试题—画像"全链路数据模型**：从"传课程"升级为岗位技能导向的精准培养闭环，技能达成率可量化。
2. **技能画像与薄弱技能识别**：考试得分按题目-技能聚合，雷达图 + 达成率可视化，培训效果可度量。
3. **基于知识库的 RAG 智能问答**：回答可溯源、按部门隔离、无结果时明确告知而非硬编（边界规则）。
4. **业务规则 + LLM 协作**：能力判定交给规则引擎（不直接让大模型判分），LLM 只生成自然语言建议，稳定可靠。
5. **AI 调用多 Provider 可切换 + 全链路降级**：无 API Key 也能完整演示，符合生产降级规范。
