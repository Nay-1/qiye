<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head" v-if="paper">
        <b>{{ paper.title }}</b>
        <span>剩余时间：<el-tag :type="remain <= 60 ? 'danger' : 'primary'">{{ remainText }}</el-tag></span>
      </div>
    </template>

    <template v-if="paper">
      <div v-for="(q, i) in paper.questions" :key="q.questionId" class="question">
        <div class="q-head">
          <b>{{ i + 1 }}.</b>
          <el-tag size="small" :type="q.type === 'SINGLE' ? 'primary' : q.type === 'MULTIPLE' ? 'warning' : 'info'" style="margin:0 8px">
            {{ { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[q.type] }}
          </el-tag>
          <span>{{ q.content }}</span>
          <span style="margin-left:auto;color:#6b7280">({{ q.score }}分)</span>
        </div>

        <div v-if="q.type === 'JUDGE'" class="q-body">
          <el-radio-group v-model="answers[q.questionId]">
            <el-radio value="TRUE">正确</el-radio>
            <el-radio value="FALSE">错误</el-radio>
          </el-radio-group>
        </div>
        <div v-else-if="q.type === 'MULTIPLE'" class="q-body">
          <el-checkbox-group v-model="multi[q.questionId]">
            <el-checkbox v-for="o in q.options" :key="o.key" :label="o.key" :value="o.key">{{ o.key }}. {{ o.text }}</el-checkbox>
          </el-checkbox-group>
        </div>
        <div v-else class="q-body">
          <el-radio-group v-model="answers[q.questionId]">
            <el-radio v-for="o in q.options" :key="o.key" :label="o.key" :value="o.key">{{ o.key }}. {{ o.text }}</el-radio>
          </el-radio-group>
        </div>
      </div>

      <el-button type="primary" size="large" style="width:100%;margin-top:16px" @click="submit">提交答卷</el-button>
    </template>
  </el-card>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { attemptSubmit } from '@/api'

const route = useRoute()
const router = useRouter()
const paper = ref(null)
const answers = reactive({})
const multi = reactive({})
const loading = ref(false)
const remain = ref(0)
let timer = null

const remainText = computed(() => {
  const m = Math.floor(remain.value / 60), s = remain.value % 60
  return `${m}:${String(s).padStart(2, '0')}`
})

function startTimer() {
  remain.value = (paper.value.duration || 60) * 60
  timer = setInterval(() => {
    remain.value--
    if (remain.value <= 0) {
      clearInterval(timer)
      ElMessageBox.alert('考试时间到，系统自动提交', '提示')
      submit()
    }
  }, 1000)
}

function buildAnswerList() {
  const list = []
  for (const q of paper.value.questions) {
    let ans
    if (q.type === 'MULTIPLE') ans = (multi[q.questionId] || []).sort().join(',')
    else ans = answers[q.questionId] || ''
    list.push({ questionId: q.questionId, userAnswer: ans })
  }
  return list
}

async function submit() {
  clearInterval(timer)
  try {
    const res = await attemptSubmit({ attemptId: paper.value.attemptId, answers: buildAnswerList() })
    await ElMessageBox.alert(`本次得分：${res.totalScore} 分${res.passed ? '（通过）' : '（未通过）'}`, '提交成功')
    router.replace(`/exam-result/${paper.value.attemptId}`)
  } catch (e) { /* 已提示 */ }
}

onMounted(() => {
  const p = sessionStorage.getItem('examPaper')
  if (p) {
    paper.value = JSON.parse(p)
    paper.value.questions.forEach(q => { if (q.type === 'MULTIPLE') multi[q.questionId] = [] })
    startTimer()
  } else {
    ElMessage.warning('试卷数据缺失，请重新进入考试')
    router.replace('/home')
  }
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.question { border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
.q-head { display: flex; align-items: center; }
.q-body { margin-top: 12px; padding-left: 20px; display: flex; flex-direction: column; gap: 6px; }
</style>
