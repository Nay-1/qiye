<template>
  <div class="my-courses" v-loading="loading">
    <div class="page-intro">
      <div class="intro-left">
        <h2 class="intro-title">我的课程</h2>
        <p class="intro-sub">从岗位培训任务出发，逐章学习，让每项技能抵达达标线</p>
      </div>
      <div class="intro-right">
        <span class="stat-pill">{{ total }} 门课程</span>
      </div>
    </div>

    <el-empty v-if="!loading && !list.length" description="暂无课程" />

    <div v-else class="course-grid">
      <div v-for="c in list" :key="c.id" class="course-card" @click="goLearn(c)">
        <div class="cover">
          <span class="cover-char">{{ (c.name || '课').charAt(0) }}</span>
          <span class="cover-cat">{{ c.category || '通用' }}</span>
        </div>
        <div class="card-body">
          <h3 class="card-title">{{ c.name }}</h3>
          <div class="meta">
            <el-tag size="small" effect="plain" :class="levelTagClass(c.level)">{{ c.level }}</el-tag>
            <el-tag size="small" effect="plain" type="info">{{ c.jobName || '通用岗位' }}</el-tag>
          </div>
          <div class="progress-wrap">
            <el-progress :percentage="rateOf(c)" :stroke-width="6" :show-text="false"
                         :status="rateOf(c) === 100 ? 'success' : undefined" />
            <div class="progress-text">
              <span class="serif-num rate-num">{{ rateOf(c) }}%</span>
              <span class="rate-detail">{{ doneOf(c) }}/{{ c.chapterCount || 0 }} 章已完成</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="total > size" class="pager">
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="size"
                     v-model:current-page="page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { coursePage, studyMy } from '@/api'

const router = useRouter()
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(false)
const myStudy = ref([])   // 我的全部学习记录

async function load() {
  loading.value = true
  try {
    const [p, study] = await Promise.all([coursePage({ page: page.value, size }), studyMy()])
    total.value = p.total || 0
    list.value = p.records || []
    myStudy.value = study || []
  } finally {
    loading.value = false
  }
}

function doneOf(c) {
  return myStudy.value.filter(s => s.courseId === c.id && s.status === 'COMPLETED').length
}

function rateOf(c) {
  const totalCh = c.chapterCount || 0
  if (!totalCh) return 0
  return Math.round(doneOf(c) / totalCh * 100)
}

function levelTagClass(level) {
  return { 初级: 'lv-easy', 中级: 'lv-mid', 高级: 'lv-hard' }[level] || ''
}

function goLearn(c) {
  router.push('/learn/' + c.id)
}

onMounted(load)
</script>

<style scoped>
.page-intro {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 18px;
}
.intro-title {
  margin: 0 0 6px;
  font-family: var(--serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--ink-text);
  letter-spacing: 0.02em;
}
.intro-sub { margin: 0; color: var(--muted); font-size: 13px; }
.stat-pill {
  flex: none;
  font-size: 13px;
  color: var(--pine);
  background: var(--sage);
  border: 1px solid #D4E4DA;
  border-radius: 20px;
  padding: 5px 14px;
  font-family: var(--serif);
  font-weight: 600;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 16px;
}
.course-card {
  background: #fff;
  border: 1px solid var(--hairline);
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .16s, box-shadow .16s, border-color .16s;
}
.course-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(14, 36, 28, 0.10);
  border-color: #C6D8CD;
}

/* 封面色块：墨绿渐变 + 等高线暗纹 */
.cover {
  height: 96px;
  background: linear-gradient(135deg, var(--pine) 0%, var(--forest) 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover::before {
  content: '';
  position: absolute; inset: 0;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='300' height='150' viewBox='0 0 300 150'><g fill='none' stroke='%23FFFFFF' stroke-width='1' opacity='0.28'><ellipse cx='240' cy='30' rx='150' ry='100'/><ellipse cx='240' cy='30' rx='112' ry='74'/><ellipse cx='240' cy='30' rx='78' ry='50'/><ellipse cx='240' cy='30' rx='48' ry='30'/><ellipse cx='240' cy='30' rx='24' ry='14'/></g></svg>");
  background-size: cover;
  opacity: .9;
}
.cover-char {
  position: relative; z-index: 1;
  font-family: var(--serif);
  font-size: 40px;
  font-weight: 700;
  color: #F2F7F4;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.18);
}
.cover-cat {
  position: absolute; top: 8px; right: 10px; z-index: 1;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(14, 36, 28, 0.35);
  border-radius: 20px;
  padding: 2px 8px;
}

.card-body { padding: 14px 16px 16px; }
.card-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta { display: flex; gap: 6px; margin-bottom: 12px; }
.lv-easy { color: var(--muted); }
.lv-mid { color: var(--amber-deep); }
.lv-hard { color: var(--teal); }

.progress-wrap { }
.progress-text {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-top: 6px;
  font-size: 12px;
  color: var(--muted);
}
.rate-num { font-size: 15px; color: var(--pine); }

.pager { display: flex; justify-content: center; margin-top: 20px; }
</style>
