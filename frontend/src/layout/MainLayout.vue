<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon><School /></el-icon>
        <span>智能培训考核系统</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1d2939" text-color="#cbd5e1"
               active-text-color="#fff" class="menu">
        <el-menu-item index="/home"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>

        <template v-if="store.canManage">
          <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><span>数据看板</span></el-menu-item>
          <el-menu-item index="/jobs"><el-icon><Briefcase /></el-icon><span>岗位技能</span></el-menu-item>
          <el-menu-item index="/courses"><el-icon><Reading /></el-icon><span>课程管理</span></el-menu-item>
          <el-menu-item index="/questions"><el-icon><Document /></el-icon><span>题库管理</span></el-menu-item>
          <el-menu-item index="/exams"><el-icon><EditPen /></el-icon><span>考试管理</span></el-menu-item>
          <el-menu-item index="/stats"><el-icon><TrendCharts /></el-icon><span>统计分析</span></el-menu-item>
          <el-menu-item index="/knowledge"><el-icon><FolderOpened /></el-icon><span>知识库</span></el-menu-item>
          <el-menu-item index="/ai"><el-icon><MagicStick /></el-icon><span>AI 智能助手</span></el-menu-item>
          <el-menu-item v-if="store.isAdmin" index="/system"><el-icon><Setting /></el-icon><span>系统管理</span></el-menu-item>
        </template>

        <template v-else>
          <el-menu-item index="/profile"><el-icon><User /></el-icon><span>我的技能画像</span></el-menu-item>
          <el-menu-item index="/ai"><el-icon><MagicStick /></el-icon><span>AI 智能助手</span></el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title">{{ $route.meta.title || '' }}</div>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="32">{{ store.user?.name?.charAt(0) }}</el-avatar>
            <span class="uname">{{ store.user?.name }}（{{ store.user?.roleName }}）</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

function onCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #1d2939; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #fff; font-weight: 600; }
.menu { border-right: none; }
.header { background: #fff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.page-title { font-size: 16px; font-weight: 600; color: #1f2937; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #374151; }
.uname { font-size: 14px; }
.main { background: #f3f4f6; }
</style>
