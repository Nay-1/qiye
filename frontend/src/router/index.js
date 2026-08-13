import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue') },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页' } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'system', name: 'System', component: () => import('@/views/SystemManage.vue'), meta: { title: '系统管理', roles: ['ADMIN'] } },
      { path: 'jobs', name: 'Jobs', component: () => import('@/views/JobSkillManage.vue'), meta: { title: '岗位技能', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'courses', name: 'Courses', component: () => import('@/views/CourseManage.vue'), meta: { title: '课程管理', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'my-courses', name: 'MyCourses', component: () => import('@/views/MyCourses.vue'), meta: { title: '我的课程' } },
      { path: 'learn/:id', name: 'Learn', component: () => import('@/views/CourseLearn.vue'), meta: { title: '课程学习' } },
      { path: 'questions', name: 'Questions', component: () => import('@/views/QuestionManage.vue'), meta: { title: '题库管理', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'exams', name: 'Exams', component: () => import('@/views/ExamManage.vue'), meta: { title: '考试管理', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'exam-take/:attemptId', name: 'ExamTake', component: () => import('@/views/ExamTake.vue'), meta: { title: '在线考试' } },
      { path: 'exam-result/:attemptId', name: 'ExamResult', component: () => import('@/views/ExamResult.vue'), meta: { title: '成绩详情' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/ProfileView.vue'), meta: { title: '技能画像' } },
      { path: 'profile/:userId', name: 'ProfileUser', component: () => import('@/views/ProfileView.vue'), meta: { title: '技能画像', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'knowledge', name: 'Knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { title: '知识库', roles: ['ADMIN', 'TRAINER'] } },
      { path: 'ai', name: 'Ai', component: () => import('@/views/AiAssistant.vue'), meta: { title: 'AI 智能助手' } },
      { path: 'stats', name: 'Stats', component: () => import('@/views/StatsView.vue'), meta: { title: '统计分析', roles: ['ADMIN', 'TRAINER'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const store = useUserStore()
  if (to.path === '/login') return next()
  if (!store.token) return next('/login')
  if (to.meta.roles && !to.meta.roles.includes(store.roleCode)) {
    return next('/home')
  }
  next()
})

export default router
