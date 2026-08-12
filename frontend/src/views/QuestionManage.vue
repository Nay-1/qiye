<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="题干搜索" clearable style="width:220px" @keyup.enter="load" />
      <el-select v-model="type" placeholder="题型" clearable style="width:130px">
        <el-option label="单选题" value="SINGLE" /><el-option label="多选题" value="MULTIPLE" /><el-option label="判断题" value="JUDGE" />
      </el-select>
      <el-select v-model="skillFilter" placeholder="考核技能" clearable style="width:160px">
        <el-option v-for="s in skills" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新增题目</el-button>
      <el-button type="warning" @click="$router.push('/ai')">AI 生成试题</el-button>
    </div>

    <el-table :data="questions" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="题型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'SINGLE' ? 'primary' : row.type === 'MULTIPLE' ? 'warning' : 'info'">
            {{ { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断' }[row.type] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="题干" prop="content" show-overflow-tooltip min-width="260" />
      <el-table-column label="答案" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'JUDGE'" size="small">{{ row.answer === 'TRUE' ? '对' : '错' }}</el-tag>
          <el-tag v-else size="small" type="success">{{ row.answer }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="skillNames" label="考核技能" min-width="140" />
      <el-table-column prop="source" label="来源" width="80" align="center">
        <template #default="{ row }"><el-tag size="small" :type="row.source === 'AI' ? 'danger' : 'info'" effect="plain">{{ row.source === 'AI' ? 'AI' : '人工' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size"
                   :current-page="page" @current-change="p => { page = p; load() }" />

    <el-dialog v-model="dialog" :title="form.id ? '编辑题目' : '新增题目'" width="680px" top="6vh">
      <el-form label-width="90px">
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio value="SINGLE">单选题</el-radio>
            <el-radio value="MULTIPLE">多选题</el-radio>
            <el-radio value="JUDGE">判断题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.content" type="textarea" :rows="2" />
        </el-form-item>

        <el-form-item v-if="form.type !== 'JUDGE'" label="选项" required>
          <div style="width:100%">
            <div v-for="(opt, i) in form.options" :key="i" class="opt-row">
              <el-input v-model="opt.key" :disabled="!editing" placeholder="Key" style="width:70px" />
              <el-input v-model="opt.text" placeholder="选项内容" style="flex:1" />
              <el-button link type="danger" @click="form.options.splice(i, 1)">删除</el-button>
            </div>
            <el-button size="small" @click="form.options.push({ key: nextKey(), text: '' })">添加选项</el-button>
          </div>
        </el-form-item>

        <el-form-item v-if="form.type === 'JUDGE'" label="正确答案">
          <el-radio-group v-model="form.answer">
            <el-radio value="TRUE">正确</el-radio>
            <el-radio value="FALSE">错误</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-else label="正确答案">
          <el-checkbox-group v-if="form.type === 'MULTIPLE'" v-model="multiAnswer">
            <el-checkbox v-for="o in form.options" :key="o.key" :label="o.key" :value="o.key">{{ o.key }}</el-checkbox>
          </el-checkbox-group>
          <el-radio-group v-else v-model="form.answer">
            <el-radio v-for="o in form.options" :key="o.key" :label="o.key" :value="o.key">{{ o.key }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="考核技能" required>
          <el-select v-model="form.skillIds" multiple placeholder="至少选择一个技能（否则无法进入技能画像）" style="width:100%">
            <el-option v-for="s in skills" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionPage, questionCreate, questionUpdate, questionDelete, skillList } from '@/api'

const questions = ref([]), skills = ref([])
const loading = ref(false), page = ref(1), size = ref(10), total = ref(0)
const keyword = ref(''), type = ref(''), skillFilter = ref(null)
const dialog = ref(false), editing = ref(true), multiAnswer = ref([])
const form = reactive({ id: null, type: 'SINGLE', content: '', options: [], answer: '', analysis: '', skillIds: [] })

function nextKey() {
  const used = new Set(form.options.map(o => o.key))
  return 'ABCDEFGH'.split('').find(k => !used.has(k)) || 'X'
}
function onTypeChange() {
  if (form.type === 'JUDGE') { form.options = []; form.answer = 'TRUE' }
  else { form.options = [{ key: 'A', text: '' }, { key: 'B', text: '' }, { key: 'C', text: '' }, { key: 'D', text: '' }] }
}

async function load() {
  loading.value = true
  const d = await questionPage({ page: page.value, size: size.value, keyword: keyword.value, type: type.value || undefined, skillId: skillFilter || undefined })
  questions.value = d.records; total.value = d.total; loading.value = false
}
function openCreate() {
  Object.assign(form, { id: null, type: 'SINGLE', content: '', options: [], answer: '', analysis: '', skillIds: [] })
  onTypeChange(); dialog.value = true; editing.value = true
}
function openEdit(row) {
  Object.assign(form, { id: row.id, type: row.type, content: row.content, answer: row.answer, analysis: row.analysis, skillIds: [...row.skillIds] })
  form.options = row.options?.map(o => ({ key: o.key, text: o.text })) || []
  multiAnswer.value = row.type === 'MULTIPLE' ? (row.answer || '').split(',').filter(Boolean) : []
  dialog.value = true; editing.value = true
}
async function save() {
  if (!form.content) return ElMessage.warning('请输入题干')
  if (!form.skillIds.length) return ElMessage.warning('请至少绑定一个考核技能')
  let answer = form.answer
  if (form.type === 'MULTIPLE') answer = [...multiAnswer.value].sort().join(',')
  if (!answer) return ElMessage.warning('请设置正确答案')
  const payload = { ...form, answer, options: form.type === 'JUDGE' ? [] : form.options.filter(o => o.text) }
  if (form.id) await questionUpdate(payload)
  else await questionCreate(payload)
  ElMessage.success('保存成功'); dialog.value = false; load()
}
async function remove(row) {
  await ElMessageBox.confirm(`确定删除该题目？`, '提示', { type: 'warning' })
  await questionDelete(row.id); ElMessage.success('已删除'); load()
}

onMounted(() => { load(); skillList().then(d => skills.value = d) })
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.pager { margin-top: 12px; justify-content: flex-end; }
.opt-row { display: flex; gap: 8px; margin-bottom: 8px; }
</style>
