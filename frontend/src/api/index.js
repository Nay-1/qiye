import request from '@/utils/request'

// 认证
export const login = (username, password) => request.post('/auth/login', { username, password })
export const getMe = () => request.get('/auth/me')

// 部门
export const deptList = () => request.get('/dept/list')
export const deptCreate = data => request.post('/dept', data)
export const deptUpdate = data => request.put('/dept', data)
export const deptDelete = id => request.delete(`/dept/${id}`)

// 角色
export const roleList = () => request.get('/role/list')

// 用户
export const userPage = params => request.get('/user/page', { params })
export const userCreate = data => request.post('/user', data)
export const userUpdate = data => request.put('/user', data)
export const userResetPwd = (id, password) => request.put(`/user/${id}/password`, { password })
export const userToggleStatus = id => request.put(`/user/${id}/status`)
export const userDelete = id => request.delete(`/user/${id}`)

// 岗位 / 技能
export const jobList = () => request.get('/job/list')
export const jobCreate = data => request.post('/job', data)
export const jobUpdate = data => request.put('/job', data)
export const jobDelete = id => request.delete(`/job/${id}`)
export const skillList = () => request.get('/skill/list')
export const skillCreate = data => request.post('/skill', data)
export const skillUpdate = data => request.put('/skill', data)
export const skillDelete = id => request.delete(`/skill/${id}`)
export const jobSkills = jobId => request.get(`/job-skill/job/${jobId}`)
export const jobSkillsSave = (jobId, items) => request.post(`/job-skill/job/${jobId}`, { items })

// 员工岗位
export const userJobOf = userId => request.get(`/user-job/user/${userId}`)
export const userJobAssign = data => request.post('/user-job/assign', data)

// 培训任务
export const taskMy = () => request.get('/training-task/my')
export const taskByUser = userId => request.get(`/training-task/user/${userId}`)
export const taskPage = params => request.get('/training-task/page', { params })

// 课程
export const coursePage = params => request.get('/course/page', { params })
export const courseDetail = id => request.get(`/course/${id}`)
export const courseCreate = data => request.post('/course', data)
export const courseUpdate = data => request.put('/course', data)
export const courseDelete = id => request.delete(`/course/${id}`)
export const courseSkills = courseId => request.get(`/course-skill/course/${courseId}`)
export const courseSkillsSave = (courseId, items) => request.post(`/course-skill/course/${courseId}`, { items })
export const chaptersSave = (courseId, chapters) => request.post(`/chapter/course/${courseId}`, chapters)
export const chapterDelete = id => request.delete(`/chapter/${id}`)

// 学习
export const studyStart = data => request.post('/study/start', data)
export const studyProgress = data => request.post('/study/progress', data)
export const studyMyCourse = courseId => request.get(`/study/course/${courseId}`)
export const studyMy = () => request.get('/study/my')
export const studyByUser = userId => request.get(`/study/user/${userId}`)

// 题库
export const questionPage = params => request.get('/question/page', { params })
export const questionDetail = id => request.get(`/question/${id}`)
export const questionCreate = data => request.post('/question', data)
export const questionUpdate = data => request.put('/question', data)
export const questionDelete = id => request.delete(`/question/${id}`)

// 考试
export const examPage = params => request.get('/exam/page', { params })
export const examDetail = id => request.get(`/exam/${id}`)
export const examCreate = data => request.post('/exam', data)
export const examUpdate = data => request.put('/exam', data)
export const examDelete = id => request.delete(`/exam/${id}`)
export const examSaveQuestions = (id, items) => request.post(`/exam/${id}/questions`, { items })
export const examPublish = id => request.put(`/exam/${id}/publish`)
export const examClose = id => request.put(`/exam/${id}/close`)

// 考试作答
export const attemptStart = examId => request.post('/exam-attempt/start', { examId })
export const attemptSubmit = data => request.post('/exam-attempt/submit', data)
export const attemptDetail = id => request.get(`/exam-attempt/${id}`)
export const attemptMy = () => request.get('/exam-attempt/my')
export const attemptByUser = userId => request.get(`/exam-attempt/user/${userId}`)
export const attemptByExam = examId => request.get(`/exam-attempt/exam/${examId}`)

// 技能画像
export const skillProfileMine = () => request.get('/user-skill/mine')
export const skillProfileUser = userId => request.get(`/user-skill/user/${userId}`)
export const skillRecalc = () => request.post('/user-skill/recalc')

// 知识库
export const knowledgeUpload = (formData) => request.post('/knowledge/upload', formData, {
  headers: { 'Content-Type': 'multipart/form-data' }
})
export const knowledgePage = params => request.get('/knowledge/page', { params })
export const knowledgeDetail = id => request.get(`/knowledge/${id}`)
export const knowledgeDelete = id => request.delete(`/knowledge/${id}`)
export const knowledgeSearch = data => request.post('/knowledge/search', data)

// AI
export const aiChat = data => request.post('/ai/chat', data)
export const aiGenerateQuestions = data => request.post('/ai/generate-questions', data)
export const aiStudyAdvice = () => request.post('/ai/study-advice')

// 统计
export const statsOverview = () => request.get('/stats/overview')
export const statsStudy = () => request.get('/stats/study')
export const statsExam = () => request.get('/stats/exam')
export const statsSkill = () => request.get('/stats/skill')
export const statsDept = () => request.get('/stats/dept')
export const statsRanking = () => request.get('/stats/ranking')
