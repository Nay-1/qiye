-- ============================================================
-- 企业岗位技能培训与智能考核系统 - 演示数据
-- 说明：所有初始账号密码均为 123456（BCrypt）
--       张三预置了一次考试提交与技能画像，用于直接演示
--       岗位→技能→课程→学习→考试→画像→AI建议 全链路
-- ============================================================

-- ---------- 角色 ----------
INSERT INTO role (id, code, name, remark) VALUES
 (1, 'ADMIN',    '系统管理员',   '系统管理权限'),
 (2, 'TRAINER',  '培训负责人',   '培训业务管理权限'),
 (3, 'EMPLOYEE', '企业员工',     '学习与考试权限');

SELECT setval('role_id_seq', 3);

-- ---------- 部门 ----------
INSERT INTO dept (id, name) VALUES
 (1, '研发部'),
 (2, '测试部'),
 (3, '产品部'),
 (4, '运维部');

SELECT setval('dept_id_seq', 4);

-- ---------- 用户（密码 123456） ----------
INSERT INTO sys_user (id, username, password, name, dept_id, role_id) VALUES
 (1, 'admin',    '$2b$12$AjbqnQB4PGJeu9I7aAG/aumoQSzDH8QlOhQY3SKUgT3aaZxIq4j2e', '管理员',   1, 1),
 (2, 'trainer',  '$2b$12$AjbqnQB4PGJeu9I7aAG/aumoQSzDH8QlOhQY3SKUgT3aaZxIq4j2e', '王培训',   1, 2),
 (3, 'zhangsan', '$2b$12$AjbqnQB4PGJeu9I7aAG/aumoQSzDH8QlOhQY3SKUgT3aaZxIq4j2e', '张三',     1, 3),
 (4, 'lisi',     '$2b$12$AjbqnQB4PGJeu9I7aAG/aumoQSzDH8QlOhQY3SKUgT3aaZxIq4j2e', '李四',     2, 3),
 (5, 'wangwu',   '$2b$12$AjbqnQB4PGJeu9I7aAG/aumoQSzDH8QlOhQY3SKUgT3aaZxIq4j2e', '王五',     4, 3);

SELECT setval('sys_user_id_seq', 5);

-- ---------- 岗位 ----------
INSERT INTO job (id, name, dept_id, description) VALUES
 (1, 'Java开发工程师', 1, '负责后端系统研发，掌握 Java/Spring Boot/数据库/缓存技术栈'),
 (2, '测试工程师',     2, '负责软件测试与质量保障'),
 (3, '运维工程师',     4, '负责系统部署与运维保障');

SELECT setval('job_id_seq', 3);

-- ---------- 技能（技能自身不设等级） ----------
INSERT INTO skill (id, name, description) VALUES
 (1, 'Java基础',    'Java 语法、面向对象、集合与异常'),
 (2, 'Spring Boot', 'Spring Boot 快速开发与 Web 应用'),
 (3, 'Redis',       'Redis 缓存、持久化与一致性问题'),
 (4, 'MySQL',       'SQL 语句、索引与性能优化'),
 (5, '软件测试',    '测试方法、用例设计与测试执行'),
 (6, 'Linux运维',   'Linux 常用命令、服务部署与排障');

SELECT setval('skill_id_seq', 6);

-- ---------- 岗位技能（核心） ----------
INSERT INTO job_skill (id, job_id, skill_id, target_level, weight) VALUES
 (1, 1, 1, '中级', 3),   -- Java开发: Java基础=中级
 (2, 1, 2, '高级', 4),   -- Java开发: Spring Boot=高级
 (3, 1, 3, '中级', 2),   -- Java开发: Redis=中级
 (4, 1, 4, '高级', 3),   -- Java开发: MySQL=高级
 (5, 2, 5, '高级', 4),   -- 测试: 软件测试=高级
 (6, 2, 4, '初级', 2),   -- 测试: MySQL=初级
 (7, 3, 6, '高级', 4),   -- 运维: Linux运维=高级
 (8, 3, 3, '中级', 2);   -- 运维: Redis=中级

SELECT setval('job_skill_id_seq', 8);

