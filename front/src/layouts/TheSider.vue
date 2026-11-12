<!-- ============================================================
     TheSider.vue —— 侧边导航（可折叠），菜单项来自 data/menus.js。
     ============================================================ -->
<template>
  <div :class="['sider', { collapsed }]">
    <div v-for="m in menuItems" :key="m.key"
         :class="['nav-item', { active: isActive(m.to) }]"
         :title="m.label"
         @click="router.push(m.to)">
      <el-icon class="nav-icon" :size="20"><component :is="resolveIcon(m.icon)" /></el-icon>
      <span v-show="!collapsed" class="nav-label">{{ m.label }}</span>
    </div>

    <div class="sider-toggle" :title="collapsed ? '展开菜单' : '收起菜单'" @click="collapsed = !collapsed">
      <el-icon class="nav-icon" :size="20"><component :is="resolveIcon(collapsed ? 'Expand' : 'Fold')" /></el-icon>
      <span v-show="!collapsed" class="nav-label">收起菜单</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { menus } from '@/data/menus'
import { resolveIcon } from '@/utils/icons'

const collapsed = ref(false)          // 菜单折叠状态
const route = useRoute()
const router = useRouter()

const menuItems = computed(() => menus.map(m => ({ ...m, to: `/${m.key}` })))

/** 子路径（如 /app/create）也归属对应菜单，避免二级页把高亮丢掉 */
function isActive(to) {
  return route.path === to || route.path.startsWith(`${to}/`)
}
</script>

<style scoped>
.sider{width:224px;flex:0 0 auto;background:#fff;border-right:1px solid var(--line);
  padding:16px 12px;display:flex;flex-direction:column;
  transition:width .22s ease,padding .22s ease;overflow:hidden;}
.sider.collapsed{width:68px;padding:16px 10px;}

.nav-item{display:flex;align-items:center;gap:12px;padding:0 16px;height:46px;border-radius:var(--radius);
  cursor:pointer;color:#4B5A70;font-size:14.5px;font-weight:500;margin-bottom:6px;
  white-space:nowrap;transition:background .15s,color .15s;}
.nav-item:hover{background:var(--brand-soft-2);color:var(--brand-dark);}
.nav-item.active{background:var(--brand-soft);color:var(--brand-dark);font-weight:700;}
.sider.collapsed .nav-item{justify-content:center;padding:0;}

.nav-icon{flex:0 0 auto;}
.nav-label{overflow:hidden;}

.sider-toggle{margin-top:auto;display:flex;align-items:center;gap:12px;padding:0 16px;height:46px;
  border-radius:var(--radius);cursor:pointer;color:var(--muted);font-size:13.5px;font-weight:500;
  white-space:nowrap;transition:background .15s,color .15s;}
.sider-toggle:hover{background:#F2F6FC;color:#4B5A70;}
.sider.collapsed .sider-toggle{justify-content:center;padding:0;}
</style>
