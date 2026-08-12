<template>
  <div>
    <!-- ============ 员工：成长主页 ============ -->
    <template v-if="!store.canManage">
      <el-row :gutter="16">
        <!-- 签名：达标靶环 -->
        <el-col :xs="24" :md="9">
          <el-card class="ring-card">
            <template #header>达标靶环</template>
            <div class="ring-wrap">
              <svg viewBox="0 0 320 320" class="ring-svg" role="img" aria-label="综合达标率靶环">
                <!-- 等高线环 -->
                <circle cx="160" cy="160" r="132" class="r c1"/>
                <circle cx="160" cy="160" r="118" class="r c2"/>
                <circle cx="160" cy="160" r="104" class="r c3"/>
                <circle cx="160" cy="160" r="80" class="r c4"/>
                <circle cx="160" cy="160" r="56" class="r c5"/>
                <circle cx="160" cy="160" r="34" class="r c6"/>

                <!-- 达标基线（满环） -->
                <circle cx="160" cy="160" r="120" class="ring-base"/>
                <text x="160" y="38" class="ring-base-label" text-anchor="middle">达标线</text>

                <!-- 已攀爬的达标弧 -->
                <circle
                  cx="160" cy="160" r="120"
                  class="ring-progress"
                  :style="{ strokeDasharray: `${arcDash} ${C}`, strokeDashoffset: '0' }"
                />
                <!-- 技能点（每个技能 = 环上一步） -->
                <g v-for="(s, i) in skillMarks" :key="i">
                  <circle :cx="s.x" :cy="s.y" r="5.5" class="skill-dot" :class="s.cls" :fill="s.color" stroke="#FFFFFF" stroke-width="1.5">
                    <title>{{ s.name }} · {{ s.rate ?? '—' }}%</title>
                  </circle>
                </g>

                <!-- 中心达标率 -->
                <text x="160" y="156" class="ring-num" text-anchor="middle">{{ overallRate }}</text>
                <text x="160" y="180" class="ring-num-label" text-anchor="middle">综合达标率 %</text>
              </svg>

              <div class="legend">
                <span class="lg"><i style="background:#1C6B4F" />已达标 {{ reachedCount }}</span>
                <span class="lg"><i style="background:#E5A13C" />达标中 {{ midCount }}</span>
                <span class="lg"><i style="background:#C2563B" />薄弱 {{ weakCount }}</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 成长简报 -->
        <el-col :xs="24" :md="15">
          <el-card class="brief-card">
            <template #header>成长简报</template>

            <div class="stat-grid">
              <div class="stat" v-for="c in statChips" :key="c.label">
                <div class="stat-num serif-num" :class="c.tone">{{ c.value }}</div>
                <div class="stat-label">{{ c.label }}</div>
              </div>
            </div>

            <div class="brief-block">
              <div class="brief-title">下一步 · 攻薄弱技能</div>
              <div class="weak-chips">
                <template v-if="weakSkills.length">
                  <span v-for="w in weakSkills" :key="w.skillName" class="weak-chip" @click="goAdvice(w)">
                    {{ w.skillName }}<el-icon :size="12"><Right /></el-icon>
                  </span>
                </template>
                <span v-else class="weak-none">暂未发现薄弱技能，继续保持 🎉</span>
              </div>
              <el-button type="primary" class="advice-btn" @click="$router.push('/ai')">
                <el-icon :size="15"><MagicStick /></el-icon>向 AI 请教下一步
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="mt16">
        <el-col :xs="24" :md="14">
          <el-card shadow="never">
            <template #header>
              <span>我的学习任务</span>
              <el-button text type="primary" size="small" style="float:right" @click="$router.push('/courses')">去学习</el-button>
            </template>
            <el-table :data="tasks" size="small" max-height="320">
              <el-table-column prop="skillName" label="技能" width="110" />
              <el-table-column prop="courseName" label="课程" />
              <el-table-column prop="courseLevel" label="难度" width="70" align="center" />
              <el-table-column label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ statusText(row.status) }}</el-tag>
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

        <el-col :xs="24" :md="10">
          <el-card shadow="never">
            <template #header>可参加的考试</template>
            <el-table :data="availableExams" size="small" max-height="320">
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

    <!-- ============ 管理员 / 培训负责人：快捷入口 ============ -->
    <template v-else>
      <div class="admin-hero contour-bg">
        <div class="hero-title">培训态势工作台</div>
        <div class="hero-sub">从岗位需求出发，管理技能体系、课程、题库与考核，数据驱动员工技能达标</div>
        <div class="hero-tags">
          <span class="ht"><b>{{ ov.userCount ?? '—' }}</b> 员工</span>
          <span class="ht"><b>{{ ov.courseCount ?? '—' }}</b> 课程</span>
          <span class="ht"><b>{{ ov.examCount ?? '—' }}</b> 在考考试</span>
          <span class="ht"><b>{{ ov.weakSkillCount ?? '—' }}</b> 薄弱项</span>
        </div>
      </div>

      <div class="quick-grid">
        <div v-for="q in quickLinks" :key="q.path" class="quick-item" @click="$router.push(q.path)">
          <div class="qi-icon"><el-icon :size="24"><component :is="q.icon" /></el-icon></div>
          <div class="qi-title">{{ q.title }}</div>
          <div class="qi-sub">{{ q.sub }}</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { taskMy, examPage, attemptMy, skillProfileMine, attemptStart, statsOverview } from '@/api'

