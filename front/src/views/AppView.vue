<!-- ============================================================
    AppView.vue —— 应用管理。
    卡片网格：第一格是「创建应用」，其后为 GET /apps/listAll 返回的应用。
    发布渠道本期后端固定返回空数组，卡片上暂只展示占位。
    ============================================================ -->
<template>
  <PageHead title="应用管理" tip="一个应用可绑定多个语料分组，并按渠道对外发布。" />

  <div v-loading="loading" class="card">
    <div class="app-grid">
      <!-- 创建应用：大号 + 卡片 -->
      <div class="app-card app-card--create" @click="createApp">
        <el-icon class="create-icon" :size="46"><Plus /></el-icon>
        <span class="create-text">创建应用</span>
      </div>

      <!-- 已创建的应用：整卡可点，进入编辑页 -->
      <div v-for="a in apps" :key="a.id" class="app-card app-card--item" @click="editApp(a)">
        <div class="app-head">
          <div class="app-avatar">{{ (a.name || '?').slice(0, 1) }}</div>
          <div class="app-title-box">
            <div class="app-name">{{ a.name }}</div>
            <div class="app-sub">{{ a.description || '—' }}</div>
          </div>
        </div>

        <div class="app-body">
          <div class="app-row">
            <span class="app-label">AppKey</span>
            <span class="app-value app-key" :title="a.appKey">{{ a.appKey }}</span>
          </div>
          <div class="app-row">
            <span class="app-label">语料分组</span>
            <span class="app-value" :title="groupNames(a.corpusGroups).join('、')">
              <template v-if="a.corpusGroups?.length">
                {{ groupNames(a.corpusGroups).join('、') }}
              </template>
              <span v-else class="is-off">未绑定</span>
            </span>
          </div>
          <div class="app-row">
            <span class="app-label">发布渠道</span>
            <span class="app-value">
              <template v-if="a.publishChannels?.length">
                {{ a.publishChannels.length }} 个
              </template>
              <span v-else class="is-off">未配置</span>
            </span>
          </div>
        </div>

        <div class="app-foot">
          <span class="app-time">{{ a.updatedAt }}</span>
          <div class="app-actions">
            <el-button text type="danger" size="small" @click.stop="removeApp(a)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <EmptyHint v-if="!apps.length && !loading" :icon="Plus" text="还没有应用，点左侧开始创建" />
  </div>

  <!-- 删除确认：需手动输入应用名 -->
  <el-dialog :model-value="!!removingApp" title="删除应用" width="460px"
             :close-on-click-modal="false" @update:model-value="cancelRemove">
    <template v-if="removingApp">
      <p class="del-line delete-warn">
        删除后该应用与其语料分组绑定会一并清除，且不可恢复。
      </p>
      <p class="del-line">
        请输入应用名称 <b class="del-name">{{ removingApp.name }}</b> 以确认：
      </p>
      <el-input v-model="confirmName" size="large" clearable
                :placeholder="removingApp.name"
                @keyup.enter="canConfirmRemove && confirmRemove()" />
    </template>

    <template #footer>
      <el-button size="large" @click="cancelRemove">取消</el-button>
      <el-button type="danger" size="large" :disabled="!canConfirmRemove"
                 :loading="removing" @click="confirmRemove">
        确认删除
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import PageHead from '@/components/PageHead.vue'
import EmptyHint from '@/components/EmptyHint.vue'
import { appApi } from '@/api/app'
import { errorMessage } from '@/api/http'

const router = useRouter()
const apps = ref([])
const loading = ref(false)

/** 接口返回的 corpusGroups 已是对象数组，这里只取名称拼接展示 */
function groupNames(groups) {
  return (groups || []).map(g => g.name).filter(Boolean)
}

async function load() {
  loading.value = true
  try {
    apps.value = (await appApi.listAll()) || []
  } catch (e) {
    // 401 由拦截器统一跳登录，其余错误取 detail 展示
    ElMessage.error(errorMessage(e, '加载应用列表失败'))
  } finally {
    loading.value = false
  }
}

