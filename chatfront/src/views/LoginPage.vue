<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { login } from '../api/login'
import NotFound from '../components/NotFound.vue'

const props = defineProps({
  // 从路由 /login/:appkey 传入
  appkey: {
    type: String,
    default: '',
  },
})

const router = useRouter()

// 无 appkey 时展示 404
const notFound = ref(!props.appkey)

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)

async function handleLogin() {
  if (!form.username.trim() || !form.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    // 开放登录接口：POST /open/loginedwindow/login，入参 appKey/username/password
    await login({
      appKey: props.appkey,
      username: form.username.trim(),
      password: form.password,
    })
    router.replace('/loginedchat')
  } catch (err) {
    // 登录失败：弹出接口返回文案（ProblemDetail.detail），无文案时兜底
    const msg = err?.message || '登录失败，请稍后重试'
    ElMessage.error(msg.startsWith('HTTP 401') ? '用户名或密码错误' : msg)
    console.error('[login]', err)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <!-- 无 appkey：404 -->
  <NotFound v-if="notFound" />

  <div v-else class="login-page">
    <div class="login-card">
      <div class="login-header">
        <el-icon class="login-logo"><User /></el-icon>
        <div class="login-title">基于AI的确定性可拓展智能问答系统</div>
        <div class="login-subtitle">请登录后继续</div>
      </div>

      <el-form class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            size="large"
            placeholder="用户名"
            :prefix-icon="User"
            autocomplete="username"
            @keydown.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            autocomplete="current-password"
            @keydown.enter="handleLogin"
          />
        </el-form-item>

        <el-button
          class="login-btn"
          type="primary"
          size="large"
          :loading="loading"
          @click="handleLogin"
        >
          登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  padding: 40px 32px 32px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid #e4e7ed;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 28px;
}

.login-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  font-size: 26px;
  color: #ffffff;
  background: #409eff;
  margin-bottom: 14px;
}

.login-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.login-subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.login-form {
  margin-top: 4px;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
}
</style>
