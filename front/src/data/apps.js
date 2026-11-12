/* ============================================================
   apps.js —— 应用管理的常量定义。
   应用数据全部来自接口（GET /apps/listAll），此处只保留渠道枚举。
   ============================================================ */

/* ------------------------------------------------------------ 渠道定义 */

/**
 * 渠道分类：左侧分类树，右侧展示该分类下已有的接入记录。
 * 字段说明：
 *   key    渠道标识（与后端约定）
 *   label  展示名
 *   desc   一句话说明
 *   fields 该渠道需要填写的参数（不确定的细节先按常见字段占位）
 *   multi  是否允许接多个（第三方渠道通常可以，原生渠道一般一个）
 */
export const CHANNEL_GROUPS = [
  {
    key: 'native',
    label: '原生渠道',
    desc: '应用自身直接对外开放的入口',
    channels: [
      {
        key: 'api',
        label: 'API 访问',
        icon: 'Connection',
        desc: '由业务后端直接调用接口',
        multi: true,
        fields: [
          { key: 'name', label: '接入名称', placeholder: '如：订单服务' },
          { key: 'source', label: '来源标识', placeholder: '如 order-service' },
          { key: 'secret', label: '调用密钥', placeholder: '留空则由系统生成' },
        ],
      },
      {
        key: 'free_login',
        label: '免登陆窗口',
        icon: 'Monitor',
        desc: '打开页面即可对话，无需登录',
        multi: false,
        fields: [
          { key: 'name', label: '窗口名称', placeholder: '如：官网悬浮窗' },
          { key: 'entry_url', label: '入口地址', placeholder: 'https://example.com/chat' },
          { key: 'expire', label: '链接有效期', placeholder: '如 30 天，留空为长期' },
        ],
      },
      {
        key: 'account_login',
        label: '账号密码登录窗口',
        icon: 'User',
        desc: '需账号密码登录后才能对话',
        multi: false,
        fields: [
          { key: 'name', label: '窗口名称', placeholder: '如：员工工作台' },
          { key: 'entry_url', label: '入口地址', placeholder: 'https://example.com/portal' },
          { key: 'auth_url', label: '鉴权地址', placeholder: 'https://example.com/login' },
        ],
      },
    ],
  },
  {
    key: 'third',
    label: '第三方发布渠道',
    desc: '接入外部平台的机器人 / 公众号',
    channels: [
      {
        key: 'wechat_personal',
        label: '微信个人号',
        icon: 'ChatDotRound',
        desc: '通过个人微信号接入',
        multi: true,
        fields: [
          { key: 'name', label: '接入名称', placeholder: '如：客服小助手' },
          { key: 'account', label: '微信号', placeholder: '登录用的微信号' },
          { key: 'callback', label: '回调地址', placeholder: 'https://example.com/callback' },
        ],
      },
      /* ----------------------------------------------------------
         以下 4 个渠道暂未开放，先在「发布渠道」Tab 隐藏入口。
         这里只注释、不删除；需要启用时去掉本块注释即可。
         ----------------------------------------------------------
      {
        key: 'feishu_bot',
        label: '飞书机器人',
        icon: 'Promotion',
        desc: '飞书群机器人 / 应用机器人',
        multi: true,
        fields: [
          { key: 'name', label: '接入名称', placeholder: '如：飞书客服群' },
          { key: 'app_id', label: 'App ID', placeholder: 'cli_ 开头' },
          { key: 'app_secret', label: 'App Secret', placeholder: '飞书应用密钥' },
        ],
      },
      {
        key: 'dingtalk_bot',
        label: '钉钉机器人',
        icon: 'Bell',
        desc: '钉钉群机器人 / 企业内部机器人',
        multi: true,
        fields: [
          { key: 'name', label: '接入名称', placeholder: '如：钉钉客服群' },
          { key: 'app_key', label: 'AppKey', placeholder: 'ding 开头' },
          { key: 'app_secret', label: 'AppSecret', placeholder: '钉钉应用密钥' },
        ],
      },
      {
        key: 'wecom_bot',
        label: '企业微信机器人',
        icon: 'Service',
        desc: '企微群机器人 / 自建应用',
        multi: true,
        fields: [
          { key: 'name', label: '接入名称', placeholder: '如：企微客服A' },
          { key: 'corp_id', label: '企业 ID', placeholder: 'ww 开头' },
          { key: 'secret', label: '应用 Secret', placeholder: '企微应用密钥' },
        ],
      },
      {
        key: 'wechat_mp',
        label: '微信公众号',
        icon: 'ChatLineRound',
        desc: '服务号 / 订阅号消息自动回复',
        multi: false,
        fields: [
          { key: 'name', label: '公众号名称', placeholder: '如：官方服务号' },
          { key: 'app_id', label: 'AppID', placeholder: 'wx 开头' },
          { key: 'app_secret', label: 'AppSecret', placeholder: '公众号密钥' },
        ],
      },
      ---------------------------------------------------------- */
    ],
  },
]

/** 拍平后的渠道列表，便于按 key 查找 */
export const ALL_CHANNELS = CHANNEL_GROUPS.flatMap(g => g.channels)

/** key -> 渠道定义 */
export function findChannel(key) {
  return ALL_CHANNELS.find(c => c.key === key)
}

/** key -> 中文名，用于展示 */
export function channelLabel(key) {
  const hit = findChannel(key)
  return hit ? hit.label : key
}
