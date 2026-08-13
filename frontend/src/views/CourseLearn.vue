<template>
  <div class="learn-wrap" v-loading="loading">
    <template v-if="course">
      <!-- 课程头 -->
      <div class="course-head">
        <div class="head-left">
          <h2 class="course-name">{{ course.name }}</h2>
          <div class="tags">
            <el-tag size="small" effect="plain">{{ course.category || '通用' }}</el-tag>
            <el-tag size="small" effect="plain" class="tag-level">{{ course.level }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ course.jobName || '通用岗位' }}</el-tag>
            <el-tag v-for="s in course.skills || []" :key="s.skillId" size="small" effect="plain"
                    class="tag-skill">培养技能：{{ s.skillName }}{{ s.required ? '（必修）' : '' }}</el-tag>
          </div>
          <p class="desc">{{ course.description }}</p>
        </div>
        <div class="head-right">
          <el-progress type="circle" :percentage="courseProgress" :width="78" :stroke-width="7">
            <template #default>
              <span class="serif-num course-rate">{{ courseProgress }}%</span>
            </template>
          </el-progress>
          <div class="head-right-text">
            <div class="head-rate-label">课程进度</div>
            <div class="head-rate-sub">{{ doneCount }}/{{ chapters.length }} 章已完成</div>
          </div>
        </div>
      </div>

      <!-- 学习主体 -->
      <div class="learn-body">
        <aside class="catalog" :class="{ folded: catalogFolded }">
          <div class="catalog-head" @click="catalogFolded = !catalogFolded">
            <el-icon :size="14" class="fold-icon">
              <ArrowDown v-if="!catalogFolded" />
              <ArrowRight v-else />
            </el-icon>
            <span class="catalog-title-text">章节目录</span>
            <span class="catalog-count">{{ doneCount }}/{{ chapters.length }}</span>
          </div>
          <div v-show="!catalogFolded" class="catalog-list">
            <div v-for="(ch, idx) in chapters" :key="ch.id"
                 :class="['catalog-item', { active: current?.id === ch.id, done: ch.studyRecord?.status === 'COMPLETED' }]"
                 @click="openChapter(ch, idx)">
              <span class="state-dot" />
              <span class="catalog-seq">第{{ ch.seq }}章</span>
              <span class="catalog-title">{{ ch.title }}</span>
              <span v-if="ch.studyRecord?.status === 'COMPLETED'" class="catalog-check">✓</span>
            </div>
          </div>
        </aside>

        <section class="reader">
          <template v-if="current">
            <div class="reader-head">
              <div class="reader-head-left">
                <h3 class="reader-chapter-title">第 {{ current.seq }} 章 {{ current.title }}</h3>
                <div class="reader-meta">
                  <span>本章进度 <b class="serif-num">{{ currentProgress }}%</b></span>
                  <span class="dot-sep">·</span>
                  <span>本次学习 {{ fmt(sessionSeconds) }}</span>
                  <span class="dot-sep">·</span>
                  <span>累计 {{ fmt(totalStudySeconds) }}</span>
                </div>
              </div>
              <el-progress class="reader-bar" :percentage="currentProgress" :stroke-width="6" :show-text="false" />
            </div>

            <article ref="contentEl" class="reader-content" @scroll="onContentScroll">
              <p v-if="current.content" class="content-text">{{ current.content }}</p>
              <el-empty v-else description="本章暂无内容" :image-size="80" />
            </article>

            <div class="reader-foot">
              <el-button :disabled="!prevChapter" @click="goPrev">
                <el-icon :size="14"><ArrowLeft /></el-icon>
                <span>上一节</span>
              </el-button>
              <el-button type="primary" :class="{ 'can-finish': canFinish }" :disabled="isDone" @click="markDone">
                {{ isDone ? '已完成' : (atBottom ? '已读到底部 · 完成本章' : '完成本章') }}
              </el-button>
              <el-button :disabled="!nextChapter" @click="goNext">
                <span>下一节</span>
                <el-icon :size="14"><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <el-empty v-else description="课程暂无章节" />
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseDetail, studyStart, studyProgress } from '@/api'

const route = useRoute()
const course = ref(null)
const current = ref(null)
const currentIdx = ref(-1)
const catalogFolded = ref(false)
const loading = ref(false)

const sessionSeconds = ref(0)     // 当前章节本次会话阅读秒数
const reportedSeconds = ref(0)    // 已上报过的 duration 秒数（防重复累计）
const atBottom = ref(false)       // 是否已滚动到底部
let timer = null

