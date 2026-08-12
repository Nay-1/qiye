<template>
  <el-card shadow="never">
    <el-tabs v-model="tab">
      <el-tab-pane label="用户管理" name="user">
        <div class="toolbar">
          <el-input v-model="keyword" placeholder="用户名/姓名" clearable style="width:220px" @keyup.enter="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openCreate">新增用户</el-button>
        </div>
        <el-table :data="users" v-loading="loading" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="deptName" label="部门" width="110" />
          <el-table-column prop="roleName" label="角色" width="130" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="260">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
              <el-button link :type="row.enabled ? 'danger' : 'success'" size="small" @click="toggle(row)">{{ row.enabled ? '禁用' : '启用' }}</el-button>
              <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size"
                       :current-page="page" @current-change="p => { page = p; load() }" />
      </el-tab-pane>

      <el-tab-pane label="部门管理" name="dept">
        <div class="toolbar">
          <el-button type="success" @click="deptDialog = true; deptForm = { name: '' }">新增部门</el-button>
        </div>
        <el-table :data="depts" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="部门名称" />
          <el-table-column prop="userCount" label="员工数" width="100" align="center" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="deptDialog = true; deptForm = { ...row }">编辑</el-button>
              <el-button link type="danger" size="small" @click="removeDept(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialog" :title="editId ? '编辑用户' : '新增用户'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名" required><el-input v-model="form.username" :disabled="!!editId" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item v-if="!editId" label="密码" required><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.deptId" style="width:100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleId" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="deptDialog" :title="deptForm.id ? '编辑部门' : '新增部门'" width="360px">
      <el-form label-width="80px">
        <el-form-item label="名称" required><el-input v-model="deptForm.name" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDialog = false">取消</el-button>
        <el-button type="primary" @click="saveDept">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userPage, userCreate, userUpdate, userResetPwd, userToggleStatus, userDelete,
         deptList, deptCreate, deptUpdate, deptDelete, roleList } from '@/api'

const tab = ref('user')
const users = ref([]), depts = ref([]), roles = ref([])
const loading = ref(false), page = ref(1), size = ref(10), total = ref(0), keyword = ref('')
const dialog = ref(false), editId = ref(null)
const form = reactive({ username: '', name: '', password: '', deptId: null, roleId: null })
const deptDialog = ref(false), deptForm = reactive({ id: null, name: '' })

async function load() {
  loading.value = true
  const d = await userPage({ page: page.value, size: size.value, keyword: keyword.value })
  users.value = d.records
  total.value = d.total
  loading.value = false
}
async function loadDepts() { depts.value = await deptList() }
async function loadRoles() { roles.value = await roleList() }

function openCreate() { editId.value = null; Object.assign(form, { username: '', name: '', password: '', deptId: null, roleId: null }); dialog.value = true }
function openEdit(row) { editId.value = row.id; Object.assign(form, { username: row.username, name: row.name, deptId: row.deptId, roleId: row.roleId }); dialog.value = true }

async function saveUser() {
  if (!form.username || !form.name) return ElMessage.warning('请填写用户名和姓名')
  if (!editId.value && !form.password) return ElMessage.warning('请填写密码')
  if (editId.value) await userUpdate({ id: editId.value, name: form.name, deptId: form.deptId, roleId: form.roleId })
  else await userCreate({ ...form })
  ElMessage.success('保存成功')
  dialog.value = false
  load()
}
async function resetPwd(row) {
  const { value } = await ElMessageBox.prompt('输入新密码', `重置 ${row.username} 的密码`, { inputType: 'password' })
  await userResetPwd(row.id, value)
  ElMessage.success('密码已重置')
}
async function toggle(row) { await userToggleStatus(row.id); load() }
async function remove(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.name}」？`, '提示', { type: 'warning' })
  await userDelete(row.id)
  ElMessage.success('已删除')
  load()
}
async function saveDept() {
  if (!deptForm.name) return ElMessage.warning('请输入部门名称')
  if (deptForm.id) await deptUpdate({ ...deptForm })
  else await deptCreate({ name: deptForm.name })
  ElMessage.success('保存成功')
  deptDialog.value = false
  loadDepts()
}
async function removeDept(row) {
  await ElMessageBox.confirm(`确定删除部门「${row.name}」？`, '提示', { type: 'warning' })
  await deptDelete(row.id)
  loadDepts()
}

onMounted(() => { load(); loadDepts(); loadRoles() })
</script>

<style scoped>
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; }
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
