<template>
  <div class="login-page">
    <!-- 左侧：练兵面板 -->
    <div class="brand-panel">
      <div class="contour-art" aria-hidden="true">
        <svg viewBox="0 0 300 300">
          <g fill="none" stroke-linecap="round">
            <path d="M40 250c60-8 78-52 120-70 62-27 130-10 170-34 42-26 34-78 60-100" stroke="#2A7F8A" stroke-width="2.5" opacity=".9"/>
            <path d="M22 268c80-10 100-62 152-84 76-32 158-13 204-40 52-31 42-92 72-118" stroke="#8EB5A7" stroke-width="2.5" opacity=".5"/>
            <path d="M70 232c52-7 68-44 104-59 53-23 111-9 146-29 36-22 29-66 52-85" stroke="#1C6B4F" stroke-width="2.5"/>
            <path d="M124 190c40-5 52-33 80-44 41-18 86-7 114-23 28-17 23-52 41-67" stroke="#1C6B4F" stroke-width="2" opacity=".8"/>
            <path d="M186 152c30-4 39-24 59-32 30-13 63-5 84-17 20-12 17-38 30-48" stroke="#E5A13C" stroke-width="2.5"/>
          </g>
          <circle cx="264" cy="55" r="6" fill="#E5A13C"/>
          <circle cx="264" cy="55" r="12" fill="none" stroke="#E5A13C" stroke-width="1.5" opacity=".6"/>
        </svg>
      </div>

      <div class="brand-head">
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
          <div class="brand-sub">企业岗位技能培训与智能考核系统</div>
        </div>
      </div>

      <h1 class="hero">让每项技能<br />都抵达<em>达标线</em></h1>
      <p class="hero-sub">岗位需求 → 技能体系 → 课程学习 → 在线考核 → 技能画像 → AI 建议</p>

      <div class="flow">
        <div v-for="(s, i) in steps" :key="s" class="step" :style="{ '--i': i }">
          <span class="step-no">{{ i + 1 }}</span>
          <span class="step-name">{{ s }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧：表单 -->
    <div class="form-side">
      <div class="login-box">
        <div class="box-title">
          <h2>登录系统</h2>
          <p>使用企业分配的账号进入培训与考核平台</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" size="large" @keyup.enter="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock" />
          </el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="onSubmit">登 录</el-button>
        </el-form>

        <div class="tips">
          <div class="tips-title">演示账号（密码均为 123456）</div>
          <div class="tips-row"><b>管理员</b> admin <span>·</span> <b>培训负责人</b> trainer <span>·</span> <b>员工</b> zhangsan</div>
        </div>
      </div>
      <div class="form-side-note">岗位练兵 · 以考促学 · 数据驱动技能成长</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const steps = ['岗位需求', '技能体系', '课程学习', '在线考核', '技能画像', 'AI 建议']

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  background: var(--paper);
}

/* —— 左练兵面板 —— */
.brand-panel {
  flex: 1 1 54%;
  max-width: 640px;
  background: var(--ink);
  color: #EAF3EE;
  padding: 52px 56px;
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.contour-art { position: absolute; right: -40px; bottom: -10px; width: 340px; height: 340px; opacity: .9; pointer-events: none; }

.brand-head { display: flex; align-items: center; gap: 13px; position: relative; z-index: 1; }
.brand-mark { width: 46px; height: 46px; flex: none; }
.brand-title { font-family: var(--serif); font-size: 22px; font-weight: 700; letter-spacing: 0.1em; }
.brand-sub { font-size: 11px; color: #7FA392; letter-spacing: 0.12em; margin-top: 2px; }

.hero {
  font-family: var(--serif);
  font-size: 46px;
  font-weight: 700;
  line-height: 1.35;
  margin: 46px 0 14px;
  color: #F2F7F4;
  position: relative; z-index: 1;
}
.hero em { font-style: normal; color: var(--amber); position: relative; }
.hero-sub { font-size: 13px; color: #8FA99B; letter-spacing: 0.06em; position: relative; z-index: 1; }

.flow {
  margin-top: 46px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 18px;
  position: relative; z-index: 1;
}
.step { display: flex; align-items: center; gap: 9px; }
.step-no {
  width: 26px; height: 26px; border-radius: 50%;
  border: 1px solid rgba(229,161,60,.55);
  color: var(--amber);
  font-family: var(--serif); font-weight: 700; font-size: 13px;
  display: flex; align-items: center; justify-content: center;
  flex: none;
}
.step-name { font-size: 13px; color: #CFDFD5; }

/* —— 右表单 —— */
.form-side {
  flex: 1 1 46%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--paper);
  position: relative;
}
.login-box {
  width: 400px;
  background: #FFFFFF;
  border: 1px solid var(--hairline);
  border-radius: 16px;
  padding: 38px 36px 30px;
  box-shadow: 0 10px 34px rgba(14, 36, 28, .08);
}
.box-title { margin-bottom: 26px; }
.box-title h2 { font-family: var(--serif); font-size: 24px; font-weight: 700; margin: 0 0 8px; color: var(--ink-text); }
.box-title p { margin: 0; font-size: 13px; color: var(--muted); }
.login-btn { width: 100%; font-weight: 600; letter-spacing: 0.3em; margin-top: 4px; }
.tips {
  margin-top: 22px;
  padding: 14px 16px;
  background: var(--paper);
  border: 1px dashed var(--hairline);
  border-radius: 10px;
}
.tips-title { font-size: 12px; color: var(--muted); margin-bottom: 6px; }
.tips-row { font-size: 12px; color: #4A5A52; }
.tips-row b { color: var(--pine); font-weight: 600; }
.tips-row span { margin: 0 6px; color: #C3D1C8; }
.form-side-note {
  margin-top: 22px;
  font-family: var(--serif);
  font-size: 12px;
  color: #9BA9A1;
  letter-spacing: 0.18em;
}

@media (max-width: 900px) {
  .brand-panel { display: none; }
  .form-side { flex: 1; }
}
</style>