const REPORT_EVERY = 15   // 每满 15s 上报一次
const AUTO_STEP = 3       // 每阅读 3s 进度 +1（上限 90，不自动打满）

const chapters = computed(() => course.value?.chapters || [])
const doneCount = computed(() => chapters.value.filter(c => c.studyRecord?.status === 'COMPLETED').length)
const courseProgress = computed(() => chapters.value.length
  ? Math.round(doneCount.value / chapters.value.length * 100)
  : 0)

const currentRec = computed(() => current.value?.studyRecord)
const isDone = computed(() => currentRec.value?.status === 'COMPLETED')

// 本章进度：已完成固定 100；否则取「后端已有进度」与「本地自动涨进度」较大值
const currentProgress = computed(() => {
  if (isDone.value) return 100
  const base = currentRec.value?.progress || 0
  const auto = Math.min(90, Math.floor(sessionSeconds.value / AUTO_STEP))
  return Math.max(base, auto)
})

// 全课程累计学习时长（所有章节 studyDuration 之和）
const totalStudySeconds = computed(() =>
  chapters.value.reduce((sum, c) => sum + (c.studyRecord?.studyDuration || 0), 0)
)

const prevChapter = computed(() => currentIdx.value > 0 ? chapters.value[currentIdx.value - 1] : null)
const nextChapter = computed(() => currentIdx.value < chapters.value.length - 1 ? chapters.value[currentIdx.value + 1] : null)
const canFinish = computed(() => !isDone.value && (atBottom.value || currentProgress.value >= 90))

