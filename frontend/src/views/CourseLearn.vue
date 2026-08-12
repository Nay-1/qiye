<template>
  <el-card shadow="never" v-loading="loading">
    <template v-if="course">
      <div class="course-head">
        <h2>{{ course.name }}</h2>
        <div class="tags">
          <el-tag size="small">{{ course.category }}</el-tag>
          <el-tag size="small" type="warning">{{ course.level }}</el-tag>
          <el-tag size="small" type="info">{{ course.jobName || '通用' }}</el-tag>
          <el-tag v-for="s in course.skills || []" :key="s.skillId" size="small" type="success" effect="plain">
            培养技能：{{ s.skillName }} {{ s.required ? '（必修）' : '' }}
          </el-tag>
        </div>
        <p class="desc">{{ course.description }}</p>
      </div>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-card shadow="never" header="章节">
            <div class="chapter-list">
              <div v-for="ch in course.chapters || []" :key="ch.id"
                   :class="['chapter-item', { active: current?.id === ch.id }]"
                   @click="openChapter(ch)">
                <div class="chapter-title">第 {{ ch.seq }} 章 {{ ch.title }}</div>
                <el-tag v-if="ch.studyRecord?.status === 'COMPLETED'" type="success" size="small">已完成</el-tag>
                <el-tag v-else-if="ch.studyRecord?.status === 'IN_PROGRESS'" type="warning" size="small">学习中</el-tag>
                <el-tag v-else size="small" type="info">未学</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="16">
          <el-card shadow="never">
            <template #header>
              <div class="content-head">
                <b>{{ current ? `第 ${current.seq} 章 ${current.title}` : '选择章节开始学习' }}</b>
                <div>
                  <el-progress v-if="courseProgress" type="circle" :width="50" :percentage="courseProgress" :stroke-width="5" />
                  <el-button v-if="current" type="primary" :disabled="current.studyRecord?.status === 'COMPLETED'" @click="markDone">标记本章完成</el-button>
                </div>
              </div>
            </template>
            <div class="chapter-content">
              <p v-if="current" style="white-space: pre-wrap">{{ current.content || '（本章暂无内容）' }}</p>
              <el-empty v-else description="请选择左侧章节开始学习" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { courseDetail, studyStart, studyProgress, studyMyCourse } from '@/api'

const route = useRoute()
const course = ref(null)
const current = ref(null)
const loading = ref(false)

const courseProgress = computed(() => {
  const chapters = course.value?.chapters || []
  if (!chapters.length) return 0
  const done = chapters.filter(c => c.studyRecord?.status === 'COMPLETED').length
  return Math.round(done / chapters.length * 100)
})

async function load() {
  loading.value = true
  course.value = await courseDetail(route.params.id)
  loading.value = false
}

async function openChapter(ch) {
  current.value = ch
  await studyStart({ courseId: course.value.id, chapterId: ch.id })
  if (!ch.studyRecord) {
    ch.studyRecord = { status: 'IN_PROGRESS', progress: 0 }
  }
  ch.studyRecord.status = 'IN_PROGRESS'
}

async function markDone() {
  const rec = await studyProgress({ chapterId: current.value.id, progress: 100, duration: 0 })
  current.value.studyRecord = rec
  ElMessage.success('本章学习完成！')
  load()
}

onMounted(load)
</script>

<style scoped>
.course-head { margin-bottom: 16px; }
.course-head h2 { margin: 0 0 8px; }
.tags { display: flex; gap: 8px; flex-wrap: wrap; }
.desc { color: #6b7280; margin-top: 10px; }
.chapter-list { max-height: 480px; overflow: auto; }
.chapter-item { display: flex; justify-content: space-between; align-items: center; padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: all .2s; }
.chapter-item:hover { border-color: #3b82f6; }
.chapter-item.active { border-color: #3b82f6; background: #eff6ff; }
.chapter-title { font-size: 14px; }
.content-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.chapter-content { min-height: 400px; line-height: 1.9; color: #374151; }
</style>
