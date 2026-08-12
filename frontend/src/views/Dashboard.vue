<template>
  <div>
    <el-row :gutter="16" class="cards">
      <el-col v-for="c in cards" :key="c.label" :xs="12" :md="6">
        <el-card shadow="never" class="stat-card">
          <div class="card-num serif-num" :class="c.tone">{{ c.value }}</div>
          <div class="card-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12"><el-card shadow="never"><template #header><b>考试成绩趋势</b></template><div ref="trendRef" class="chart" /></el-card></el-col>
      <el-col :span="12"><el-card shadow="never"><template #header><b>课程学习完成率</b></template><div ref="courseRef" class="chart" /></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8"><el-card shadow="never"><template #header><b>薄弱技能 Top5</b></template><div ref="weakRef" class="chart" /></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><template #header><b>部门培训完成率</b></template><div ref="deptRef" class="chart" /></el-card></el-col>
      <el-col :span="8"><el-card shadow="never"><template #header><b>员工技能得分排名</b></template><div ref="rankRef" class="chart" /></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { statsOverview, statsExam, statsStudy, statsSkill, statsDept, statsRanking } from '@/api'

const cards = ref([])
const trendRef = ref(), courseRef = ref(), weakRef = ref(), deptRef = ref(), rankRef = ref()

function initChart(el) {
  return echarts.init(el)
}

function renderTrend(data) {
  const chart = initChart(trendRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: data.trend.map(t => t.label), axisLabel: { fontSize: 10 } },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ type: 'line', smooth: true, data: data.trend.map(t => t.score), areaStyle: { color: 'rgba(28,107,79,.15)' }, lineStyle: { color: '#1C6B4F', width: 2.5 }, itemStyle: { color: '#1C6B4F' }, symbolSize: 6 }]
  })
}

function renderCourse(data) {
  const chart = initChart(courseRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 30, bottom: 50 },
    xAxis: { type: 'category', data: data.courses.map(c => c.courseName), axisLabel: { fontSize: 10, interval: 0, rotate: 25 } },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: data.courses.map(c => c.completeRate), itemStyle: { color: '#2A7F8A', borderRadius: [4,4,0,0] }, barWidth: 20 }]
  })
}

function renderWeak(data) {
  const chart = initChart(weakRef.value)
  chart.setOption({
    tooltip: {},
    grid: { left: 60, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.weakTop.map(w => w.skillName).reverse() },
    series: [{ type: 'bar', data: data.weakTop.map(w => w.count).reverse(), itemStyle: { color: '#C2563B', borderRadius: [0,4,4,0] }, barWidth: 16 }]
  })
}

function renderDept(data) {
  const chart = initChart(deptRef.value)
  chart.setOption({
    color: ['#1C6B4F', '#E5A13C', '#2A7F8A', '#6B7B72', '#C2563B', '#8EB5A7'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '65%'], center: ['50%', '42%'],
      data: data.map(d => ({ name: d.deptName, value: d.studyRate })),
      label: { formatter: '{b}\n{c}%', fontSize: 11, color: '#33423A' },
      itemStyle: { borderRadius: 4 }
    }]
  })
}

function renderRank(data) {
  const chart = initChart(rankRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'value', max: 100 },
    yAxis: { type: 'category', data: data.map(r => r.userName).reverse() },
    series: [{ type: 'bar', data: data.map(r => r.avgScore).reverse(), itemStyle: { color: '#E5A13C', borderRadius: [0,4,4,0] }, barWidth: 16, label: { show: true, position: 'right', color: '#33423A' } }]
  })
}

onMounted(async () => {
  const [ov, ex, st, sk, dp, rk] = await Promise.all([
    statsOverview(), statsExam(), statsStudy(), statsSkill(), statsDept(), statsRanking()
  ])
  const tones = ['pine', 'amber', 'teal', 'clay']
  cards.value = [
    { label: '企业员工', value: ov.userCount },
    { label: '培训课程', value: ov.courseCount },
    { label: '在考考试', value: ov.examCount },
    { label: '知识文档', value: ov.docCount },
    { label: '参与学习率', value: ov.studyRate + '%' },
    { label: '考试平均分', value: ov.avgScore },
    { label: '考试通过率', value: ov.passRate + '%' },
    { label: '薄弱技能项', value: ov.weakSkillCount }
  ].map((c, i) => ({ ...c, tone: tones[i % tones.length] }))
  renderTrend(ex)
  renderCourse(st)
  renderWeak(sk)
  renderDept(dp)
  renderRank(rk)
  window.addEventListener('resize', () => {
    ;[trendRef, courseRef, weakRef, deptRef, rankRef].forEach(r => r.value && echarts.getInstanceByDom(r.value)?.resize())
  })
})
</script>

<style scoped>
.cards { margin-bottom: 16px; }
.cards .el-col { margin-bottom: 16px; }
.stat-card { position: relative; overflow: hidden; }
.stat-card::after {
  content: '';
  position: absolute; right: -34px; top: -38px;
  width: 120px; height: 120px;
  border-radius: 50%;
  border: 1px solid #DCE7E0;
  box-shadow: 0 0 0 14px rgba(220,231,224,.35), 0 0 0 30px rgba(220,231,224,.22), 0 0 0 48px rgba(220,231,224,.12);
  pointer-events: none;
}
.card-num { font-size: 28px; color: var(--ink-text); position: relative; }
.card-num.pine { color: var(--pine); }
.card-num.amber { color: var(--amber-deep); }
.card-num.teal { color: var(--teal); }
.card-num.clay { color: var(--clay); }
.card-label { color: var(--muted); font-size: 13px; margin-top: 4px; position: relative; }
.el-row + .el-row { margin-top: 16px; }
.chart { height: 300px; }
</style>
