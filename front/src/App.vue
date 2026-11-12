<!-- ============================================================
     App.vue —— 登录页只渲染表单；其余页面为「顶栏 + 侧边导航 + 路由出口」。
     （模板为多根节点，让 .topbar / .body 直接成为 #app 的弹性子项）
     ============================================================ -->
<template>
  <router-view v-if="isPublicPage" />

  <template v-else>
    <TheTopbar />

    <div class="body">
      <TheSider />
      <div class="main">
        <router-view />
      </div>
    </div>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TheTopbar from '@/layouts/TheTopbar.vue'
import TheSider from '@/layouts/TheSider.vue'

const route = useRoute()

const isPublicPage = computed(() => route.meta.public === true)
</script>

<style scoped>
.body{flex:1;display:flex;min-height:0;}
.main{flex:1;min-width:0;padding:24px;overflow:auto;}
</style>
