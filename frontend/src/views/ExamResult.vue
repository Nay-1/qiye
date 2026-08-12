<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head" v-if="detail">
        <b>{{ detail.examTitle }}</b>
        <div>
          <el-tag :type="detail.passed ? 'success' : 'danger'" size="large">{{ detail.passed ? '已通过' : '未通过' }}</el-tag>
          <span class="score">总分：{{ detail.totalScore }} / 及格线 {{ detail.passScore }}</span>
        </div>
      </div>
    </template>

    <template v-if="detail">
      <div v-for="(a, i) in detail.answers" :key="i" class="answer-item">
        <div class="a-head">
          <b>{{ i + 1 }}.</b>
          <el-tag size="small" :type="a.question.type === 'SINGLE' ? 'primary' : a.question.type === 'MULTIPLE' ? 'warning' : 'info'" style="margin:0 8px">
            {{ { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[a.question.type] }}
          </el-tag>
          <span>{{ a.question.content }}</span>
          <span style="margin-left:auto">
            <el-tag :type="a.correct ? 'success' : 'danger'" size="small">{{ a.correct ? `正确 +${a.score}` : '错误' }}</el-tag>
          </span>
        </div>

        <div v-if="a.question.type !== 'JUDGE'" class="opts">
          <div v-for="o in a.question.options" :key="o.key" :class="['opt', optClass(o.key, a)]">
            {{ o.key }}. {{ o.text }}
          </div>
        </div>

        <div class="meta">
          <div>我的答案：<b>{{ a.userAnswer || '（未作答）' }}</b></div>
          <div>正确答案：<b style="color:#059669">{{ a.question.answer }}</b></div>
          <div v-if="a.question.analysis" style="color:#6b7280">解析：{{ a.question.analysis }}</div>
        </div>
      </div>
    </template>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { attemptDetail } from '@/api'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)

function optClass(key, a) {
  const correct = (a.question.answer || '').split(',').includes(key)
  const mine = (a.userAnswer || '').split(',').includes(key)
  if (correct && mine) return 'opt-right'
  if (correct) return 'opt-correct'
  if (mine) return 'opt-wrong'
  return ''
}

onMounted(async () => {
  loading.value = true
  detail.value = await attemptDetail(route.params.attemptId)
  loading.value = false
})
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.score { margin-left: 16px; font-weight: 600; color: #374151; }
.answer-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
.a-head { display: flex; align-items: center; }
.opts { padding-left: 24px; margin-top: 8px; display: flex; flex-direction: column; gap: 4px; }
.opt { padding: 4px 8px; border-radius: 4px; }
.opt-right { background: #d1fae5; }
.opt-correct { color: #059669; font-weight: 600; }
.opt-wrong { background: #fee2e2; color: #dc2626; }
.meta { margin-top: 10px; padding-left: 24px; color: #374151; display: flex; flex-direction: column; gap: 4px; font-size: 14px; }
</style>