-- ---------- 员工岗位 ----------
INSERT INTO user_job (id, user_id, job_id, is_primary) VALUES
 (1, 3, 1, TRUE),   -- 张三 → Java开发（主）
 (2, 4, 2, TRUE),   -- 李四 → 测试（主）
 (3, 5, 3, TRUE);   -- 王五 → 运维（主）

SELECT setval('user_job_id_seq', 3);

-- ---------- 课程 ----------
INSERT INTO course (id, name, category, level, job_id, description) VALUES
 (1, 'Java基础入门',    '技术', '初级', 1, 'Java 语法与面向对象基础'),
 (2, 'Spring Boot实战', '技术', '高级', 1, 'Spring Boot 快速开发实战'),
 (3, 'Redis缓存优化',   '技术', '中级', 1, '缓存策略与缓存一致性'),
 (4, 'MySQL性能优化',   '技术', '高级', 1, '索引优化与慢查询治理'),
 (5, '软件测试基础',    '技术', '中级', 2, '测试方法与用例设计'),
 (6, 'Linux运维实战',   '技术', '中级', 3, 'Linux 命令与服务部署');

SELECT setval('course_id_seq', 6);

-- ---------- 课程技能 ----------
INSERT INTO course_skill (id, course_id, skill_id, weight, required) VALUES
 (1, 1, 1, 3, TRUE),
 (2, 2, 2, 4, TRUE),
 (3, 3, 3, 2, TRUE),
 (4, 4, 4, 3, TRUE),
 (5, 5, 5, 4, TRUE),
 (6, 6, 6, 4, TRUE);

SELECT setval('course_skill_id_seq', 6);

-- ---------- 课程章节 ----------
INSERT INTO course_chapter (id, course_id, title, content, seq) VALUES
 (1, 1, 'Java 基本语法',   '变量、数据类型、运算符、流程控制。', 1),
 (2, 1, '面向对象',        '类与对象、封装、继承、多态。', 2),
 (3, 2, 'Spring Boot 快速上手', '环境搭建、起步依赖、自动配置原理。', 1),
 (4, 2, 'Web 开发',        'RESTful 接口、参数校验、异常处理。', 2),
 (5, 3, '缓存策略',        '缓存穿透、击穿、雪崩的应对方案。', 1),
 (6, 3, '缓存一致性',      'Cache Aside、延迟双删等一致性方案。', 2),
 (7, 4, '索引优化',        'B+树索引、最左前缀、覆盖索引。', 1),
 (8, 4, '慢查询治理',      '慢日志定位、EXPLAIN 分析、SQL 改写。', 2),
 (9, 5, '测试方法',        '黑盒/白盒、单元/集成/系统测试。', 1),
 (10, 5, '测试用例设计',   '等价类、边界值、场景法。', 2),
 (11, 6, '常用命令',       '文件、进程、网络、权限相关命令。', 1),
 (12, 6, '服务部署',       'systemd、Nginx、日志排查。', 2);

SELECT setval('course_chapter_id_seq', 12);

-- ---------- 学习记录（张三：Java基础入门已完成，SpringBoot进行中） ----------
INSERT INTO study_record (user_id, course_id, chapter_id, progress, study_duration, status, started_at, completed_at) VALUES
 (3, 1, 1, 100, 1800, 'COMPLETED', '2026-07-01 09:00:00', '2026-07-01 09:30:00'),
 (3, 1, 2, 100, 2400, 'COMPLETED', '2026-07-02 10:00:00', '2026-07-02 10:40:00'),
 (3, 2, 3, 60,  1200, 'IN_PROGRESS', '2026-07-05 14:00:00', NULL);

-- ---------- 培训任务（张三 岗位→技能→课程 展开） ----------
INSERT INTO training_task (id, user_id, job_id, skill_id, course_id, status, source) VALUES
 (1, 3, 1, 1, 1, 'COMPLETED', 'SYSTEM'),
 (2, 3, 1, 2, 2, 'IN_PROGRESS', 'SYSTEM'),
 (3, 3, 1, 3, 3, 'PENDING', 'SYSTEM'),
 (4, 3, 1, 4, 4, 'PENDING', 'SYSTEM');

SELECT setval('training_task_id_seq', 4);

