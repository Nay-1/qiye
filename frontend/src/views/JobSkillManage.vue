<template>
  <el-card shadow="never">
    <el-tabs v-model="tab">
      <!-- 岗位管理 -->
      <el-tab-pane label="岗位管理" name="job">
        <div class="toolbar">
          <el-button type="success" @click="openJobDialog()">新增岗位</el-button>
        </div>
        <el-table :data="jobs" border>
          <el-table-column prop="name" label="岗位名称" />
          <el-table-column prop="deptName" label="所属部门" width="140" />
          <el-table-column prop="skillCount" label="技能数" width="90" align="center" />
          <el-table-column prop="description" label="岗位描述" show-overflow-tooltip />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openJobDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="removeJob(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 技能管理 -->
      <el-tab-pane label="技能管理" name="skill">
        <div class="toolbar">
          <el-button type="success" @click="openSkillDialog()">新增技能</el-button>
        </div>
        <el-table :data="skills" border>
          <el-table-column prop="name" label="技能名称" />
          <el-table-column prop="description" label="技能说明" />
          <el-table-column prop="jobCount" label="被岗位引用" width="100" align="center" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openSkillDialog(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="removeSkill(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 岗位技能配置 -->
      <el-tab-pane label="岗位技能配置" name="cfg">
        <div class="toolbar">
          <el-select v-model="cfgJobId" placeholder="选择岗位" style="width:240px" @change="loadJobSkills">
            <el-option v-for="j in jobs" :key="j.id" :label="j.name" :value="j.id" />
          </el-select>
          <el-button type="primary" :disabled="!cfgJobId" @click="saveJobSkills">保存配置</el-button>
        </div>
        <el-empty v-if="!cfgJobId" description="请先选择岗位" />
        <el-table v-else :data="cfgSkills" border>
          <el-table-column prop="skillName" label="技能" />
          <el-table-column label="目标等级" width="220">
            <template #default="{ row }">
              <el-select v-model="row.targetLevel" size="small" style="width:120px">
                <el-option v-for="lv in ['初级','中级','高级']" :key="lv" :label="lv" :value="lv" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="权重" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.weight" :min="1" :max="10" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="cfgSkills.splice($index, 1)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="cfgJobId" class="toolbar" style="margin-top:12px">
          <el-select v-model="addSkillId" placeholder="添加技能" style="width:240px">
            <el-option v-for="s in skills" :key="s.id" :label="s.name" :value="s.id" :disabled="cfgSkills.some(x => x.skillId === s.id)" />
          </el-select>
          <el-button @click="addSkill">添加</el-button>
        </div>
      </el-tab-pane>

      <!-- 员工岗位分配 -->
      <el-tab-pane label="员工岗位分配" name="userJob">
        <div class="toolbar">
          <el-select v-model="assignUserId" filterable placeholder="选择员工" style="width:240px" @change="loadUserJob">
            <el-option v-for="u in employees" :key="u.id" :label="`${u.name}（${u.username}）`" :value="u.id" />
          </el-select>
          <el-button type="primary" :disabled="!assignUserId" @click="saveAssign">保存分配</el-button>
        </div>
        <el-empty v-if="!assignUserId" description="请先选择员工" />
        <template v-else>
          <el-checkbox-group v-model="assignJobIds">
            <el-checkbox v-for="j in jobs" :key="j.id" :label="j.id" border style="margin-right:12px">
              {{ j.name }}
            </el-checkbox>
          </el-checkbox-group>
          <div style="margin-top:16px">
            <span style="margin-right:12px">主岗位：</span>
            <el-radio-group v-model="primaryJobId">
              <el-radio v-for="id in assignJobIds" :key="id" :label="id">
                {{ jobs.find(j => j.id === id)?.name }}
              </el-radio>
            </el-radio-group>
          </div>
        </template>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="jobDialog" :title="jobForm.id ? '编辑岗位' : '新增岗位'" width="440px">
      <el-form label-width="80px">
        <el-form-item label="岗位名称" required><el-input v-model="jobForm.name" /></el-form-item>
        <el-form-item label="所属部门">
          <el-select v-model="jobForm.deptId" style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位描述"><el-input v-model="jobForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="jobDialog=false">取消</el-button><el-button type="primary" @click="saveJob">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="skillDialog" :title="skillForm.id ? '编辑技能' : '新增技能'" width="440px">
      <el-form label-width="80px">
        <el-form-item label="技能名称" required><el-input v-model="skillForm.name" /></el-form-item>
        <el-form-item label="技能说明"><el-input v-model="skillForm.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="skillDialog=false">取消</el-button><el-button type="primary" @click="saveSkill">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { jobList, jobCreate, jobUpdate, jobDelete, skillList, skillCreate, skillUpdate, skillDelete,
         jobSkills, jobSkillsSave, deptList, userPage, userJobOf, userJobAssign } from '@/api'