const store = useUserStore()
const router = useRouter()
const tasks = ref([])
const exams = ref([])
const attempts = ref([])
const profile = ref({})
const ov = ref({})

const quickLinks = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine', sub: '培训全局态势' },
  { path: '/jobs', title: '岗位技能', icon: 'Briefcase', sub: '技能体系设计' },
  { path: '/courses', title: '课程管理', icon: 'Reading', sub: '课程与章节' },
  { path: '/questions', title: '题库管理', icon: 'Document', sub: '题目与技能绑定' },
  { path: '/exams', title: '考试管理', icon: 'EditPen', sub: '组卷与发布' },
  { path: '/stats', title: '统计分析', icon: 'TrendCharts', sub: '部门·薄弱·排名' },
  { path: '/knowledge', title: '知识库', icon: 'FolderOpened', sub: 'RAG 问答语料' },
  { path: '/ai', title: 'AI 助手', icon: 'MagicStick', sub: '问答·出题·建议' }
]

/* —— 达标靶环数据 —— */
const skills = computed(() => profile.value.skills || [])
const overallRate = computed(() => {
  if (!skills.value.length) return '—'
  const total = skills.value.reduce((a, s) => a + (s.rate ?? 0), 0)
  return Math.round(total / skills.value.length)
})
const reachedCount = computed(() => skills.value.filter(s => s.reached).length)
const weakSkills = computed(() => skills.value.filter(s => s.weak))
const weakCount = computed(() => profile.value.weakCount ?? weakSkills.value.length)
const midCount = computed(() => Math.max(0, skills.value.length - reachedCount.value - weakSkills.value.length))

const C = 2 * Math.PI * 120
const arcDash = computed(() => {
  const r = skills.value.length ? (overallRate.value === '—' ? 0 : overallRate.value) : 0
  return C * r / 100
})
const skillMarks = computed(() => {
  const n = skills.value.length
  return skills.value.map((s, i) => {
    const a = (i * 360 / n - 90) * Math.PI / 180
    return {
      x: 160 + 120 * Math.cos(a),
      y: 160 + 120 * Math.sin(a),
      name: s.skillName,
      rate: s.rate,
      color: s.weak ? '#C2563B' : s.reached ? '#1C6B4F' : '#E5A13C'
    }
  })
})

const statChips = computed(() => [
  { label: '学习任务', value: taskStats.value.total, tone: '' },
  { label: '已完成任务', value: taskStats.value.completed, tone: 'ok' },
  { label: '薄弱技能', value: weakCount.value, tone: 'warn' },
  { label: '通过考试', value: passedExamCount.value, tone: '' }
])

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
function goAdvice(w) {
  ElMessage.info(`已带上下文跳转 AI：请教「${w.skillName}」如何达标`)
  router.push('/ai')
}
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
  } else {
    try { ov.value = await statsOverview() } catch (e) { /* 弱提示 */ }
  }
})
</script>

<style scoped>
.mt16 { margin-top: 16px; }

