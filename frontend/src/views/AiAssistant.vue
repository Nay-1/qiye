<template>
  <el-card shadow="never">
    <el-tabs v-model="tab">
      <!-- AI 问答 -->
      <el-tab-pane label="AI 培训问答（RAG）" name="chat">
        <div class="chat-box">
          <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
            <div class="bubble">
              <div class="msg-role">{{ m.role === 'user' ? '我' : 'AI 助手' }}</div>
              <div class="msg-content" style="white-space:pre-wrap">{{ m.content }}</div>
              <div v-if="m.sources?.length" class="msg-src">
                来源：<el-tag v-for="s in m.sources" :key="s" size="small" style="margin-right:6px">{{ s }}</el-tag>
              </div>
            </div>
          </div>
          <el-empty v-if="!messages.length" description="向我提问企业培训相关问题，回答基于知识库" />
        </div>
        <div class="chat-input">
          <el-input v-model="question" placeholder="输入问题，如：缓存雪崩怎么解决" @keyup.enter="sendChat" />
          <el-button type="primary" :loading="chatLoading" @click="sendChat">发送</el-button>
        </div>
      </el-tab-pane>

      <!-- 学习建议 -->
      <el-tab-pane label="AI 学习建议" name="advice">
        <el-button type="primary" :loading="adviceLoading" @click="loadAdvice" style="margin-bottom:16px">生成我的学习建议</el-button>
        <el-empty v-if="!advices.length" description="点击按钮，基于技能画像识别薄弱技能并生成建议" />
        <el-card v-for="(a, i) in advices" :key="i" shadow="hover" style="margin-bottom:12px">
          <template #header>
            <b style="color:#dc2626">{{ a.skillName }}</b>
            <span style="margin-left:12px;color:#6b7280">得分 {{ a.score }} / 目标 {{ a.targetLevel }} / 达成率 {{ a.rate }}%</span>
          </template>
          <div style="margin-bottom:8px">
            推荐课程：
            <el-tag v-for="c in a.courses" :key="c.id" size="small" type="success" style="margin-right:6px">{{ c.name }}</el-tag>
            <el-button v-if="a.courses.length" link type="primary" size="small" @click="$router.push(`/learn/${a.courses[0].id}`)">去学习</el-button>
          </div>
          <div style="line-height:1.8;color:#374151">{{ a.advice }}</div>
        </el-card>
      </el-tab-pane>

      <!-- AI 出题 -->
      <el-tab-pane v-if="store.canManage" label="AI 生成试题" name="gen">
        <div class="toolbar">
          <el-input v-model="genForm.topic" placeholder="主题，如：Spring Boot" style="width:240px" />
          <el-input-number v-model="genForm.count" :min="1" :max="10" />
          <el-select v-model="genForm.type" placeholder="题型" clearable style="width:140px">
            <el-option label="单选题" value="单选题" /><el-option label="多选题" value="多选题" /><el-option label="判断题" value="判断题" />
          </el-select>
          <el-button type="primary" :loading="genLoading" @click="doGenerate">AI 生成</el-button>
        </div>

        <el-empty v-if="!generated.length" description="输入主题，让 AI 生成试题草稿；审核并绑定技能后入库" />

        <div v-for="(q, i) in generated" :key="i" class="gen-item">
          <div class="gen-head">
            <el-tag size="small" :type="q.type === 'SINGLE' ? 'primary' : q.type === 'MULTIPLE' ? 'warning' : 'info'">
              {{ { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[q.type] || q.type }}
            </el-tag>
            <span style="font-weight:600">{{ q.content }}</span>
          </div>
          <div v-if="q.options?.length" class="gen-opts">
            <div v-for="o in q.options" :key="o.key">{{ o.key }}. {{ o.text }}</div>
          </div>
          <div class="gen-answer">答案：{{ q.answer }}<span v-if="q.analysis" style="margin-left:16px;color:#6b7280">解析：{{ q.analysis }}</span></div>
          <div class="gen-actions">
            <el-select v-model="q.skillId" placeholder="绑定考核技能（必选）" size="small" style="width:200px">
              <el-option v-for="s in skills" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-button type="success" size="small" @click="reviewSave(q)">审核入库</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiChat, aiStudyAdvice, aiGenerateQuestions, questionCreate, skillList } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const tab = ref('chat')
const messages = ref([]), question = ref(''), chatLoading = ref(false)
const advices = ref([]), adviceLoading = ref(false)
const generated = ref([]), genLoading = ref(false), skills = ref([])
const genForm = reactive({ topic: '', count: 5, type: '' })

async function sendChat() {
  if (!question.value.trim()) return
  messages.value.push({ role: 'user', content: question.value })
  chatLoading.value = true
  try {
    const res = await aiChat({ question: question.value })
    messages.value.push({ role: 'assistant', content: res.answer, sources: res.sources })
  } finally {
    chatLoading.value = false
    question.value = ''
  }
}

async function loadAdvice() {
  adviceLoading.value = true
  try { advices.value = await aiStudyAdvice() } finally { adviceLoading.value = false }
}

async function doGenerate() {
  if (!genForm.topic) return ElMessage.warning('请输入出题主题')
  genLoading.value = true
  try {
    generated.value = await aiGenerateQuestions({ topic: genForm.topic, count: genForm.count, type: genForm.type || undefined })
    ElMessage.success(`已生成 ${generated.value.length} 道题目草稿，请审核`)
  } finally { genLoading.value = false }
}

async function reviewSave(q) {
  if (!q.skillId) return ElMessage.warning('请为该题绑定考核技能（强制，用于技能画像计算）')
  await questionCreate({
    type: q.type === '多选题' ? 'MULTIPLE' : q.type === '判断题' ? 'JUDGE' : 'SINGLE',
    content: q.content,
    options: q.type === '判断题' ? [] : (q.options || []).map(o => ({ key: o.key, text: o.text })),
    answer: q.type === '判断题' ? (q.answer === '正确' || q.answer === 'TRUE' ? 'TRUE' : 'FALSE') : q.answer,
    analysis: q.analysis || '',
    skillIds: [q.skillId],
    source: 'AI'
  })
  ElMessage.success('题目已入库')
  generated.value = generated.value.filter(x => x !== q)
}

onMounted(() => { if (store.canManage) skillList().then(d => skills.value = d) })
</script>

<style scoped>
.chat-box { min-height: 380px; max-height: 460px; overflow: auto; padding: 8px 0; }
.msg { display: flex; margin-bottom: 12px; }
.msg.user { justify-content: flex-end; }
.msg.assistant { justify-content: flex-start; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 10px; }
.msg.user .bubble { background: #3b82f6; color: #fff; }
.msg.assistant .bubble { background: #f1f5f9; }
.msg-role { font-size: 12px; opacity: .7; margin-bottom: 4px; }
.msg-src { margin-top: 8px; }
.chat-input { display: flex; gap: 10px; margin-top: 12px; }
.toolbar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.gen-item { border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
.gen-head { display: flex; gap: 8px; align-items: center; }
.gen-opts { padding-left: 20px; margin-top: 8px; color: #374151; }
.gen-answer { margin-top: 8px; color: #059669; }
.gen-actions { margin-top: 10px; display: flex; gap: 10px; align-items: center; }
</style>
