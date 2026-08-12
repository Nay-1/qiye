<template>
  <div v-loading="loading">
    <div v-if="store.canManage" class="toolbar">
      <el-select v-model="userId" filterable placeholder="选择员工查看画像" style="width:260px" @change="load">
        <el-option v-for="u in employees" :key="u.id" :label="`${u.name}（${u.username}）`" :value="u.id" />
      </el-select>
    </div>

    <el-row :gutter="16" v-if="profile">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><b>{{ profile.userName }} 的技能画像</b></template>
          <div ref="radarRef" style="height:360px" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div class="head">
              <b>技能明细</b>
              <el-button type="primary" size="small" @click="loadAdvice">获取 AI 学习建议</el-button>
            </div>
          </template>
          <el-table :data="profile.skills" size="small" border>
            <el-table-column prop="skillName" label="技能" min-width="110" />
            <el-table-column prop="currentLevel" label="当前等级" width="90" align="center">
              <template #default="{ row }"><el-tag size="small" :type="levelTag(row.currentLevel)">{{ row.currentLevel }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="targetLevel" label="目标等级" width="90" align="center">
              <template #default="{ row }"><span :class="{ 'target-miss': row.targetLevel !== '--' && row.currentLevel === '未考核' }">{{ row.targetLevel }}</span></template>
            </el-table-column>
            <el-table-column label="得分" width="90" align="center">
              <template #default="{ row }">{{ row.score ?? '—' }}</template>
            </el-table-column>
            <el-table-column label="达成率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.rate ?? 0" :color="row.reached ? '#10b981' : '#ef4444'" :stroke-width="10" :show-text="false" style="width:80%;display:inline-block" />
                <span style="margin-left:6px;font-size:12px">{{ row.rate ?? '—' }}%</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.weak" type="danger" size="small">薄弱</el-tag>
                <el-tag v-else-if="row.reached" type="success" size="small">达标</el-tag>
                <el-tag v-else type="info" size="small">未考核</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="advices.length" shadow="never" style="margin-top:16px">
      <template #header><b>AI 学习建议（基于薄弱技能）</b></template>
      <el-alert v-for="(a, i) in advices" :key="i" :title="`${a.skillName}：得分 ${a.score}，目标 ${a.targetLevel}，达成率 ${a.rate}%`" type="warning" :closable="false" style="margin-bottom:12px">
        <template #default>
          <div style="margin-bottom:6px">推荐课程：<el-tag v-for="c in a.courses" :key="c.id" size="small" style="margin-right:6px">{{ c.name }}</el-tag></div>
          <div style="line-height:1.8">{{ a.advice }}</div>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { skillProfileMine, skillProfileUser, aiStudyAdvice, userPage } from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const store = useUserStore()
const profile = ref(null)
const userId = ref(null)
const employees = ref([])
const advices = ref([])
const loading = ref(false)
const radarRef = ref()

function levelTag(lv) { return { '初级': 'info', '中级': 'warning', '高级': 'danger', '未考核': '' }[lv] || 'info' }

async function load() {
  loading.value = true
  const uid = userId.value ?? route.params.userId ?? null
  profile.value = uid ? await skillProfileUser(uid) : await skillProfileMine()
  advices.value = []
  loading.value = false
  nextTickRenderRadar()
}

function nextTickRenderRadar() {
  setTimeout(() => {
    if (!radarRef.value || !profile.value?.skills.length) return
    const chart = echarts.init(radarRef.value)
    const sk = profile.value.skills
    const names = sk.map(s => s.skillName)
    const targetLine = { '初级': 60, '中级': 60, '高级': 80 }
    chart.setOption({
      tooltip: {},
      legend: {
        bottom: 0,
        left: 'center',
        itemWidth: 14,
        itemHeight: 8,
        textStyle: { color: '#6B7B72', fontSize: 12 },
        data: ['当前得分', '达标线']
      },
      radar: {
        indicator: names.map(n => ({ name: n, max: 100 })),
        radius: '64%',
        center: ['50%', '48%'],
        splitNumber: 5,
        axisName: { color: '#33423A', fontSize: 12 },
        splitArea: { areaStyle: { color: ['#FBFDFC', '#EFF6F1'] } },
        splitLine: { lineStyle: { color: '#DCE7E0' } },
        axisLine: { lineStyle: { color: '#DCE7E0' } }
      },
      series: [
        {
          type: 'radar',
          name: '当前得分',
          symbolSize: 4,
          data: [{
            value: sk.map(s => s.score ?? 0),
            name: '当前得分',
            areaStyle: { color: 'rgba(28,107,79,.22)' },
            lineStyle: { color: '#1C6B4F', width: 2 },
            itemStyle: { color: '#1C6B4F' }
          }]
        },
        {
          type: 'radar',
          name: '达标线',
          symbol: 'none',
          data: [{
            value: sk.map(s => targetLine[s.targetLevel] ?? null),
            name: '达标线',
            areaStyle: { show: false },
            lineStyle: { color: '#E5A13C', width: 2, type: 'dashed' },
            itemStyle: { color: '#E5A13C' }
          }]
        }
      ]
    })
  }, 50)
}

async function loadAdvice() {
  advices.value = await aiStudyAdvice()
  if (!advices.value.length) {
    ElMessage.info('当前没有薄弱技能，继续保持！')
  }
}

onMounted(async () => {
  if (store.canManage) {
    const d = await userPage({ page: 1, size: 500 })
    employees.value = d.records.filter(u => u.roleCode === 'EMPLOYEE')
    if (route.params.userId) userId.value = Number(route.params.userId)
    else if (employees.value.length) userId.value = employees.value[0].id
  }
  load()
  watch(() => route.params.userId, () => load())
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.target-miss { color: #ef4444; }
</style>