function createApp() {
  router.push('/app/create')
}

function editApp(a) {
  router.push(`/app/edit/${a.id}`)
}

/* ---------- 删除应用（连带其分组绑定） ---------- */
/** 待删除的应用，非空时展示确认弹窗 */
const removingApp = ref(null)
const confirmName = ref('')
const removing = ref(false)

/** 输入的名称必须与应用名完全一致才能删除 */
const canConfirmRemove = computed(
  () => !!removingApp.value && confirmName.value.trim() === removingApp.value.name,
)

function removeApp(a) {
  removingApp.value = a
  confirmName.value = ''
}

function cancelRemove() {
  removingApp.value = null
  confirmName.value = ''
}

async function confirmRemove() {
  if (!canConfirmRemove.value) {
    return
  }

  removing.value = true
  try {
    // /apps/delete/{id} 成功返回空响应体，不做任何解析
    await appApi.remove(removingApp.value.id)
    ElMessage.success('应用已删除')
    cancelRemove()
    await load()
  } catch (e) {
    // 应用不存在时后端返回 500，原因在 detail 里
    ElMessage.error(errorMessage(e, '删除应用失败'))
  } finally {
    removing.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 卡片网格：自适应列宽，最小 280px */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.app-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--brand-soft-2);
  transition: border-color .15s, background .15s, box-shadow .15s, transform .15s;
}

/* ---------- 创建应用 ---------- */
.app-card--create {
  min-height: 224px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  border: 1.5px dashed #BBD4F7;
  cursor: pointer;
  color: var(--brand);
}
.app-card--create:hover {
  border-color: var(--brand);
  background: var(--brand-soft);
  box-shadow: 0 4px 16px rgba(62, 139, 255, .14);
  transform: translateY(-2px);
}
.create-icon {
  /* 圆形底衬，突出大号 + 号 */
  width: 76px;
  height: 76px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #fff;
  border: 1.5px dashed #BBD4F7;
  transition: border-color .15s, background .15s;
}
.app-card--create:hover .create-icon {
  border-color: var(--brand);
  background: var(--brand-soft);
}
.create-text {
  font-size: 15px;
  font-weight: 600;
}

/* ---------- 应用卡片 ---------- */
.app-card--item {
  display: flex;
  flex-direction: column;
  padding: 16px;
  /* 整卡可点，进入编辑页 */
  cursor: pointer;
}
.app-card--item:hover {
  border-color: #BCD6F7;
  background: #F0F7FF;
  box-shadow: var(--shadow);
  transform: translateY(-2px);
}

.app-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.app-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: var(--brand-soft);
  color: var(--brand-dark);
  font-size: 16px;
  font-weight: 700;
}
.app-title-box {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.app-name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-sub {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--muted);
}

/* 信息区 */
.app-body {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  gap: 8px;
  /* 占满中间空间，让底部操作区对齐卡片底边 */
  flex: 1;
}
.app-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
}
.app-label {
  flex: 0 0 62px;
  color: var(--muted);
}
.app-value {
  min-width: 0;
  color: #4B5A70;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.app-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: #4B5A70;
}
.is-off {
  color: var(--muted);
  font-weight: 600;
}

/* 底部：更新时间 + 操作 */
.app-foot {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid var(--line);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.app-time {
  color: var(--muted);
  font-size: 12px;
}
.app-actions {
  display: flex;
  gap: 2px;
  flex: 0 0 auto;
}

/* ---------- 删除确认弹窗 ---------- */
.del-line {
  margin: 0 0 12px;
  font-size: 13.5px;
  line-height: 1.7;
  color: #4B5A70;
}
.delete-warn {
  color: var(--danger);
}
.del-name {
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--brand-soft-2);
  color: var(--text);
  font-weight: 700;
  word-break: break-all;
}
</style>
