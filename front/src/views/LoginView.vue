<!-- ============================================================
     LoginView.vue —— 登录页（后端 POST /auth/login 签发 JWT）
     ============================================================ -->
<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="logo"><el-icon :size="22"><Cpu /></el-icon></div>
        <div>
          <div class="brand-name">基于AI的确定性可拓展智能问答系统</div>
          <div class="brand-sub">Deterministic Intent Matching Assistant</div>
        </div>
      </div>

      <el-form label-position="top" size="large" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" class="form-control"
                    placeholder="请输入用户名" clearable autofocus
                    @keyup.enter="submit">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" size="large" class="form-control"
                    placeholder="请输入密码" show-password
                    @keyup.enter="submit">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
      </el-form>

      <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
        {{ loading ? '登录中…' : '登 录' }}
      </el-button>

      <p v-if="error" class="login-error">{{ error }}</p>

    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cpu, User, Lock } from '@element-plus/icons-vue'
import { errorMessage } from '@/api/http'
import { useAuth } from '@/stores/useAuth'

const router = useRouter()
const route = useRoute()
const { login } = useAuth()

const form = reactive({ username: '', password: '' })
const loading = ref(false)
const error = ref('')

async function submit() {
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await login(form.username, form.password)
    ElMessage.success('登录成功，欢迎使用')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e) {
    error.value = errorMessage(e, '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page{min-height:100vh;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#EAF3FF 0%,#F1F6FC 45%,#DCEAFF 100%);padding:24px;}

.login-card{width:100%;max-width:420px;background:#fff;border:1px solid var(--line);
  border-radius:var(--radius-lg);padding:34px 32px 26px;box-shadow:0 10px 40px rgba(46,102,180,.12);}

.login-brand{display:flex;align-items:center;gap:12px;margin-bottom:28px;}
.logo{width:46px;height:46px;flex:0 0 46px;border-radius:var(--radius);color:#fff;
  display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#6BA6FF,#3E8BFF);box-shadow:0 3px 10px rgba(62,139,255,.32);}
.brand-name{font-weight:700;font-size:15.5px;letter-spacing:.2px;}
.brand-sub{color:var(--muted);font-size:11.5px;margin-top:2px;}

.form-control{width:100%;}
.login-btn{width:100%;height:48px;font-size:16px;margin-top:4px;}
.login-error{color:var(--danger);font-size:13px;margin:12px 0 0;text-align:center;}

.login-tip{margin-top:22px;padding-top:16px;border-top:1px dashed var(--line);
  color:var(--muted);font-size:12.5px;display:flex;align-items:center;gap:8px;justify-content:center;}
.tip-label{font-weight:600;color:#3D4B60;}
.tip-sep{color:#C3D2E4;}
.login-tip code{background:var(--brand-soft);color:var(--brand-dark);
  padding:2px 8px;border-radius:var(--radius-sm);font-size:12.5px;font-weight:600;}
</style>
