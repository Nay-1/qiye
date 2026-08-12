<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="课程名称" clearable style="width:220px" @keyup.enter="load" />
      <el-select v-model="category" placeholder="分类" clearable style="width:140px">
        <el-option label="技术" value="技术" /><el-option label="管理" value="管理" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增课程</el-button>
    </div>

    <el-table :data="courses" v-loading="loading" border>
      <el-table-column prop="name" label="课程名称" min-width="180" />
      <el-table-column prop="category" label="分类" width="90" align="center" />
      <el-table-column prop="level" label="难度" width="80" align="center" />
      <el-table-column prop="jobName" label="适用岗位" width="140" />
      <el-table-column prop="chapterCount" label="章节" width="70" align="center" />
      <el-table-column prop="description" label="简介" show-overflow-tooltip />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" size="small" @click="goLearn(row)">预览学习</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size"
                   :current-page="page" @current-change="p => { page = p; load() }" />

    <el-dialog v-model="dialog" :title="courseForm.id ? '编辑课程' : '新增课程'" width="760px" top="5vh">
      <el-tabs v-model="editTab">
        <el-tab-pane label="基本信息" name="base">
          <el-form :model="courseForm" label-width="90px">
            <el-form-item label="课程名称" required><el-input v-model="courseForm.name" /></el-form-item>
            <el-row :gutter="12">
              <el-col :span="8"><el-form-item label="分类"><el-input v-model="courseForm.category" placeholder="技术/管理" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="难度">
                <el-select v-model="courseForm.level" style="width:100%"><el-option v-for="l in ['初级','中级','高级']" :key="l" :label="l" :value="l" /></el-select>
              </el-form-item></el-col>
              <el-col :span="8"><el-form-item label="适用岗位">
                <el-select v-model="courseForm.jobId" clearable style="width:100%"><el-option v-for="j in jobs" :key="j.id" :label="j.name" :value="j.id" /></el-select>
              </el-form-item></el-col>
            </el-row>
            <el-form-item label="简介"><el-input v-model="courseForm.description" type="textarea" /></el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane :disabled="!courseForm.id" label="技能关联" name="skills">
          <div v-if="courseForm.id">
            <div class="toolbar">
              <el-select v-model="addSkillId" placeholder="选择技能" style="width:200px">
                <el-option v-for="s in skills" :key="s.id" :label="s.name" :value="s.id" :disabled="csList.some(x => x.skillId === s.id)" />
              </el-select>
              <el-button @click="addCourseSkill">添加技能</el-button>
            </div>
            <el-table :data="csList" border size="small">
              <el-table-column prop="skillName" label="技能" />
              <el-table-column label="是否必修" width="130">
                <template #default="{ row }"><el-switch v-model="row.required" /></template>
              </el-table-column>
              <el-table-column label="权重" width="140">
                <template #default="{ row }"><el-input-number v-model="row.weight" :min="1" :max="10" size="small" /></template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }"><el-button link type="danger" size="small" @click="csList.splice($index,1)">移除</el-button></template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane :disabled="!courseForm.id" label="章节管理" name="chapters">
          <div v-if="courseForm.id">
            <div class="toolbar">
              <el-button type="success" @click="chapters.push({ title: '', content: '' })">新增章节</el-button>
              <el-button type="primary" @click="saveChapters">保存章节</el-button>
            </div>
            <el-table :data="chapters" border size="small">
              <el-table-column label="序号" type="index" width="60" />
              <el-table-column label="章节标题">
                <template #default="{ row }"><el-input v-model="row.title" placeholder="章节标题" /></template>
              </el-table-column>
              <el-table-column label="内容">
                <template #default="{ row }"><el-input v-model="row.content" type="textarea" :rows="2" placeholder="章节内容/知识点" /></template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }"><el-button link type="danger" size="small" @click="chapters.splice($index,1)">删除</el-button></template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveBase">保存基本信息</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { coursePage, courseCreate, courseUpdate, courseDelete, courseSkills, courseSkillsSave, chaptersSave, jobList, skillList, courseDetail } from '@/api'

const router = useRouter()
const courses = ref([]), jobs = ref([]), skills = ref([])
const loading = ref(false), page = ref(1), size = ref(10), total = ref(0), keyword = ref(''), category = ref('')
const dialog = ref(false), editTab = ref('base')
const courseForm = reactive({ id: null, name: '', category: '', level: '初级', jobId: null, description: '' })
const csList = ref([]), chapters = ref([]), addSkillId = ref(null)

async function load() {
  loading.value = true
  const d = await coursePage({ page: page.value, size: size.value, keyword: keyword.value, category: category.value || undefined })
  courses.value = d.records; total.value = d.total; loading.value = false
}
function openCreate() {
  Object.assign(courseForm, { id: null, name: '', category: '技术', level: '初级', jobId: null, description: '' })
  csList.value = []; chapters.value = []; dialog.value = true; editTab.value = 'base'
}
async function openEdit(row) {
  const detail = await courseDetail(row.id)
  Object.assign(courseForm, { id: detail.id, name: detail.name, category: detail.category, level: detail.level, jobId: detail.jobId, description: detail.description })
  csList.value = (detail.skills || []).map(s => ({ skillId: s.skillId, skillName: s.skillName, weight: s.weight ?? 1, required: !!s.required }))
  chapters.value = (detail.chapters || []).map(c => ({ id: c.id, title: c.title, content: c.content }))
  dialog.value = true; editTab.value = 'base'
}
async function saveBase() {
  if (!courseForm.name) return ElMessage.warning('请输入课程名称')
  if (courseForm.id) await courseUpdate({ ...courseForm })
  else { const d = await courseCreate({ ...courseForm }); }
  ElMessage.success('基本信息已保存')
  if (!courseForm.id) {
    // 新建后重新打开编辑以配置技能/章节
    const d = await coursePage({ page: 1, size: 1, keyword: courseForm.name })
    openEdit(d.records[0])
  }
  load()
}
function addCourseSkill() {
  const s = skills.value.find(x => x.id === addSkillId.value)
  if (s) { csList.value.push({ skillId: s.id, skillName: s.name, weight: 1, required: false }); addSkillId.value = null }
}
async function saveChapters() {
  await chaptersSave(courseForm.id, chapters.value.filter(c => c.title))
  await saveCourseSkills()
  ElMessage.success('章节已保存')
}
async function saveCourseSkills() {
  await courseSkillsSave(courseForm.id, csList.value.map(x => ({ skillId: x.skillId, weight: x.weight, required: x.required })))
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除课程「${row.name}」？`, '提示', { type: 'warning' })
  await courseDelete(row.id); ElMessage.success('已删除'); load()
}
function goLearn(row) { router.push(`/learn/${row.id}`) }

onMounted(() => { load(); jobList().then(d => jobs.value = d); skillList().then(d => skills.value = d) })
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
