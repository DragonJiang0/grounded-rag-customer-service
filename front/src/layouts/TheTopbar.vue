<!-- ============================================================
     TheTopbar.vue —— 顶栏：Logo / 品牌名 / 引擎状态 / 当前用户与退出。
     用户下拉内提供「修改密码」与「退出登录」两项。
     ============================================================ -->
<template>
  <div class="topbar">
    <div class="logo"><el-icon :size="18"><Cpu /></el-icon></div>
    <div class="brand">
      <div class="brand-name">基于AI的确定性可拓展智能问答系统</div>
      <div class="brand-sub">Deterministic Intent Matching Assistant</div>
    </div>
    <div class="topbar-spacer"></div>

    <el-dropdown trigger="click" @command="onCommand">
      <div class="user-entry">
        <el-avatar :size="36" class="avatar">{{ avatarText }}</el-avatar>
        <span class="user-name">{{ currentUser }}</span>
        <el-icon class="user-arrow"><ArrowDown /></el-icon>
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item disabled>
            {{ currentUser }}
          </el-dropdown-item>
          <el-dropdown-item divided command="changePassword">
            <el-icon><Lock /></el-icon>修改密码
          </el-dropdown-item>
          <el-dropdown-item command="logout">
            <el-icon><SwitchButton /></el-icon>退出登录
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 修改密码：原密码 + 新密码 + 确认新密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="460px"
               :close-on-click-modal="false" @closed="resetPwdForm">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="PWD_RULES" label-width="96px"
               @submit.prevent>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password
                    autocomplete="current-password" placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password
                    autocomplete="new-password" maxlength="128"
                    placeholder="6-128 个字符" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password
                    autocomplete="new-password" maxlength="128"
                    placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button size="large" @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="pwdSaving" @click="submitPwd">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Cpu, ArrowDown, Lock, SwitchButton } from '@element-plus/icons-vue'
import { authApi } from '@/api/auth'
import { errorMessage } from '@/api/http'
import { useAuth } from '@/stores/useAuth'

const router = useRouter()
const { currentUser, logout } = useAuth()

const avatarText = currentUser.slice(0, 1)

/* ---------- 修改密码 ---------- */
const pwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdFormRef = ref(null)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

/** 新密码需二次确认，避免输错后无法登录 */
function validateConfirm(rule, value, callback) {
  if (!value) {
    return callback(new Error('请再次输入新密码'))
  }
  if (value !== pwdForm.newPassword) {
    return callback(new Error('两次输入的新密码不一致'))
  }
  callback()
}

const PWD_RULES = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 128, message: '新密码长度为 6-128 个字符', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }],
}

async function submitPwd() {
  // 校验不通过时 focus 尚未结束，用 catch 忽略
  try {
    await pwdFormRef.value?.validate()
  } catch {
    return
  }

  pwdSaving.value = true
  try {
    // 成功无响应体，看 HTTP 200 即可
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码已修改')
    pwdVisible.value = false
  } catch (e) {
    // 400 原密码错误 / 新旧密码相同、404 用户不存在、401 未登录，detail 直接展示
    ElMessage.error(errorMessage(e, '修改密码失败'))
  } finally {
    pwdSaving.value = false
  }
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate()
}

async function onCommand(command) {
  if (command === 'changePassword') {
    pwdVisible.value = true
    return
  }
  if (command !== 'logout') {
    return
  }
  await logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.topbar{height:64px;flex:0 0 64px;background:#fff;border-bottom:1px solid var(--line);
  display:flex;align-items:center;padding:0 22px;gap:12px;z-index:10;box-shadow:var(--shadow);}
.logo{width:38px;height:38px;border-radius:var(--radius);color:#fff;
  display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,#6BA6FF,#3E8BFF);box-shadow:0 3px 10px rgba(62,139,255,.32);}
.brand-name{font-weight:700;font-size:16px;letter-spacing:.2px;}
.brand-sub{color:var(--muted);font-size:12px;margin-top:1px;}
.topbar-spacer{flex:1;}
.env-tag{font-size:12.5px;color:var(--ok);background:#EAF8F1;border:1px solid #BFE7D3;
  padding:6px 14px;border-radius:var(--radius);display:flex;align-items:center;gap:7px;font-weight:500;}
.env-dot{width:7px;height:7px;border-radius:50%;background:var(--ok);
  box-shadow:0 0 0 3px rgba(47,163,107,.18);}

.user-entry{display:flex;align-items:center;gap:9px;cursor:pointer;padding:4px 8px;border-radius:var(--radius);
  transition:background .15s;outline:none;}
.user-entry:hover{background:var(--brand-soft-2);}
.avatar{background:var(--brand-soft);color:var(--brand);font-weight:700;flex:0 0 auto;}
.user-name{font-size:14px;font-weight:600;color:var(--text);}
.user-arrow{color:var(--muted);font-size:13px;}
</style>
