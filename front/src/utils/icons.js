/* ============================================================
   icons.js —— 动态图标的名称注册表。

   模板里写死的图标（<Search/> 之类）在各自组件内按需 import；
   只有「名称存在数据里、由 :is 动态渲染」的图标才在这里登记。
   ============================================================ */

import {
  // 侧边导航
  ChatLineRound, Collection, FolderOpened, Grid, Document, Setting, MagicStick,
  // 侧栏折叠
  Expand, Fold,
  // 应用发布渠道
  Connection, Monitor, User, ChatDotRound, Promotion, Bell, Service,
} from '@element-plus/icons-vue'

const iconMap = {
  ChatLineRound, Collection, FolderOpened, Grid, Document, Setting, MagicStick,
  Expand, Fold,
  Connection, Monitor, User, ChatDotRound, Promotion, Bell, Service,
}

export function resolveIcon(name) {
  return iconMap[name]
}
