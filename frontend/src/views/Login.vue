<template>
  <div class="login-page">
    <div class="login-box">
      <div class="login-title">
        <h2>企业岗位技能培训与智能考核系统</h2>
        <p>岗位需求 → 技能体系 → 课程学习 → 在线考核 → 技能画像</p>
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
      <div class="tips">演示账号：admin / trainer / zhangsan，密码均为 123456</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

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
.login-page { height: 100%; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%); }
.login-box { width: 420px; background: #fff; border-radius: 12px; padding: 40px; box-shadow: 0 20px 40px rgba(0,0,0,.2); }
.login-title { text-align: center; margin-bottom: 28px; }
.login-title h2 { margin: 0 0 8px; color: #1f2937; }
.login-title p { margin: 0; color: #6b7280; font-size: 13px; }
.login-btn { width: 100%; }
.tips { margin-top: 16px; font-size: 12px; color: #9ca3af; text-align: center; }
</style>
