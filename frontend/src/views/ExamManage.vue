<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="考试名称" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增考试</el-button>
    </div>

    <el-table :data="exams" v-loading="loading" border>
      <el-table-column prop="title" label="考试名称" min-width="180" />
      <el-table-column prop="questionCount" label="题目数" width="80" align="center" />
      <el-table-column prop="totalScore" label="总分" width="70" align="center" />
      <el-table-column prop="duration" label="时长(分)" width="90" align="center" />
      <el-table-column prop="attempts" label="次数" width="60" align="center" />
      <el-table-column prop="passScore" label="及格线" width="70" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : row.status === 'CLOSED' ? 'danger' : 'info'">
            {{ { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="openCompose(row)">组卷</el-button>
          <el-button link type="success" size="small" @click="openScore(row)">成绩</el-button>
          <el-button v-if="row.status === 'DRAFT'" link type="success" size="small" @click="publish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" link type="danger" size="small" @click="close(row)">关闭</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size"
                   :current-page="page" @current-change="p => { page = p; load() }" />

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialog" :title="form.id ? '编辑考试' : '新增考试'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="考试名称" required><el-input v-model="form.title" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="时长(分)"><el-input-number v-model="form.duration" :min="5" :max="300" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="允许次数"><el-input-number v-model="form.attempts" :min="1" :max="10" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="及格线"><el-input-number v-model="form.passScore" :min="0" :max="100" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>

    <!-- 组卷 -->
    <el-dialog v-model="composeDialog" :title="`组卷：${composeExam?.title}`" width="900px" top="5vh">
      <el-row :gutter="16">
        <el-col :span="14">
          <b>题库选题</b>
          <el-input v-model="qKeyword" placeholder="搜索题干" size="small" clearable style="margin:8px 0" @input="loadQuestions" />
          <el-table :data="questionPool" size="small" border max-height="420">
            <el-table-column label="题型" width="60">
              <template #default="{ row }"><el-tag size="small">{{ { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[row.type] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="content" label="题干" show-overflow-tooltip />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button v-if="!composeItems.some(x => x.questionId === row.id)" link type="primary" size="small" @click="addQ(row)">加入</el-button>
                <span v-else style="color:#10b981">已选</span>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination layout="prev, pager, next, total" small :total="qTotal" :page-size="qSize" :current-page="qPage"
                         @current-change="p => { qPage = p; loadQuestions() }" />
        </el-col>
        <el-col :span="10">
          <b>已选题目（共 {{ composeTotal }} 分）</b>
          <el-table :data="composeItems" size="small" border max-height="460" style="margin-top:8px">
            <el-table-column label="排序" type="index" width="50" />
            <el-table-column prop="content" label="题干" show-overflow-tooltip />
            <el-table-column label="分值" width="110">
              <template #default="{ row }"><el-input-number v-model="row.score" :min="1" :max="100" size="small" style="width:90px" /></template>
            </el-table-column>
            <el-table-column label="" width="60">
              <template #default="{ $index }"><el-button link type="danger" size="small" @click="composeItems.splice($index,1)">移除</el-button></template>
            </el-table-column>
          </el-table>
          <el-button type="primary" style="margin-top:10px;width:100%" @click="saveCompose">保存组卷</el-button>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 成绩 -->
    <el-dialog v-model="scoreDialog" :title="`成绩：${scoreExam?.title}`" width="700px">
      <el-table :data="scores" size="small" border max-height="420">
        <el-table-column prop="userName" label="考生" width="120" />
        <el-table-column prop="attemptNo" label="第几次" width="80" align="center" />
        <el-table-column label="得分" width="110">
          <template #default="{ row }"><b>{{ row.totalScore ?? '—' }}</b></template>
        </el-table-column>
        <el-table-column label="是否通过" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.passed ? 'success' : 'danger'">{{ row.passed ? '通过' : '未通过' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { examPage, examCreate, examUpdate, examDelete, examPublish, examClose, questionPage,
         examDetail, examSaveQuestions, attemptByExam } from '@/api'

const exams = ref([]), loading = ref(false), page = ref(1), size = ref(10), total = ref(0), keyword = ref('')
const dialog = ref(false)
const form = reactive({ id: null, title: '', duration: 60, attempts: 1, passScore: 60 })

const composeDialog = ref(false), composeExam = ref(null), composeItems = ref([])
const questionPool = ref([]), qKeyword = ref(''), qPage = ref(1), qSize = ref(10), qTotal = ref(0)
const scoreDialog = ref(false), scoreExam = ref(null), scores = ref([])

const composeTotal = computed(() => composeItems.value.reduce((s, x) => s + (x.score || 0), 0))

async function load() {
  loading.value = true
  const d = await examPage({ page: page.value, size: size.value, keyword: keyword.value })
  exams.value = d.records; total.value = d.total; loading.value = false
}
function openCreate() { Object.assign(form, { id: null, title: '', duration: 60, attempts: 1, passScore: 60 }); dialog.value = true }
function openEdit(row) { Object.assign(form, { id: row.id, title: row.title, duration: row.duration, attempts: row.attempts, passScore: row.passScore }); dialog.value = true }
async function save() {
  if (!form.title) return ElMessage.warning('请输入考试名称')
  form.id ? await examUpdate({ ...form }) : await examCreate({ ...form })
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function publish(row) { await examPublish(row.id); ElMessage.success('已发布'); load() }
async function close(row) { await examClose(row.id); ElMessage.success('已关闭'); load() }
async function remove(row) {
  await ElMessageBox.confirm(`确定删除考试「${row.title}」？`, '提示', { type: 'warning' })
  await examDelete(row.id); ElMessage.success('已删除'); load()
}

async function openCompose(row) {
  const detail = await examDetail(row.id)
  composeExam.value = row
  composeItems.value = (detail.questionList || []).map(eq => ({ questionId: eq.questionId, content: eq.question?.content, score: eq.score, type: eq.question?.type }))
  composeDialog.value = true
  loadQuestions()
}
async function loadQuestions() {
  const d = await questionPage({ page: qPage.value, size: qSize.value, keyword: qKeyword.value })
  questionPool.value = d.records; qTotal.value = d.total
}
function addQ(row) { composeItems.value.push({ questionId: row.id, content: row.content, score: 10, type: row.type }) }
async function saveCompose() {
  if (!composeItems.value.length) return ElMessage.warning('请至少选择一道题目')
  await examSaveQuestions(composeExam.value.id, composeItems.value.map(x => ({ questionId: x.questionId, score: x.score })))
  ElMessage.success('组卷已保存'); composeDialog.value = false; load()
}

async function openScore(row) {
  scoreExam.value = row
  scores.value = await attemptByExam(row.id)
  scoreDialog.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