/* —— 达标靶环 —— */
.ring-wrap { display: flex; flex-direction: column; align-items: center; padding-top: 4px; }
.ring-svg { width: 100%; max-width: 300px; height: auto; }
.r { fill: none; stroke: #DCE7E0; stroke-width: 1; }
.c1 { opacity: .9 } .c2 { opacity: .7 } .c3 { opacity: .55 } .c4 { opacity: .45 } .c5 { opacity: .35 } .c6 { opacity: .28 }
.ring-base {
  fill: none;
  stroke: var(--amber);
  stroke-width: 1.6;
  stroke-dasharray: 3 7;
  opacity: .85;
}
.ring-base-label { font-size: 10px; fill: #B5832E; letter-spacing: .1em; }
.ring-progress {
  fill: none;
  stroke: #1C6B4F;
  stroke-width: 5;
  stroke-linecap: round;
  transform: rotate(-90deg);
  transform-origin: 160px 160px;
  transition: stroke-dasharray .8s cubic-bezier(.22,.61,.36,1);
}
.skill-dot { transition: r .2s; }

.ring-num {
  font-family: var(--serif);
  font-size: 46px;
  font-weight: 700;
  fill: var(--ink-text);
}
.ring-num-label { font-size: 11px; fill: var(--muted); letter-spacing: .18em; }

.legend { display: flex; gap: 16px; margin-top: 14px; flex-wrap: wrap; justify-content: center; }
.lg { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--muted); }
.lg i { width: 9px; height: 9px; border-radius: 50%; display: inline-block; }

/* —— 成长简报 —— */
.stat-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat {
  background: var(--paper);
  border: 1px solid var(--hairline);
  border-radius: 12px;
  padding: 14px 16px;
}
.stat-num { font-size: 30px; color: var(--ink-text); line-height: 1.1; }
.stat-num.ok { color: var(--pine); }
.stat-num.warn { color: var(--clay); }
.stat-label { font-size: 12px; color: var(--muted); margin-top: 4px; }

.brief-block { margin-top: 18px; }
.brief-title {
  font-family: var(--serif);
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-text);
  margin-bottom: 10px;
}
.weak-chips { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
.weak-chip {
  display: inline-flex; align-items: center; gap: 4px;
  background: #FDF3E7;
  color: #A05E22;
  border: 1px solid #F0D9B8;
  border-radius: 20px;
  padding: 5px 12px;
  font-size: 13px;
  cursor: pointer;
  transition: background-color .15s, border-color .15s;
}
.weak-chip:hover { background: #FBE7C9; border-color: #E5A13C; }
.weak-none { font-size: 13px; color: var(--muted); }
.advice-btn { letter-spacing: .08em; }

/* —— 管理员 —— */
.admin-hero {
  position: relative;
  background-color: var(--ink);
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='700' height='300' viewBox='0 0 700 300'><g fill='none' stroke='%231C6B4F' stroke-width='1' opacity='0.5'><ellipse cx='560' cy='150' rx='420' ry='220'/><ellipse cx='560' cy='150' rx='330' ry='165'/><ellipse cx='560' cy='150' rx='245' ry='118'/><ellipse cx='560' cy='150' rx='170' ry='78'/><ellipse cx='560' cy='150' rx='104' ry='45'/><ellipse cx='560' cy='150' rx='48' ry='20'/><ellipse cx='560' cy='150' rx='16' ry='7'/></g></svg>");
  background-size: cover;
  background-position: right center;
  border-radius: 16px;
  padding: 30px 34px;
  color: #EAF3EE;
  overflow: hidden;
}
.hero-title {
  font-family: var(--serif);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: .06em;
  position: relative; z-index: 1;
}
.hero-sub { font-size: 13px; color: #8FA99B; margin-top: 8px; position: relative; z-index: 1; }
.hero-tags { display: flex; gap: 26px; margin-top: 22px; position: relative; z-index: 1; }
.ht { font-size: 13px; color: #B9CEC2; }
.ht b { font-family: var(--serif); font-size: 24px; color: var(--amber); margin-right: 5px; }

.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-top: 16px; }
.quick-item {
  background: #FFFFFF;
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 20px 18px;
  cursor: pointer;
  transition: transform .16s, box-shadow .16s, border-color .16s;
}
.quick-item:hover {
  transform: translateY(-3px);
  border-color: #A9C9BA;
  box-shadow: 0 10px 24px rgba(14,36,28,.10);
}
.qi-icon {
  width: 46px; height: 46px; border-radius: 12px;
  background: var(--sage);
  color: var(--pine);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 12px;
}
.quick-item:hover .qi-icon { background: var(--pine); color: #fff; }
.qi-title { font-size: 15px; font-weight: 600; color: var(--ink-text); }
.qi-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }

@media (max-width: 1100px) { .quick-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 720px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