-- ---------- 题库 ----------
INSERT INTO question (id, type, content, options, answer, analysis, source) VALUES
 (1, 'SINGLE', 'Java 中基本数据类型 int 的取值范围是？',
  '[{"key":"A","text":"-32768 ~ 32767"},{"key":"B","text":"-2147483648 ~ 2147483647"},{"key":"C","text":"-2^63 ~ 2^63-1"},{"key":"D","text":"0 ~ 65535"}]'::jsonb,
  'B', 'int 为 32 位有符号整数，取值范围 -2^31 ~ 2^31-1。', 'MANUAL'),
 (2, 'SINGLE', '下列哪一项属于 Java 面向对象的三大特性之一？',
  '[{"key":"A","text":"封装"},{"key":"B","text":"递归"},{"key":"C","text":"多线程"},{"key":"D","text":"序列化"}]'::jsonb,
  'A', '面向对象三大特性：封装、继承、多态。', 'MANUAL'),
 (3, 'SINGLE', 'Spring Boot 默认内嵌的 Web 容器是？',
  '[{"key":"A","text":"Tomcat"},{"key":"B","text":"Jetty"},{"key":"C","text":"Undertow"},{"key":"D","text":"Netty"}]'::jsonb,
  'A', 'Spring Boot Web 起步依赖默认内嵌 Tomcat。', 'MANUAL'),
 (4, 'MULTIPLE', '以下哪些属于 Spring Boot 的核心特性？（多选）',
  '[{"key":"A","text":"自动配置"},{"key":"B","text":"起步依赖 Starter"},{"key":"C","text":"内嵌 Web 容器"},{"key":"D","text":"分布式事务"}]'::jsonb,
  'A,B,C', '自动配置、起步依赖、内嵌容器是 Spring Boot 核心特性。', 'MANUAL'),
 (5, 'SINGLE', 'Redis 默认服务端口号是？',
  '[{"key":"A","text":"3306"},{"key":"B","text":"5432"},{"key":"C","text":"6379"},{"key":"D","text":"8080"}]'::jsonb,
  'C', 'Redis 默认端口为 6379。', 'MANUAL'),
 (6, 'JUDGE', 'Redis 支持 RDB 与 AOF 两种持久化机制。',
  NULL, 'TRUE', 'RDB 快照 + AOF 追加日志，两种持久化机制。', 'MANUAL'),
 (7, 'SINGLE', 'MySQL InnoDB 引擎默认的索引数据结构是？',
  '[{"key":"A","text":"哈希表"},{"key":"B","text":"B+ 树"},{"key":"C","text":"红黑树"},{"key":"D","text":"跳表"}]'::jsonb,
  'B', 'InnoDB 聚簇索引基于 B+ 树。', 'MANUAL'),
 (8, 'SINGLE', '下列哪个 SQL 语句可以去除查询结果中的重复行？',
  '[{"key":"A","text":"SELECT DISTINCT name FROM t;"},{"key":"B","text":"SELECT UNIQUE name FROM t;"},{"key":"C","text":"SELECT DIFFERENT name FROM t;"},{"key":"D","text":"SELECT ALL name FROM t;"}]'::jsonb,
  'A', 'DISTINCT 用于去重。', 'MANUAL'),
 (9, 'SINGLE', '黑盒测试主要关注的是？',
  '[{"key":"A","text":"程序内部逻辑"},{"key":"B","text":"软件外部功能"},{"key":"C","text":"代码覆盖率"},{"key":"D","text":"内存泄漏"}]'::jsonb,
  'B', '黑盒测试不关注内部实现，只验证外部功能是否符合需求。', 'MANUAL'),
 (10, 'MULTIPLE', '以下属于测试用例基本要素的有？（多选）',
  '[{"key":"A","text":"测试步骤"},{"key":"B","text":"预期结果"},{"key":"C","text":"前置条件"},{"key":"D","text":"源代码"}]'::jsonb,
  'A,B,C', '测试用例要素：编号、前置条件、步骤、输入数据、预期结果等。', 'MANUAL'),
 (11, 'SINGLE', 'Linux 中用于查看进程信息的命令是？',
  '[{"key":"A","text":"ps"},{"key":"B","text":"ls"},{"key":"C","text":"cat"},{"key":"D","text":"pwd"}]'::jsonb,
  'A', 'ps 用于查看进程快照。', 'MANUAL'),
 (12, 'JUDGE', 'chmod 755 表示文件属主拥有读写执行权限。',
  NULL, 'TRUE', '7=rwx 属主；5=rx 属组；5=rx 其他。', 'MANUAL');

