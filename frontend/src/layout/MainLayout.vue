<template>
  <el-container class="layout">
    <!-- 墨绿侧栏 -->
    <el-aside width="232px" class="aside">
      <div class="brand">
        <svg class="brand-mark" viewBox="0 0 64 64" aria-hidden="true">
          <rect width="64" height="64" rx="15" fill="#123a2c"/>
          <g fill="none" stroke-linecap="round">
            <path d="M16 44c6-1 8-6 12-8 6-3 12-1 16-4 4-3 3-8 5-10" stroke="#2A7F8A" stroke-width="4"/>
            <path d="M13 49c8-1 11-7 16-10 7-4 14-3 19-7 5-4 4-10 7-13" stroke="#8EB5A7" stroke-width="4" opacity=".55"/>
            <path d="M19 39c5-1 7-5 10-7 5-2 10-1 13-4 3-3 2-7 4-9" stroke="#1C6B4F" stroke-width="4"/>
            <path d="M27 30c3-1 4-3 6-4 3-2 6-1 8-3 2-2 2-4 3-6" stroke="#E5A13C" stroke-width="4"/>
          </g>
          <circle cx="45" cy="16" r="4" fill="#E5A13C"/>
        </svg>
        <div class="brand-text">
          <div class="brand-title">技能练兵</div>
          <div class="brand-sub">智能培训 · 考核系统</div>
        </div>
      </div>

      <nav class="nav">
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <div class="nav-eyebrow">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click="go(item.path)"
          >
            <span class="tick" />
            <el-icon :size="17"><component :is="item.icon" /></el-icon>
            <span class="nav-label">{{ item.label }}</span>
          </div>
        </div>
      </nav>

      <div class="aside-foot">
        <div class="foot-line" />
        <div class="foot-text">岗位练兵 · 以考促学</div>
      </div>
    </el-aside>

    <el-container class="right">
      <!-- 头部 -->
      <el-header class="header">
        <div class="page-head">
          <div class="page-title">{{ $route.meta.title || '' }}</div>
          <div class="page-sub">技能画像 · 达标攀登</div>
        </div>

        <div class="header-right">
          <div class="date-pill">
            <el-icon :size="14"><Calendar /></el-icon>
            <span>{{ today }}</span>
          </div>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <span class="avatar">{{ store.user?.name?.charAt(0) }}</span>
              <span class="uname">{{ store.user?.name }}</span>
              <span class="role-tag">{{ roleLabel }}</span>
              <el-icon :size="12" class="caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'
})

const roleLabel = computed(() => {
  const map = { ADMIN: '管理员', TRAINER: '培训负责人', EMPLOYEE: '员工' }
  return map[store.roleCode] || ''
})

/* 菜单结构：同一路径的亮点在「考核」里成组 */
const manageGroups = [
  {
    label: '工作台',
    items: [{ path: '/home', icon: 'HomeFilled', label: '首页' }]
  },
  {
    label: '培训管理',
    items: [
      { path: '/dashboard', icon: 'DataLine', label: '数据看板' },
      { path: '/jobs', icon: 'Briefcase', label: '岗位技能' },
      { path: '/courses', icon: 'Reading', label: '课程管理' },
      { path: '/questions', icon: 'Document', label: '题库管理' },
      { path: '/exams', icon: 'EditPen', label: '考试管理' }
    ]
  },
  {
    label: '评估分析',
    items: [
      { path: '/stats', icon: 'TrendCharts', label: '统计分析' },
      { path: '/knowledge', icon: 'FolderOpened', label: '知识库' },
      { path: '/ai', icon: 'MagicStick', label: 'AI 智能助手' }
    ]
  },
  {
    label: '系统',
    items: [{ path: '/system', icon: 'Setting', label: '系统管理' }]
  }
]

const employeeGroups = [
  {
    label: '工作台',
    items: [{ path: '/home', icon: 'HomeFilled', label: '首页' }]
  },
  {
    label: '我的成长',
    items: [
      { path: '/profile', icon: 'User', label: '我的技能画像' },
      { path: '/ai', icon: 'MagicStick', label: 'AI 智能助手' }
    ]
  }
]

const groups = computed(() => store.canManage ? manageGroups : employeeGroups)

