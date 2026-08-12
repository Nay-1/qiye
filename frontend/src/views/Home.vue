<template>
  <div>
    <!-- 管理员/培训负责人：快捷入口 -->
    <template v-if="store.canManage">
      <el-card shadow="never">
        <template #header><b>快捷入口</b></template>
        <div class="quick-grid">
          <div v-for="q in quickLinks" :key="q.path" class="quick-item" @click="$router.push(q.path)">
            <el-icon :size="28"><component :is="q.icon" /></el-icon>
            <div>{{ q.title }}</div>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 员工：个人概览 -->
    <template v-else>
      <el-row :gutter="16" class="cards">
        <el-col :span="6"><el-card shadow="never">
          <div class="card-num">{{ taskStats.total }}</div><div class="card-label">学习任务</div>
        </el-card></el-col>
        <el-col :span="6"><el-card shadow="never">
          <div class="card-num">{{ taskStats.completed }}</div><div class="card-label">已完成任务</div>
        </el-card></el-col>
        <el-col :span="6"><el-card shadow="never">
          <div class="card-num">{{ profile.weakCount ?? '-' }}</div><div class="card-label">薄弱技能</div>
        </el-card></el-col>
        <el-col :span="6"><el-card shadow="never">
          <div class="card-num">{{ passedExamCount }}</div><div class="card-label">通过考试</div>
        </el-card></el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="14">
          <el-card shadow="never">
            <template #header><b>我的学习任务</b><el-button text type="primary" style="float:right" @click="$router.push('/courses')">去学习</el-button></template>
            <el-table :data="tasks" size="small" max-height="300">
              <el-table-column prop="skillName" label="技能" width="110" />
              <el-table-column prop="courseName" label="课程" />
              <el-table-column prop="courseLevel" label="难度" width="70" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="goLearn(row)">学习</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card shadow="never">
            <template #header><b>可参加的考试</b></template>
            <el-table :data="availableExams" size="small" max-height="300">
              <el-table-column prop="title" label="考试" />
              <el-table-column label="操作" width="80" align="center">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="takeExam(row)">参加</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { taskMy, examPage, attemptMy, skillProfileMine, attemptStart } from '@/api'

const store = useUserStore()
const router = useRouter()
const tasks = ref([])
const exams = ref([])
const attempts = ref([])
const profile = ref({})

const quickLinks = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine' },
  { path: '/jobs', title: '岗位技能', icon: 'Briefcase' },
  { path: '/courses', title: '课程管理', icon: 'Reading' },
  { path: '/questions', title: '题库管理', icon: 'Document' },
  { path: '/exams', title: '考试管理', icon: 'EditPen' },
  { path: '/stats', title: '统计分析', icon: 'TrendCharts' },
  { path: '/knowledge', title: '知识库', icon: 'FolderOpened' },
  { path: '/ai', title: 'AI 助手', icon: 'MagicStick' }
]

const taskStats = computed(() => ({
  total: tasks.value.length,
  completed: tasks.value.filter(t => t.status === 'COMPLETED').length
}))
const passedExamCount = computed(() => attempts.value.filter(a => a.passed).length)

const availableExams = computed(() => {
  const submittedCount = {}
  attempts.value.forEach(a => { submittedCount[a.examId] = (submittedCount[a.examId] || 0) + 1 })
  return exams.value.filter(e => {
    if (e.status !== 'PUBLISHED') return false
    const max = e.attempts || 1
    return (submittedCount[e.id] || 0) < max
  })
})

function statusTag(s) { return { PENDING: 'info', IN_PROGRESS: 'warning', COMPLETED: 'success' }[s] || 'info' }
function statusText(s) { return { PENDING: '待学', IN_PROGRESS: '学习中', COMPLETED: '已完成' }[s] || s }

function goLearn(row) { router.push(`/learn/${row.courseId}`) }
async function takeExam(exam) {
  try {
    const paper = await attemptStart(exam.id)
    router.push(`/exam-take/${paper.attemptId}`)
  } catch (e) { /* 已提示 */ }
}

onMounted(async () => {
  tasks.value = await taskMy()
  if (store.isEmployee) {
    exams.value = await examPage({ page: 1, size: 50 })
    attempts.value = await attemptMy()
    profile.value = await skillProfileMine()
  }
})
</script>

<style scoped>
.cards { margin-bottom: 16px; }
.card-num { font-size: 26px; font-weight: 700; color: #1e3a8a; }
.card-label { color: #6b7280; font-size: 13px; margin-top: 4px; }
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.quick-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px; text-align: center; cursor: pointer; color: #374151; transition: all .2s; }
.quick-item:hover { border-color: #3b82f6; color: #2563eb; box-shadow: 0 4px 12px rgba(59,130,246,.15); }
.quick-item div { margin-top: 8px; }
</style>
