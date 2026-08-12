<template>
  <div v-loading="loading">
    <el-row :gutter="16" class="cards">
      <el-col v-for="c in cards" :key="c.label" :span="6">
        <el-card shadow="never"><div class="card-num">{{ c.value }}</div><div class="card-label">{{ c.label }}</div></el-card>
      </el-col>
    </el-row>

    <el-card shadow="never">
      <el-tabs v-model="tab">
        <el-tab-pane label="学习统计" name="study">
          <el-table :data="study.courses || []" border size="small">
            <el-table-column prop="courseName" label="课程" />
            <el-table-column prop="chapterTotal" label="章节数" width="90" align="center" />
            <el-table-column prop="studyUsers" label="学习人数" width="90" align="center" />
            <el-table-column prop="completeUsers" label="完成人数" width="90" align="center" />
            <el-table-column label="完成率" width="140">
              <template #default="{ row }">
                <el-progress :percentage="row.completeRate" :color="row.completeRate >= 80 ? '#10b981' : '#f59e0b'" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
          <p class="stat-line">总学习时长：{{ study.totalStudyHours }} 小时，共 {{ study.totalRecords }} 条学习记录</p>
        </el-tab-pane>

        <el-tab-pane label="考试统计" name="exam">
          <el-descriptions :column="3" border style="margin-bottom:16px">
            <el-descriptions-item label="平均分">{{ exam.avgScore }}</el-descriptions-item>
            <el-descriptions-item label="通过率">{{ exam.passRate }}%</el-descriptions-item>
            <el-descriptions-item label="提交次数">{{ exam.attemptCount }}（通过 {{ exam.passCount }}）</el-descriptions-item>
          </el-descriptions>
          <el-table :data="exam.trend || []" border size="small">
            <el-table-column prop="label" label="提交时间" />
            <el-table-column label="得分">
              <template #default="{ row }"><el-tag :type="row.score >= 60 ? 'success' : 'danger'">{{ row.score }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="技能达成" name="skill">
          <el-descriptions :column="4" border style="margin-bottom:16px">
            <el-descriptions-item label="已考核">{{ skill.assessedCount }}</el-descriptions-item>
            <el-descriptions-item label="达标">{{ skill.reachedCount }}</el-descriptions-item>
            <el-descriptions-item label="薄弱">{{ skill.weakCount }}</el-descriptions-item>
            <el-descriptions-item label="达标率">{{ skill.reachedRate }}%</el-descriptions-item>
          </el-descriptions>
          <el-table :data="skill.weakTop || []" border size="small">
            <el-table-column prop="skillName" label="薄弱技能" />
            <el-table-column prop="count" label="薄弱人数" width="120" align="center" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="部门完成率" name="dept">
          <el-table :data="depts || []" border size="small">
            <el-table-column prop="deptName" label="部门" />
            <el-table-column prop="empCount" label="员工数" width="100" align="center" />
            <el-table-column prop="studyUsers" label="参与学习人数" width="130" align="center" />
            <el-table-column label="学习参与率" width="180">
              <template #default="{ row }"><el-progress :percentage="row.studyRate" :stroke-width="10" /></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="员工技能排名" name="rank">
          <el-table :data="ranking || []" border size="small">
            <el-table-column type="index" label="名次" width="70" />
            <el-table-column prop="userName" label="员工" />
            <el-table-column prop="deptName" label="部门" />
            <el-table-column prop="skillCount" label="技能数" width="90" align="center" />
            <el-table-column label="平均技能得分" width="180">
              <template #default="{ row }">
                <el-progress :percentage="row.avgScore" :color="row.avgScore >= 80 ? '#10b981' : '#f59e0b'" :stroke-width="10" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { statsOverview, statsStudy, statsExam, statsSkill, statsDept, statsRanking } from '@/api'

const loading = ref(false)
const cards = ref([]), study = ref({}), exam = ref({}), skill = ref({}), depts = ref([]), ranking = ref([])
const tab = ref('study')

onMounted(async () => {
  loading.value = true
  const [ov, st, ex, sk, dp, rk] = await Promise.all([
    statsOverview(), statsStudy(), statsExam(), statsSkill(), statsDept(), statsRanking()
  ])
  cards.value = [
    { label: '企业员工', value: ov.userCount },
    { label: '培训课程', value: ov.courseCount },
    { label: '在考考试', value: ov.examCount },
    { label: '知识文档', value: ov.docCount },
    { label: '参与学习率', value: ov.studyRate + '%' },
    { label: '考试平均分', value: ov.avgScore },
    { label: '考试通过率', value: ov.passRate + '%' },
    { label: '薄弱技能项', value: ov.weakSkillCount }
  ]
  study.value = st
  exam.value = ex
  skill.value = sk
  depts.value = dp
  ranking.value = rk
  loading.value = false
})
</script>

<style scoped>
.cards { margin-bottom: 16px; }
.cards .el-col { margin-bottom: 12px; }
.card-num { font-size: 24px; font-weight: 700; color: #1e3a8a; }
.card-label { color: #6b7280; font-size: 13px; margin-top: 4px; }
.stat-line { margin-top: 12px; color: #6b7280; }
</style>