/* 子页面（学习/考试作答）高亮回父菜单 */
function isActive(path) {
  const p = route.path
  if (p === path) return true
  if (p.startsWith('/learn/') && path === '/courses') return true
  if ((p.startsWith('/exam-take/') || p.startsWith('/exam-result/')) && path === '/exams') return true
  if (p.startsWith('/profile/') && path === '/profile') return true
  return false
}

function go(path) {
  if (route.path !== path) router.push(path)
}

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
.aside {
  background: var(--ink);
  color: #D6E3DB;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}
.aside::before {
  /* 侧栏角落的等高线暗纹 */
  content: '';
  position: absolute; right: -90px; bottom: -80px;
  width: 320px; height: 320px;
  background-image: url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='320' height='320' viewBox='0 0 320 320'><g fill='none' stroke='%231C6B4F' stroke-width='1' opacity='0.28'><circle cx='250' cy='60' r='230'/><circle cx='250' cy='60' r='180'/><circle cx='250' cy='60' r='132'/><circle cx='250' cy='60' r='88'/><circle cx='250' cy='60' r='50'/><circle cx='250' cy='60' r='20'/></g></svg>");
  pointer-events: none;
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 20px 20px 18px;
  position: relative;
  z-index: 1;
}
.brand-mark { width: 42px; height: 42px; flex: none; }
.brand-title {
  font-family: var(--serif);
  font-size: 19px;
  font-weight: 700;
  color: #F2F7F4;
  letter-spacing: 0.08em;
}
.brand-sub {
  font-size: 10px;
  color: #7FA392;
  letter-spacing: 0.12em;
  margin-top: 2px;
}

.nav { flex: 1; overflow-y: auto; padding: 4px 12px 16px; position: relative; z-index: 1; }
.nav-eyebrow {
  font-family: var(--serif);
  font-size: 11px;
  color: #6E9280;
  letter-spacing: 0.2em;
  padding: 14px 10px 7px;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px 10px 13px;
  margin: 2px 0;
  border-radius: 9px;
  color: #B9CEC2;
  cursor: pointer;
  font-size: 14px;
  position: relative;
  transition: background-color .16s, color .16s;
}
.nav-item:hover { background: rgba(28,107,79,.18); color: #EAF3EE; }
.nav-item.active { background: var(--forest); color: #FFFFFF; font-weight: 600; }
.nav-item.active .tick {
  content: '';
  position: absolute; left: -12px; top: 50%; transform: translateY(-50%);
  width: 4px; height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--amber);
}
.nav-label { flex: 1; }
.nav-item .el-icon { color: #8FB5A3; }
.nav-item.active .el-icon { color: var(--amber); }

.aside-foot { padding: 16px 20px; position: relative; z-index: 1; }
.foot-line { height: 1px; background: rgba(139,178,158,.18); margin-bottom: 12px; }
.foot-text { font-size: 11px; color: #6E9280; letter-spacing: 0.14em; text-align: center; font-family: var(--serif); }

.right { background: var(--paper); }

.header {
  height: 64px;
  background: #FFFFFF;
  border-bottom: 1px solid var(--hairline);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}
.page-head { display: flex; flex-direction: column; gap: 2px; }
.page-title { font-family: var(--serif); font-size: 19px; font-weight: 700; color: var(--ink-text); letter-spacing: 0.04em; }
.page-sub { font-size: 11px; color: var(--muted); letter-spacing: 0.08em; }

.header-right { display: flex; align-items: center; gap: 16px; }
.date-pill {
  display: flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--muted);
  background: var(--paper);
  border: 1px solid var(--hairline);
  border-radius: 20px;
  padding: 5px 12px;
}
.user-chip {
  display: flex; align-items: center; gap: 8px;
  cursor: pointer;
  padding: 5px 10px 5px 5px;
  border-radius: 20px;
  border: 1px solid transparent;
  transition: border-color .16s, background-color .16s;
}
.user-chip:hover { border-color: var(--hairline); background: var(--paper); }
.avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--ink); color: var(--amber);
  font-family: var(--serif); font-weight: 700; font-size: 15px;
  display: flex; align-items: center; justify-content: center;
}
.uname { font-size: 14px; font-weight: 600; color: var(--ink-text); }
.role-tag {
  font-size: 11px;
  color: var(--pine);
  background: var(--sage);
  border: 1px solid #D4E4DA;
  border-radius: 20px;
  padding: 2px 8px;
}
.caret { color: var(--muted); }

.main { background: var(--paper); padding: 20px 24px; overflow-y: auto; }
</style>
