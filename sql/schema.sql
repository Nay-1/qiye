-- ============================================================
-- 企业岗位技能培训与智能考核系统 - 数据库结构
-- PostgreSQL 16 + pgvector（单库：业务表 + 知识库向量）
-- 说明：PRD 中 user 表为 PostgreSQL 保留字，故命名为 sys_user
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- ---------- 角色表 ----------
CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,   -- ADMIN / TRAINER / EMPLOYEE
    name        VARCHAR(64)  NOT NULL,
    remark      VARCHAR(255),
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE role IS '角色表';

-- ---------- 部门表 ----------
CREATE TABLE dept (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE dept IS '部门表';

-- ---------- 用户表（PRD: user，user 为 PG 保留字 → sys_user） ----------
CREATE TABLE sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,          -- BCrypt
    name        VARCHAR(64)  NOT NULL,
    dept_id     BIGINT REFERENCES dept(id),
    role_id     BIGINT NOT NULL REFERENCES role(id),
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE sys_user IS '用户表';

-- ---------- 岗位表 ----------
CREATE TABLE job (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    dept_id     BIGINT REFERENCES dept(id),
    description TEXT,
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE job IS '岗位表';

-- ---------- 技能表（技能自身不设等级） ----------
CREATE TABLE skill (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE skill IS '技能表';

-- ---------- 岗位技能表（核心：岗位要求的目标等级与权重） ----------
CREATE TABLE job_skill (
    id          BIGSERIAL PRIMARY KEY,
    job_id      BIGINT NOT NULL REFERENCES job(id)   ON DELETE CASCADE,
    skill_id    BIGINT NOT NULL REFERENCES skill(id) ON DELETE CASCADE,
    target_level VARCHAR(16) NOT NULL,             -- 初级/中级/高级
    weight      INT DEFAULT 1,
    UNIQUE (job_id, skill_id)
);
COMMENT ON TABLE job_skill IS '岗位技能关联（target_level 岗位目标等级）';

-- ---------- 员工岗位表（一名员工可多岗位，含主岗位） ----------
CREATE TABLE user_job (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    job_id      BIGINT NOT NULL REFERENCES job(id)      ON DELETE CASCADE,
    is_primary  BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (user_id, job_id)
);
COMMENT ON TABLE user_job IS '员工岗位关联（is_primary 是否主岗位）';

-- ---------- 课程表 ----------
CREATE TABLE course (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    category    VARCHAR(64),                     -- 技术/管理等
    level       VARCHAR(16),                     -- 难度
    job_id      BIGINT REFERENCES job(id),
    description TEXT,
    cover       VARCHAR(255),
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE course IS '课程表';

-- ---------- 课程技能表（核心：课程培养哪个技能） ----------
CREATE TABLE course_skill (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    skill_id    BIGINT NOT NULL REFERENCES skill(id)  ON DELETE CASCADE,
    weight      INT DEFAULT 1,
    required    BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (course_id, skill_id)
);
COMMENT ON TABLE course_skill IS '课程技能关联';

-- ---------- 课程章节表 ----------
CREATE TABLE course_chapter (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES course(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    seq         INT DEFAULT 0,
    created_at  TIMESTAMP DEFAULT now(),
    UNIQUE (course_id, seq)
);
COMMENT ON TABLE course_chapter IS '课程章节表';

-- ---------- 学习记录表（章节级） ----------
CREATE TABLE study_record (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    course_id      BIGINT NOT NULL REFERENCES course(id)   ON DELETE CASCADE,
    chapter_id     BIGINT NOT NULL REFERENCES course_chapter(id) ON DELETE CASCADE,
    progress       INT DEFAULT 0,                          -- 0~100
    study_duration INT DEFAULT 0,                          -- 秒
    status         VARCHAR(16) DEFAULT 'NOT_STARTED',      -- NOT_STARTED/IN_PROGRESS/COMPLETED
    started_at     TIMESTAMP,
    completed_at   TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT now(),
    UNIQUE (user_id, chapter_id)
);
COMMENT ON TABLE study_record IS '章节级学习记录';

-- ---------- 培训任务表（一条任务 = 员工 × 技能 × 课程） ----------
CREATE TABLE training_task (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    job_id      BIGINT NOT NULL REFERENCES job(id)      ON DELETE CASCADE,
    skill_id    BIGINT NOT NULL REFERENCES skill(id)    ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES course(id)   ON DELETE CASCADE,
    status      VARCHAR(16) DEFAULT 'PENDING',           -- PENDING/IN_PROGRESS/COMPLETED
    source      VARCHAR(16) DEFAULT 'SYSTEM',            -- SYSTEM/AI
    created_at  TIMESTAMP DEFAULT now(),
    UNIQUE (user_id, job_id, skill_id, course_id)
);
COMMENT ON TABLE training_task IS '培训任务（岗位→技能→课程展开）';

-- ---------- 考试表 ----------
CREATE TABLE exam (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    course_id   BIGINT REFERENCES course(id),
    duration    INT NOT NULL DEFAULT 60,                 -- 分钟
    attempts    INT DEFAULT 1,                           -- 允许次数
    pass_score  INT DEFAULT 60,                          -- 及格线
    status      VARCHAR(16) DEFAULT 'DRAFT',             -- DRAFT/PUBLISHED/CLOSED
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE exam IS '考试表';

-- ---------- 题库表 ----------
CREATE TABLE question (
    id          BIGSERIAL PRIMARY KEY,
    type        VARCHAR(16) NOT NULL,                    -- SINGLE/MULTIPLE/JUDGE
    content     TEXT NOT NULL,
    options     JSONB,                                   -- [{key:'A',text:'...'}]
    answer      TEXT NOT NULL,                           -- 单选:"A" 多选:"A,B,C" 判断:"TRUE"/"FALSE"
    analysis    TEXT,
    created_by  BIGINT REFERENCES sys_user(id),
    source      VARCHAR(16) DEFAULT 'MANUAL',            -- MANUAL/AI
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE question IS '题库表';

-- ---------- 题目技能表（核心：题↔技能，score_weight 分值权重） ----------
CREATE TABLE question_skill (
    id           BIGSERIAL PRIMARY KEY,
    question_id  BIGINT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    skill_id     BIGINT NOT NULL REFERENCES skill(id)    ON DELETE CASCADE,
    score_weight INT DEFAULT 1,
    UNIQUE (question_id, skill_id)
);
COMMENT ON TABLE question_skill IS '题目技能关联';

-- ---------- 试卷题目表 ----------
CREATE TABLE exam_question (
    id           BIGSERIAL PRIMARY KEY,
    exam_id      BIGINT NOT NULL REFERENCES exam(id)     ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES question(id) ON DELETE CASCADE,
    score        INT NOT NULL DEFAULT 10,
    sort         INT DEFAULT 0,
    UNIQUE (exam_id, question_id)
);
COMMENT ON TABLE exam_question IS '试卷题目（组卷）';

-- ---------- 考试记录表 ----------
CREATE TABLE exam_attempt (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    exam_id      BIGINT NOT NULL REFERENCES exam(id)     ON DELETE CASCADE,
    attempt_no   INT DEFAULT 1,
    total_score  NUMERIC(6,1),
    status       VARCHAR(16) DEFAULT 'IN_PROGRESS',      -- IN_PROGRESS/SUBMITTED
    started_at   TIMESTAMP DEFAULT now(),
    submitted_at TIMESTAMP
);
COMMENT ON TABLE exam_attempt IS '考试记录';

-- ---------- 学生答案表 ----------
CREATE TABLE exam_answer (
    id           BIGSERIAL PRIMARY KEY,
    attempt_id   BIGINT NOT NULL REFERENCES exam_attempt(id) ON DELETE CASCADE,
    question_id  BIGINT NOT NULL REFERENCES question(id)     ON DELETE CASCADE,
    user_answer  TEXT,
    correct      BOOLEAN,
    score        NUMERIC(6,1),
    UNIQUE (attempt_id, question_id)
);
COMMENT ON TABLE exam_answer IS '学生答案';

-- ---------- 员工技能画像表（核心） ----------
CREATE TABLE user_skill (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    skill_id     BIGINT NOT NULL REFERENCES skill(id)    ON DELETE CASCADE,
    current_level VARCHAR(16),                            -- 初级/中级/高级
    target_level  VARCHAR(16),                            -- 岗位目标（多岗取最高）
    score        NUMERIC(6,1),                            -- 0~100 聚合得分
    updated_at   TIMESTAMP DEFAULT now(),
    UNIQUE (user_id, skill_id)
);
COMMENT ON TABLE user_skill IS '员工技能画像';

-- ---------- 知识库文档表（文件元数据） ----------
CREATE TABLE knowledge_file (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    path        VARCHAR(512),
    file_type   VARCHAR(16),
    size        BIGINT,
    dept_id     BIGINT REFERENCES dept(id),             -- 可见部门（null=全部门）
    status      VARCHAR(16) DEFAULT 'PROCESSING',        -- PROCESSING/READY/FAILED
    chunk_count INT DEFAULT 0,
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT now()
);
COMMENT ON TABLE knowledge_file IS '知识库文档元数据';

-- ---------- 知识库切片表（文档 → 多向量） ----------
CREATE TABLE knowledge_chunk (
    id          BIGSERIAL PRIMARY KEY,
    file_id     BIGINT NOT NULL REFERENCES knowledge_file(id) ON DELETE CASCADE,
    seq         INT DEFAULT 0,
    content     TEXT NOT NULL,
    embedding   vector(1024),                            -- Qwen text-embedding-v3
    UNIQUE (file_id, seq)
);
COMMENT ON TABLE knowledge_chunk IS '知识库切片与向量';

CREATE INDEX idx_knowledge_chunk_embedding ON knowledge_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_knowledge_chunk_file ON knowledge_chunk(file_id);
CREATE INDEX idx_question_skill_skill ON question_skill(skill_id);
CREATE INDEX idx_user_skill_user ON user_skill(user_id);
CREATE INDEX idx_exam_attempt_user ON exam_attempt(user_id);
CREATE INDEX idx_training_task_user ON training_task(user_id);
CREATE INDEX idx_study_record_user ON study_record(user_id);