const tab = ref('job')
const jobs = ref([]), skills = ref([]), depts = ref([]), employees = ref([])
const jobDialog = ref(false), jobForm = reactive({ id: null, name: '', deptId: null, description: '' })
const skillDialog = ref(false), skillForm = reactive({ id: null, name: '', description: '' })

const cfgJobId = ref(null), cfgSkills = ref([]), addSkillId = ref(null)
const assignUserId = ref(null), assignJobIds = ref([]), primaryJobId = ref(null)

const availableSkills = computed(() => skills.value.filter(s => !cfgSkills.value.some(x => x.skillId === s.id)))

async function loadJobs() { jobs.value = await jobList() }
async function loadSkills() { skills.value = await skillList() }
async function loadDepts() { depts.value = await deptList() }
async function loadEmployees() { const d = await userPage({ page: 1, size: 500 }); employees.value = d.records.filter(u => u.roleCode === 'EMPLOYEE') }

function openJobDialog(row) { row ? Object.assign(jobForm, { id: row.id, name: row.name, deptId: row.deptId, description: row.description }) : Object.assign(jobForm, { id: null, name: '', deptId: null, description: '' }); jobDialog.value = true }
async function saveJob() {
  if (!jobForm.name) return ElMessage.warning('请输入岗位名称')
  jobForm.id ? await jobUpdate({ ...jobForm }) : await jobCreate({ ...jobForm })
  ElMessage.success('保存成功'); jobDialog.value = false; loadJobs()
}
async function removeJob(row) {
  await ElMessageBox.confirm(`确定删除岗位「${row.name}」？`, '提示', { type: 'warning' })
  await jobDelete(row.id); ElMessage.success('已删除'); loadJobs()
}

function openSkillDialog(row) { row ? Object.assign(skillForm, { id: row.id, name: row.name, description: row.description }) : Object.assign(skillForm, { id: null, name: '', description: '' }); skillDialog.value = true }
async function saveSkill() {
  if (!skillForm.name) return ElMessage.warning('请输入技能名称')
  skillForm.id ? await skillUpdate({ ...skillForm }) : await skillCreate({ ...skillForm })
  ElMessage.success('保存成功'); skillDialog.value = false; loadSkills()
}
async function removeSkill(row) {
  await ElMessageBox.confirm(`确定删除技能「${row.name}」？`, '提示', { type: 'warning' })
  await skillDelete(row.id); ElMessage.success('已删除'); loadSkills()
}

async function loadJobSkills() {
  const list = await jobSkills(cfgJobId.value)
  cfgSkills.value = list.map(x => ({ skillId: x.skillId, skillName: x.skillName, targetLevel: x.targetLevel, weight: x.weight }))
}
function addSkill() {
  const s = skills.value.find(x => x.id === addSkillId.value)
  if (s) { cfgSkills.value.push({ skillId: s.id, skillName: s.name, targetLevel: '中级', weight: 1 }); addSkillId.value = null }
}
async function saveJobSkills() {
  if (!cfgSkills.value.length) return ElMessage.warning('请至少配置一个技能')
  await jobSkillsSave(cfgJobId.value, cfgSkills.value.map(x => ({ skillId: x.skillId, targetLevel: x.targetLevel, weight: x.weight })))
  ElMessage.success('配置已保存'); loadJobs(); loadSkills()
}

async function loadUserJob() {
  const list = await userJobOf(assignUserId.value)
  assignJobIds.value = list.map(x => x.jobId)
  primaryJobId.value = list.find(x => x.isPrimary)?.jobId ?? list[0]?.jobId ?? null
}
async function saveAssign() {
  if (!assignJobIds.value.length) return ElMessage.warning('请至少选择一个岗位')
  if (!primaryJobId.value) return ElMessage.warning('请选择主岗位')
  await userJobAssign({ userId: assignUserId.value, primaryJobId: primaryJobId.value, jobIds: assignJobIds.value })
  ElMessage.success('分配已保存，培训任务已自动生成')
}

onMounted(() => { loadJobs(); loadSkills(); loadDepts(); loadEmployees() })
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
</style>