SELECT setval('question_id_seq', 12);

-- ---------- 题目技能（每题绑定一个技能） ----------
INSERT INTO question_skill (id, question_id, skill_id, score_weight) VALUES
 (1,  1, 1, 1),
 (2,  2, 1, 1),
 (3,  3, 2, 1),
 (4,  4, 2, 1),
 (5,  5, 3, 1),
 (6,  6, 3, 1),
 (7,  7, 4, 1),
 (8,  8, 4, 1),
 (9,  9, 5, 1),
 (10, 10, 5, 1),
 (11, 11, 6, 1),
 (12, 12, 6, 1);

SELECT setval('question_skill_id_seq', 12);

-- ---------- 考试 ----------
INSERT INTO exam (id, title, course_id, duration, attempts, pass_score, status, created_by) VALUES
 (1, 'Java开发岗技能考核', NULL, 60, 2, 60, 'PUBLISHED', 2),
 (2, '测试工程师基础考核', NULL, 30, 1, 60, 'PUBLISHED', 2),
 (3, '运维技能考核',       NULL, 30, 1, 60, 'PUBLISHED', 2);

SELECT setval('exam_id_seq', 3);

-- ---------- 组卷（Java 岗考核 8 题，每题 10 分） ----------
INSERT INTO exam_question (id, exam_id, question_id, score, sort) VALUES
 (1, 1, 1, 10, 1),
 (2, 1, 2, 10, 2),
 (3, 1, 3, 10, 3),
 (4, 1, 4, 10, 4),
 (5, 1, 5, 10, 5),
 (6, 1, 6, 10, 6),
 (7, 1, 7, 10, 7),
 (8, 1, 8, 10, 8),
 (9,  2, 9,  20, 1),
 (10, 2, 10, 20, 2),
 (11, 3, 11, 20, 1),
 (12, 3, 12, 20, 2);

SELECT setval('exam_question_id_seq', 12);

-- ---------- 考试记录（张三提交 Java 岗考核：8题对6题=60分） ----------
INSERT INTO exam_attempt (id, user_id, exam_id, attempt_no, total_score, status, started_at, submitted_at) VALUES
 (1, 3, 1, 1, 60.0, 'SUBMITTED', '2026-07-10 10:00:00', '2026-07-10 10:30:00');

SELECT setval('exam_attempt_id_seq', 1);

-- ---------- 学生答案（对应得分：60/80；多选 Q4 漏选不给分） ----------
INSERT INTO exam_answer (id, attempt_id, question_id, user_answer, correct, score) VALUES
 (1, 1, 1, 'B',     TRUE,  10.0),
 (2, 1, 2, 'A',     TRUE,  10.0),
 (3, 1, 3, 'A',     TRUE,  10.0),
 (4, 1, 4, 'A,B',   FALSE, 0.0),
 (5, 1, 5, 'C',     TRUE,  10.0),
 (6, 1, 6, 'FALSE', FALSE, 0.0),
 (7, 1, 7, 'B',     TRUE,  10.0),
 (8, 1, 8, 'A',     TRUE,  10.0);

SELECT setval('exam_answer_id_seq', 8);

-- ---------- 员工技能画像（张三：由上面考试聚合，薄弱技能=Spring Boot） ----------
INSERT INTO user_skill (id, user_id, skill_id, current_level, target_level, score) VALUES
 (1, 3, 1, '高级', '中级', 100.0),   -- Java基础：达标
 (2, 3, 2, '初级', '高级', 50.0),    -- Spring Boot：62.5% 未达标 → 薄弱
 (3, 3, 3, '初级', '中级', 50.0),    -- Redis：83% 达标
 (4, 3, 4, '高级', '高级', 100.0);   -- MySQL：达标

SELECT setval('user_skill_id_seq', 4);
