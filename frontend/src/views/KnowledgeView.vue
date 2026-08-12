<template>
  <el-card shadow="never">
    <template v-if="store.canManage">
      <div class="upload-box">
        <el-upload drag :auto-upload="false" :limit="1" accept=".pdf,.docx,.txt" :on-change="onFileChange" :on-remove="() => file = null">
          <el-icon :size="40" color="#909399"><UploadFilled /></el-icon>
          <div>拖拽或点击上传知识文档（PDF / Word / TXT，≤20MB）</div>
        </el-upload>
        <div class="upload-opts">
          <span>可见部门：</span>
          <el-select v-model="deptId" clearable placeholder="不选=全部门" style="width:160px">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
          <el-button type="primary" :loading="uploading" :disabled="!file" @click="doUpload">上传并处理</el-button>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="文档名称" clearable style="width:220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="files" v-loading="loading" border>
      <el-table-column prop="name" label="文档名称" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="80" align="center">
        <template #default="{ row }"><el-tag size="small">{{ row.fileType.toUpperCase() }}</el-tag></template>
      </el-table-column>
      <el-table-column label="可见范围" width="110" align="center">
        <template #default="{ row }">{{ row.deptName || '全部门' }}</template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="切片数" width="80" align="center" />
      <el-table-column prop="size" label="大小" width="100" align="center">
        <template #default="{ row }">{{ (row.size / 1024).toFixed(1) }} KB</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="上传时间" width="160" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="store.canManage" link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pager" layout="total, prev, pager, next" :total="total" :page-size="size"
                   :current-page="page" @current-change="p => { page = p; load() }" />

    <el-dialog v-model="detailDialog" :title="detail?.name" width="640px">
      <p style="white-space: pre-wrap; color: #374151; line-height: 1.8">{{ detail?.preview }}</p>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeUpload, knowledgePage, knowledgeDetail, knowledgeDelete, deptList } from '@/api'
import { useUserStore } from '@/stores/user'

const store = useUserStore()
const files = ref([]), depts = ref([])
const loading = ref(false), page = ref(1), size = ref(10), total = ref(0), keyword = ref('')
const file = ref(null), deptId = ref(null), uploading = ref(false)
const detailDialog = ref(false), detail = ref(null)

async function load() {
  loading.value = true
  const d = await knowledgePage({ page: page.value, size: size.value, keyword: keyword.value })
  files.value = d.records; total.value = d.total; loading.value = false
}
function onFileChange(uploadFile) { file.value = uploadFile.raw }
async function doUpload() {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    if (deptId.value) fd.append('deptId', deptId.value)
    const kf = await knowledgeUpload(fd)
    ElMessage.success(`上传成功，解析出 ${kf.chunkCount} 个切片`)
    file.value = null
    load()
  } finally { uploading.value = false }
}
async function viewDetail(row) { detail.value = await knowledgeDetail(row.id); detailDialog.value = true }
async function remove(row) {
  await ElMessageBox.confirm(`确定删除文档「${row.name}」？`, '提示', { type: 'warning' })
  await knowledgeDelete(row.id); ElMessage.success('已删除'); load()
}
onMounted(() => { load(); deptList().then(d => depts.value = d).catch(() => {}) })
</script>

<style scoped>
.upload-box { margin-bottom: 16px; }
.upload-opts { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.toolbar { margin-bottom: 12px; display: flex; gap: 10px; }
.pager { margin-top: 12px; justify-content: flex-end; }
</style>