function fmt(s) {
  s = Math.max(0, Math.floor(s || 0))
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

async function load() {
  loading.value = true
  try {
    course.value = await courseDetail(route.params.id)
    const chs = chapters.value
    // 学习通式「接着学」：自动打开第一个未完成章节
    const firstPending = chs.find(c => !c.studyRecord || c.studyRecord.status !== 'COMPLETED')
    const target = firstPending || chs[0]
    if (target) {
      await openChapter(target, chs.indexOf(target))
    }
  } finally {
    loading.value = false
  }
}

async function openChapter(ch, idx) {
  if (current.value?.id === ch.id) return
  await flushProgress()          // 先上报当前章
  current.value = ch
  currentIdx.value = idx
  sessionSeconds.value = 0
  reportedSeconds.value = 0
  atBottom.value = false
  try {
    const rec = await studyStart({ courseId: course.value.id, chapterId: ch.id })
    if (rec) ch.studyRecord = rec
    if (!ch.studyRecord) ch.studyRecord = { status: 'IN_PROGRESS', progress: 0 }
    if (ch.studyRecord.status !== 'COMPLETED') ch.studyRecord.status = 'IN_PROGRESS'
  } catch (e) { /* 静默 */ }
  startTimer()
}

function startTimer() {
  stopTimer()
  timer = setInterval(() => {
    sessionSeconds.value++
    if (sessionSeconds.value - reportedSeconds.value >= REPORT_EVERY) flushProgress()
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

async function flushProgress() {
  if (!current.value) return
  const delta = sessionSeconds.value - reportedSeconds.value
  if (delta <= 0) return
  reportedSeconds.value = sessionSeconds.value
  try {
    const rec = await studyProgress({ chapterId: current.value.id, progress: currentProgress.value, duration: delta })
    if (rec) current.value.studyRecord = rec
  } catch (e) { /* 静默：离场上报失败不阻塞 */ }
}

function onContentScroll() {
  const el = contentEl.value
  if (!el) return
  atBottom.value = el.scrollTop + el.clientHeight >= el.scrollHeight - 8
}

async function markDone() {
  const delta = sessionSeconds.value - reportedSeconds.value
  try {
    const rec = await studyProgress({ chapterId: current.value.id, progress: 100, duration: Math.max(delta, 0) })
    if (rec) current.value.studyRecord = rec
    reportedSeconds.value = sessionSeconds.value
    ElMessage.success('本章学习完成')
    if (courseProgress.value === 100) {
      ElMessage.success('恭喜！本课程已全部学完')
    }
  } catch (e) {
    ElMessage.error('上报失败，请重试')
  }
}

function goPrev() {
  if (prevChapter.value) openChapter(prevChapter.value, currentIdx.value - 1)
}

function goNext() {
  if (nextChapter.value) openChapter(nextChapter.value, currentIdx.value + 1)
}

onMounted(load)
onBeforeUnmount(() => { stopTimer(); flushProgress() })
onBeforeRouteLeave(() => { stopTimer(); flushProgress() })
</script>

<style scoped>
.learn-wrap { max-width: 1180px; margin: 0 auto; }

/* —— 课程头 —— */
.course-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  background: #fff;
  border: 1px solid var(--hairline);
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(14, 36, 28, 0.04), 0 4px 14px rgba(14, 36, 28, 0.05);
}
.course-name {
  margin: 0 0 8px;
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-text);
  letter-spacing: 0.02em;
}
.tags { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.tag-level { color: var(--amber-deep); }
.tag-skill { color: var(--pine); background: var(--sage); border-color: #D4E4DA; }
.desc { color: var(--muted); margin: 10px 0 0; line-height: 1.7; }

.head-right { display: flex; align-items: center; gap: 14px; flex: none; }
.course-rate { font-size: 17px; }
.head-rate-label { font-family: var(--serif); font-weight: 600; color: var(--ink-text); font-size: 15px; }
.head-rate-sub { font-size: 12px; color: var(--muted); margin-top: 4px; }

/* —— 学习主体 —— */
.learn-body {
  display: flex;
  gap: 16px;
  align-items: stretch;
}

/* 左侧章节目录 */
.catalog {
  flex: none;
  width: 264px;
  background: #fff;
  border: 1px solid var(--hairline);
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.catalog.folded .catalog-list { display: none; }
.catalog-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--hairline);
  user-select: none;
  background: #FBFCFB;
}
.catalog-title-text {
  font-family: var(--serif);
  font-weight: 600;
  color: var(--ink-text);
  flex: 1;
}
.fold-icon { color: var(--muted); }
.catalog-count {
  font-size: 12px;
  color: var(--pine);
  background: var(--sage);
  border-radius: 20px;
  padding: 2px 10px;
  font-family: var(--serif);
  font-weight: 600;
}
.catalog-list { flex: 1; overflow-y: auto; padding: 8px; max-height: 560px; }
.catalog-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px;
  border-radius: 9px;
  cursor: pointer;
  margin-bottom: 2px;
  transition: background-color .15s, color .15s;
}
.catalog-item:hover { background: var(--paper); }
.catalog-item.active { background: var(--sage); }
.catalog-item.active .catalog-title { color: var(--pine); font-weight: 600; }

.state-dot {
  width: 8px; height: 8px; border-radius: 50%;
  flex: none;
  background: #C7D6CC;                    /* 未学：灰 */
  border: 1px solid #A9BEB0;
}
.catalog-item:not(.done):not(.active) .state-dot { background: transparent; }
.catalog-item:not(.done).active .state-dot { background: var(--amber); border-color: var(--amber); }
.catalog-item.done .state-dot { background: var(--pine); border-color: var(--pine); }

.catalog-seq {
  font-size: 11px;
  color: var(--muted);
  flex: none;
  font-variant-numeric: tabular-nums;
}
.catalog-title {
  flex: 1;
  font-size: 13px;
  color: var(--ink-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.catalog-check { color: var(--pine); font-weight: 700; flex: none; }

/* 右侧阅读区 */
.reader {
  flex: 1;
  min-width: 0;
  background: #fff;
  border: 1px solid var(--hairline);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.reader-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hairline);
  background: #FBFCFB;
}
.reader-chapter-title {
  margin: 0;
  font-family: var(--serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--ink-text);
}
.reader-meta { font-size: 12px; color: var(--muted); margin-top: 6px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.reader-meta b { color: var(--pine); }
.dot-sep { color: #CBD5CE; }
.reader-bar { width: 180px; flex: none; }

.reader-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px 28px;
  min-height: 420px;
  line-height: 2.05;
}
.content-text {
  margin: 0;
  font-size: 15px;
  color: var(--ink-text);
  white-space: pre-wrap;
  word-break: break-word;
}

/* 底部导航 */
.reader-foot {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-top: 1px solid var(--hairline);
  background: #FBFCFB;
}
.reader-foot .el-button + .el-button { margin-left: 0; }
.reader-foot .el-button { min-width: 120px; }
.reader-foot .el-button.can-finish {
  background: var(--amber);
  border-color: var(--amber);
  color: #fff;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(229, 161, 60, 0.35);
}
.reader-foot .el-button.can-finish:hover {
  background: var(--amber-deep);
  border-color: var(--amber-deep);
}
</style>
