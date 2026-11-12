/*
 Navicat Premium Dump SQL

 Source Server         : 本地MYSQL
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : 127.0.0.1:3306
 Source Schema         : server

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/11/2026 17:55:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for agent_config
-- ----------------------------
DROP TABLE IF EXISTS `agent_config`;
CREATE TABLE `agent_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键，固定为 1',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '助手名称',
  `avatar` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '头像文字',
  `welcome` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '欢迎语',
  `fallback` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '未命中兜底话术',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Agent 配置';

-- ----------------------------
-- Records of agent_config
-- ----------------------------
BEGIN;
INSERT INTO `agent_config` (`id`, `name`, `avatar`, `welcome`, `fallback`, `updated_at`) VALUES (1, '衣见客服', '衣', '您好，我是衣见旗舰店客服小衣。尺码、面料、颜色、库存、退换货、发货时效都可以问我；查订单或物流请直接把订单号、物流单号发给我。', '这个问题我暂时没有查到准确信息，已经为您转接人工客服（在线时间 9:00-21:00），请稍候。', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for agent_prompt
-- ----------------------------
DROP TABLE IF EXISTS `agent_prompt`;
CREATE TABLE `agent_prompt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_name` varchar(64) NOT NULL COMMENT 'Agent 名称（唯一标识）',
  `content` text COMMENT '提示词内容',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_name` (`agent_name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent 调试提示词';

-- ----------------------------
-- Records of agent_prompt
-- ----------------------------
BEGIN;
INSERT INTO `agent_prompt` (`id`, `agent_name`, `content`, `created_at`, `updated_at`) VALUES (1, 'OrderQueryAgent', '# 角色\n你是「衣见」服装旗舰店的订单客服，负责把系统查到的订单数据整理成一条可以直接发给顾客的消息。\n\n# 任务\n把 <查询结果> 里的原始字段整理成一条 Markdown 客服回复。\n\n# 输出要求\n1. 第一行加粗订单号，例如：**订单号：SO202611100001**\n2. 用「-」列表逐项展示有值的字段：下单时间、订单状态、实付金额、商品备注（颜色尺码）、更新时间\n3. 时间统一格式化为 yyyy年MM月dd日 HH:mm，空值写「暂无」\n4. 金额保留两位小数并带单位「元」\n5. 订单状态前加图标：✅ 已完成　🚚 已发货/备货中　⏳ 待支付/已支付\n6. 列表之后另起一行，写一句不超过 20 字的提示语，针对当前状态给出下一步动作\n7. 只使用 <查询结果> 中的字段，不得编造物流进度、退款进度等查询结果之外的信息\n8. 只输出消息正文，不要解释，不要输出 JSON\n\n# 示例格式\n**订单号：<查询结果中的订单号>**\n\n- 下单时间：<值>\n- 订单状态：<图标 + 状态>\n- 实付金额：<值> 元\n- 商品备注：<值>\n- 更新时间：<值>\n\n<一句提示语>', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `agent_prompt` (`id`, `agent_name`, `content`, `created_at`, `updated_at`) VALUES (2, 'LogisticsTrackAgent', '# 角色\n你是「衣见」服装旗舰店的物流客服，负责把系统查到的物流数据整理成一条可以直接发给顾客的消息。\n\n# 任务\n把 <查询结果> 里的原始字段整理成一条 Markdown 客服回复。\n\n# 输出要求\n1. 第一行加粗物流单号，例如：**物流单号：SF7712345678002**\n2. 用「-」列表逐项展示有值的字段：承运商、物流状态、当前位置、预计送达、关联订单号、更新时间\n3. 时间统一格式化为 yyyy年MM月dd日 HH:mm，空值写「待定」\n4. 物流状态前加图标：✅ 已签收　🚚 运输中/派送中　📦 已揽收\n5. 列表之后另起一行，写一句不超过 20 字的提示语；显示签收时提醒核对包裹，运输中时提示耐心等待\n6. 只使用 <查询结果> 中的字段，不得编造轨迹节点、派送员电话等查询结果之外的信息\n7. 只输出消息正文，不要解释，不要输出 JSON\n\n# 示例格式\n**物流单号：<查询结果中的物流单号>**\n\n- 承运商：<值>\n- 物流状态：<图标 + 状态>\n- 当前位置：<值>\n- 预计送达：<值>\n- 关联订单号：<值>\n\n<一句提示语>', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for app
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '应用 ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '应用名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '应用描述',
  `app_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调用凭证 AppKey，全局唯一',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '1 启用 / 0 停用',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  `multi_agent_enabled` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_name` (`name`),
  UNIQUE KEY `uk_app_app_key` (`app_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用';

-- ----------------------------
-- Records of app
-- ----------------------------
BEGIN;
INSERT INTO `app` (`id`, `name`, `description`, `app_key`, `enabled`, `created_at`, `updated_at`, `multi_agent_enabled`) VALUES (1, '衣见服装旗舰店', '服装类目演示：8 分类语料 + 订单查询 / 物流跟踪 Agent', 'ak_clothingdemo01', 1, '2026-11-10 10:00:00', '2026-11-10 11:13:10', 1);
COMMIT;

-- ----------------------------
-- Table structure for app_access_window
-- ----------------------------
DROP TABLE IF EXISTS `app_access_window`;
CREATE TABLE `app_access_window` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '所属应用 ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '免密窗口名称，同一应用内唯一，供管理端识别',
  `window_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '免密窗口唯一标识，全局唯一，用于拼接免密打开链接',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后使用时间，未使用过为 NULL',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_window_key` (`window_key`),
  UNIQUE KEY `uk_app_name` (`app_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用免密窗口';

-- ----------------------------
-- Records of app_access_window
-- ----------------------------
BEGIN;
INSERT INTO `app_access_window` (`id`, `app_id`, `name`, `window_key`, `last_used_at`, `created_at`, `updated_at`) VALUES (1, 1, '服装演示免密窗口', 'wk-clothing-demo-2026', '2026-11-10 16:16:36', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for app_agent
-- ----------------------------
DROP TABLE IF EXISTS `app_agent`;
CREATE TABLE `app_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用 ID',
  `agent_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Agent 名称，对应已注册的 Agent bean 名',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_agent` (`app_id`,`agent_name`),
  KEY `idx_agent_name` (`agent_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用-Agent关联';

-- ----------------------------
-- Records of app_agent
-- ----------------------------
BEGIN;
INSERT INTO `app_agent` (`id`, `app_id`, `agent_name`, `created_at`) VALUES (3, 1, 'OrderQueryAgent', '2026-11-10 11:13:10');
INSERT INTO `app_agent` (`id`, `app_id`, `agent_name`, `created_at`) VALUES (4, 1, 'LogisticsTrackAgent', '2026-11-10 11:13:10');
COMMIT;

-- ----------------------------
-- Table structure for app_api_access
-- ----------------------------
DROP TABLE IF EXISTS `app_api_access`;
CREATE TABLE `app_api_access` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '接入 ID',
  `app_id` bigint NOT NULL COMMENT '所属应用 ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接入名称，同一应用内唯一',
  `api_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key，全局唯一，后端生成',
  `expire_at` datetime DEFAULT NULL COMMENT '过期时间，NULL 表示长期有效',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后一次使用时间，未使用过为 NULL',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_name` (`app_id`,`name`),
  UNIQUE KEY `uk_api_key` (`api_key`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用 API 访问接入';

-- ----------------------------
-- Records of app_api_access
-- ----------------------------
BEGIN;
INSERT INTO `app_api_access` (`id`, `app_id`, `name`, `api_key`, `expire_at`, `last_used_at`, `created_at`, `updated_at`) VALUES (1, 1, '服装演示API', 'sk-clothingdemo202611100000000001', NULL, NULL, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for app_chat_options
-- ----------------------------
DROP TABLE IF EXISTS `app_chat_options`;
CREATE TABLE `app_chat_options` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用 ID',
  `temperature` decimal(3,2) DEFAULT NULL COMMENT '采样温度，NULL 表示不覆盖；建议 0.00~2.00，追求确定性取 0 或极小值',
  `top_p` decimal(3,2) DEFAULT NULL COMMENT 'nucleus 采样，NULL 表示不覆盖；取值 0.00~1.00',
  `max_output_tokens` int DEFAULT NULL COMMENT '最大输出 token 数，NULL 表示不覆盖',
  `memory_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否开启对话记忆：1-开启（默认），0-关闭',
  `memory_rounds` int NOT NULL DEFAULT '10' COMMENT '对话记忆保留的问答轮数，一轮=用户消息+模型回复；仅在开启记忆时生效',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='模型对话参数';

-- ----------------------------
-- Records of app_chat_options
-- ----------------------------
BEGIN;
INSERT INTO `app_chat_options` (`id`, `app_id`, `temperature`, `top_p`, `max_output_tokens`, `memory_enabled`, `memory_rounds`, `created_at`, `updated_at`) VALUES (1, 1, 0.30, NULL, 2048, 1, 5, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for app_corpus_group
-- ----------------------------
DROP TABLE IF EXISTS `app_corpus_group`;
CREATE TABLE `app_corpus_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '应用 ID',
  `group_id` bigint NOT NULL COMMENT '语料分组 ID',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_group` (`app_id`,`group_id`),
  KEY `idx_group_id` (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用-语料分组关联';

-- ----------------------------
-- Records of app_corpus_group
-- ----------------------------
BEGIN;
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (9, 1, 1, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (10, 1, 2, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (11, 1, 3, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (12, 1, 4, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (13, 1, 5, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (14, 1, 6, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (15, 1, 7, '2026-11-10 11:13:10');
INSERT INTO `app_corpus_group` (`id`, `app_id`, `group_id`, `created_at`) VALUES (16, 1, 8, '2026-11-10 11:13:10');
COMMIT;

-- ----------------------------
-- Table structure for app_password_window
-- ----------------------------
DROP TABLE IF EXISTS `app_password_window`;
CREATE TABLE `app_password_window` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_id` bigint NOT NULL COMMENT '所属应用 ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '窗口名称，同一应用内唯一，供管理端识别',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录账号，同一应用内唯一',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录密码，BCrypt 密文',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用 0 禁用，禁用后不允许登录',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '登录令牌，登录成功签发，退出或过期后清空',
  `token_expire_at` datetime DEFAULT NULL COMMENT '登录令牌过期时间，NULL 表示无有效登录态',
  `last_used_at` datetime DEFAULT NULL COMMENT '最后登录时间，未使用过为 NULL',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_name` (`app_id`,`name`),
  UNIQUE KEY `uk_app_username` (`app_id`,`username`),
  UNIQUE KEY `uk_app_password_window_token` (`token`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='应用账号密码窗口';

-- ----------------------------
-- Records of app_password_window
-- ----------------------------
BEGIN;
INSERT INTO `app_password_window` (`id`, `app_id`, `name`, `username`, `password`, `status`, `token`, `token_expire_at`, `last_used_at`, `created_at`, `updated_at`) VALUES (1, 1, '服装演示账号', 'cloth', '$2a$10$f3a4Q3Xuv71N6Cs3ILjOvuKqtdMSrpnTS7C3eQSEl/rMnm8pQqMfm', 1, NULL, NULL, NULL, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for app_prompt
-- ----------------------------
DROP TABLE IF EXISTS `app_prompt`;
CREATE TABLE `app_prompt` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint unsigned NOT NULL COMMENT '应用ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '提示词内容（含 {question}、{answer} 变量）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_app_id` (`app_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='提示词表';

-- ----------------------------
-- Records of app_prompt
-- ----------------------------
BEGIN;
INSERT INTO `app_prompt` (`id`, `app_id`, `content`, `created_at`, `updated_at`) VALUES (1, 1, '# 角色与目标\n你是「衣见」服装旗舰店的在线客服，名字叫小衣。语气亲切、专业、简洁，像真人客服一样直接解决问题。\n\n# 回答依据\n只能依据 <标准答案>（{answer}）作答，不得编造、推测或引入标准答案之外的信息，包括价格、库存、发货时效和售后政策。\n如果 <标准答案> 是「未找到任何答案」，或者与用户问题明显不相关，就明确告知「这个问题我暂时没有查到准确信息」，并引导用户换个问法或者转人工，不得自行给出结论。\n\n# 回答组织\n1. 先给一句直接结论，再分点补充关键信息，最多 4 点。\n2. 涉及尺码、时效、金额、政策条件时，数字必须原样保留，不得四舍五入或改写。\n3. 用户问题缺少判断所需的信息（如身高体重、订单号、商品款号）时，先追问一条最关键的信息，再给建议。\n4. 不要输出 Markdown 表格，用短段落和「-」列表，方便手机阅读。\n5. 不使用表情符号，不堆砌客套话。\n\n# 问题推荐\n回答结束后，从 <系统关键词> 中挑选 3 个与当前问题相关、用户可能继续追问的问题，格式固定为：\n\n您可能还想问：\n1. xxx\n2. xxx\n3. xxx\n\n推荐问题必须原样取自 <系统关键词>，不得改写或编造；不足 3 个时有多少列多少。\n\n# 本次输入\n<用户当前问题> {question} </用户当前问题>\n\n# 输出格式\n（回答正文）\n\n您可能还想问：\n1. xxx\n2. xxx\n3. xxx', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for bot_credential
-- ----------------------------
DROP TABLE IF EXISTS `bot_credential`;
CREATE TABLE `bot_credential` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bot_token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'Bearer 鉴权令牌（扫码确认后由 leader 写入；空串表示尚未登录）',
  `baseurl` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'ilink 基座地址',
  `ilink_bot_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码确认返回的 ilink_bot_id',
  `ilink_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码确认返回的 ilink_user_id',
  `scanner_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '扫码人（人工维护的展示名称）',
  `app_id` bigint DEFAULT NULL COMMENT '绑定的应用 ID：该机器人收到的消息按此应用触发 embabel 回答',
  `invalid` tinyint(1) NOT NULL DEFAULT '0' COMMENT '凭证是否无效：1 表示该 token 已失效（getupdates 异常时标记），启动/恢复时跳过',
  `updates_buf` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'get_updates 游标（本会话独立维护）',
  `updated_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最后写入者实例 ID',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ilink_bot_id` (`ilink_bot_id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='微信 Bot 登录凭证（全局单行）';

-- ----------------------------
-- Records of bot_credential
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for bot_leader
-- ----------------------------
DROP TABLE IF EXISTS `bot_leader`;
CREATE TABLE `bot_leader` (
  `id` tinyint NOT NULL DEFAULT '1' COMMENT '固定单行，值恒为 1',
  `instance_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '当前 leader 实例 ID（空串表示无人持有）',
  `expire_time` datetime NOT NULL COMMENT '租约到期时间，由 leader 心跳续租',
  `updates_buf` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'get_updates 游标，随 leader 交接',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Bot 监听主节点租约（单例选举）';

-- ----------------------------
-- Records of bot_leader
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for bot_message
-- ----------------------------
DROP TABLE IF EXISTS `bot_message`;
CREATE TABLE `bot_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dedupe_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等键：优先微信 message_id，缺失时用 用户+上下文+内容 的哈希',
  `message_id` bigint DEFAULT NULL COMMENT '微信侧消息 ID',
  `from_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '发送方用户 ID',
  `to_user_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收方（机器人）用户 ID',
  `message_type` tinyint DEFAULT NULL COMMENT '消息类型：1=用户消息',
  `message_state` tinyint DEFAULT NULL COMMENT '消息状态，透传 SDK 字段',
  `context_token` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '回复时必须回带的上下文令牌',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '消息文本内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '处理状态：0待处理 1处理中 2完成 3失败待重试',
  `owner` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '当前处理实例 ID',
  `retry_count` int NOT NULL DEFAULT '0' COMMENT '已重试次数',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '最近一次失败原因',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dedupe_key` (`dedupe_key`),
  KEY `idx_status_id` (`status`,`id`),
  KEY `idx_from_user` (`from_user_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=161 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Bot 入站消息与处理任务';

-- ----------------------------
-- Records of bot_message
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for chat_history
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `conversation_id` varchar(128) NOT NULL,
  `app_id` bigint DEFAULT NULL COMMENT '所属应用 ID',
  `title` varchar(255) DEFAULT NULL COMMENT '标题',
  `history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '聊天记录内容',
  `seq` int NOT NULL DEFAULT '1' COMMENT '同一会话内的分段序号：history 超长时拆成多行，1 为第一段，查询时按 seq 升序合并',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `channel` int NOT NULL COMMENT '0.调试  1.API访问，2.免登陆窗口 3.账号密码登录窗口 4.微信个人号 5.飞书机器人 6.钉钉机器人 7.企业微信机器人 8.微信公众号',
  `tag` varchar(64) DEFAULT NULL COMMENT '标签',
  `customer_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '用户侧是否已删除：0-未删除（默认），1-已删除（软删除，仅列表过滤，不物理删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_seq` (`conversation_id`,`seq`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天记录表';

-- ----------------------------
-- Records of chat_history
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for chat_memory
-- ----------------------------
DROP TABLE IF EXISTS `chat_memory`;
CREATE TABLE `chat_memory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `memory_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话记忆 ID（当前传入会话 Long id 的字符串）',
  `app_id` bigint DEFAULT NULL COMMENT '所属应用 ID，NULL 表示无应用归属（如调试会话）',
  `messages` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息列表 JSON（langchain4j ChatMessageSerializer 序列化结果）',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx` (`memory_id`,`app_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会话记忆';

-- ----------------------------
-- Records of chat_memory
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for corpus
-- ----------------------------
DROP TABLE IF EXISTS `corpus`;
CREATE TABLE `corpus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '语料名称',
  `answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '命中后返回的答案',
  `group_id` bigint DEFAULT NULL COMMENT '所属语料分组，NULL 表示未分组',
  `enabled` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_corpus_group` (`group_id`),
  KEY `idx_corpus_enabled` (`enabled`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语料';

-- ----------------------------
-- Records of corpus
-- ----------------------------
BEGIN;
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (1, '尺码表与测量参数', '商品详情页的「尺码表」在价格下方、颜色选择上方，点开可以看每个码的衣长、胸围、肩宽、袖长，单位是 cm。测量时请把衣服平铺、扣好纽扣再量，手工测量误差 1-2cm 属于正常范围。如果详情页没有该款尺码表，把商品链接或款号发给客服，我们帮您查。', 1, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (2, '按身高体重推荐尺码', '常规版型参考：身高 160cm、体重 90 斤左右选 S；165cm、105 斤左右选 M；170cm、120 斤左右选 L；175cm、140 斤左右选 XL；180cm、160 斤左右选 XXL。介于两个码之间，或者肩宽、胸围偏大，建议选大一码。修身款要在常规码基础上再加一码。', 1, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (3, '版型偏大偏小', '本店常规款是标准版型，码数正常，按平时穿的码选即可。宽松款（oversize）胸围比常规款大 6-8cm，想穿合身建议小一码；修身款胸围收 4cm 左右，建议大一码。每款详情页的「版型说明」都会标注，拿不准可以把身高、体重、三围发给客服帮您判断。', 1, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (4, '特殊体型选码', '肩宽或胸围偏大：以胸围数据为准选码，肩线略宽不影响穿。胯宽腿粗：选直筒或 A 字版型，避免紧身款。个子高（女生 175cm 以上、男生 185cm 以上）：重点看衣长和裤长，裤装可以选加长版。微胖体型建议深色加宽松版型，尺码按胸围加 4cm 余量选。', 1, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (5, '均码与男女同款', '均码一般适合女生身高 155-170cm、体重 90-125 斤，弹性面料可以放宽到 130 斤。男女同款按男码表选，女生建议小一码。情侣款按各自尺码分别下单即可，同款不同码不影响搭配。均码商品详情页会标注适穿范围，超出范围不建议购买。', 1, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (6, '面料成分查询', '成分在详情页「面料成分」和衣服内侧的水洗标上都有标注，例如 100% 棉、95% 棉加 5% 氨纶、65% 聚酯纤维加 35% 粘纤。纯棉亲肤透气但容易皱；加氨纶有弹性；聚酯纤维挺括耐穿。具体款式的成分以详情页为准，需要确认可以把款号发给客服。', 2, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (7, '透气性与亲肤度', '纯棉、麻、莫代尔透气性好，适合夏天和贴身穿；聚酯纤维排汗快，但闷热感会略高一点。本店贴身类商品都做过甲醛和 pH 值检测，敏感肌可以穿，首次穿着前建议先洗一遍。皮肤特别敏感的顾客建议选浅色纯棉款，减少染料接触。', 2, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (8, '起球掉色与缩水', '纯棉和羊毛混纺正常穿着不易起球，针织、毛呢类摩擦多了会有轻微起球，用去球器处理即可。深色（黑色、藏青、墨绿）首次洗涤建议单独冷水洗，出现少量浮色属于正常。面料做过预缩处理，正常洗涤缩水率在 3% 以内。', 2, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (9, '厚薄与适穿季节', '详情页的「厚薄指数」分薄款、常规、加厚三档：薄款适合 25℃ 以上，常规款适合 15-25℃，加厚款适合 10℃ 以下。夏季主推 180g 左右纯棉，秋冬外套在 320g 以上。具体适穿温度以详情页标注为准。', 2, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (10, '弹性、透光与垂感', '含氨纶 3% 以上的款式有明显弹力，活动不受限；不含氨纶的纯棉、雪纺基本无弹。白色、米白等浅色薄款在强光下会轻微透，建议搭配浅色内衣。垂感取决于克重，克重越高越挺括，雪纺和醋酸类垂感最好。', 2, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (11, '实物色差说明', '商品图是在专业灯光下拍摄的，不同手机和电脑屏幕显示也有差异，实物颜色可能比图片略深或略浅，属于正常色差范围。详情页放了自然光实拍图，可以重点看带「实拍」标签的图片。对颜色要求严格的顾客建议同时购买运费险。', 3, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (12, '可选颜色与色卡', '具体可选颜色以详情页色卡为准，页面显示的颜色都可以下单，售罄的颜色会置灰。目前主推黑色、米白、藏青、卡其、墨绿五个基础色。想要的颜色如果暂未上架，可以把色号发给客服，我们会记录需求反馈给商品部。', 3, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (13, '模特图与买家秀颜色', '模特图和主图展示的是同一个颜色，详情页每个颜色都有独立实拍。视频和直播因为灯光偏暖，颜色会比实物亮一点。想看某个颜色的买家秀实拍，告诉客服颜色，我们发给您参考。', 3, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (14, '显瘦与肤色搭配', '黑色、藏青、墨绿显瘦且百搭；米白、浅蓝提亮，但对面料厚度要求高；黄皮建议避开荧光色和正黄，选奶白、雾霾蓝、豆沙色更衬肤色。深色下装搭配浅色上装是最稳的组合，不确定可以发日常穿搭照片，客服帮您参考。', 3, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (15, '固色与深浅色分开洗', '深色首次洗涤请单独冷水手洗，加少量食盐有助于固色，不要长时间浸泡。深浅色一定要分开洗，避免串色。牛仔类掉色属于工艺特性，建议反面洗、反面晾。正常洗涤 10 次以内色牢度在 3-4 级，符合国家标准。', 3, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (16, '实时库存查询', '商品页显示的库存是实时库存，能选中的尺码就是有货，置灰表示当前缺货。库存每 5 分钟同步一次，大促期间可能有短暂延迟，最终以提交订单时能否支付为准。', 4, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (17, '补货时间与到货提醒', '常规款缺货后一般 7-15 天补货，爆款和基础色会优先安排。补货时间以商品页的「到货通知」为准。点击「到货提醒」后，补货上架会第一时间发消息通知您。超过 30 天仍未补货的款式，建议看看同系列的替代款。', 4, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (18, '断码处理', '断码表示该颜色、该尺码已经停产或本季不再生产，无法单独加做。可以看看同款的其他颜色是否有您的码，或者选择相似版型的替代款，客服可以帮您挑。断码款不退不换的说明会在详情页标注。', 4, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (19, '预售商品规则', '预售款按详情页的「预售发货时间」发货，一般是付款后 7-15 天，页面会写明最晚发货日。预售款支持 7 天无理由退货；定金在支付尾款后转为货款，未支付尾款时定金可以退。急用建议直接选现货款。', 4, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (20, '限购与库存锁定', '活动款一般限购 2-5 件，超出数量的订单会被系统拦截。拍下后库存为您保留 30 分钟，超时未支付会自动释放，所以购物车里的商品不锁库存，建议尽快付款。', 4, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (21, '洗涤方式', '水洗标是最终依据：标注「可机洗」的请装洗衣袋、选轻柔模式；标注「手洗」的请冷水轻柔搓洗，不要用力拧干；标注「干洗」的（羊毛、真丝、西装）请送干洗店。深浅色分开洗，拉链和纽扣扣好再洗。', 5, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (22, '水温与洗涤剂', '常规棉质 30℃ 以下冷水或温水洗，羊毛、真丝 30℃ 以下冷水洗。不要使用含氯漂白剂，选中性洗衣液。洗涤前把衣服反面、拉好拉链，浸泡时间不超过 15 分钟，长时间浸泡容易掉色和变形。', 5, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (23, '晾晒与烘干', '针织、羊毛、真丝建议平铺阴干，不要悬挂，避免拉长变形；棉质可以悬挂晾干，但避免正午暴晒。除标注「可烘干」的款式外，都不建议烘干，高温会让面料缩水。深色反面晾晒可以减少褪色。', 5, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (24, '熨烫温度', '棉麻 180-200℃、羊毛 140-160℃、真丝和化纤 110-130℃，建议垫一层薄布再熨。蒸汽挂烫适合外套和衬衫，针织不要用力拉扯。熨烫前确认面料干燥，停留时间不要过长，避免烫出亮痕。', 5, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (25, '羊毛真丝等特殊面料护理', '羊毛：冷水手洗或干洗，平铺阴干，不要拧；真丝：冷水手洗、中性洗涤剂、阴干、低温熨烫；羽绒服：中性洗涤剂机洗轻柔档，低温烘干并放网球拍打蓬松；针织：折叠收纳，不要悬挂。具体以水洗标为准。', 5, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (26, '7 天无理由退货', '支持签收后 7 天无理由退货，要求吊牌完整、未下水、不影响二次销售。贴身衣物（内衣、袜子）和定制款不支持无理由退货。退回时请把赠品和原包装一起寄回，缺少配件可能影响退款金额。', 6, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (27, '换尺码换颜色', '支持同款换尺码、换颜色，签收后 7 天内可申请，换货一次免运费，质量问题不限次数。流程是申请换货、寄回、仓库质检、重新发出，一般 3-5 天完成。目标尺码缺货时，建议先退货再重新下单。', 6, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (28, '退货运费与运费险', '非质量问题（不喜欢、拍错、尺码不合适）的退货运费由买家承担，本店默认赠送运费险，理赔金额以保险公司核定为准。质量问题（破损、错发、明显色差）运费由本店承担，凭快递单报销。上门取件首重一般 8-12 元。', 6, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (29, '退款到账时效', '仓库签收并质检通过后 1-3 个工作日发起退款，原路退回：微信、支付宝 1-3 天到账，银行卡 3-7 个工作日。可以在「我的订单-退款详情」查看进度。超过时效仍未到账，请联系客服核查。', 6, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (30, '退换货操作流程', '操作路径：我的订单，选择商品，申请售后，选退货退款，填写原因并提交。审核通过后页面会给出退货地址和退货单号填写入口。退货请用原包装或结实纸箱，寄出后务必回填快递单号，否则仓库无法认领。一单多件支持只退其中一件。', 6, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (31, '发货时效', '现货订单付款后 48 小时内发出，当日 16:00 前付款的尽量当天发出。预售款按详情页标注的预售时间发货。节假日和大型活动期间可能顺延 1-2 天，页面会提前公告。', 7, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (32, '快递公司与指定快递', '默认发顺丰、中通、圆通，系统按仓库和地区自动匹配，暂不支持指定快递。需要顺丰的订单可以在备注里说明，运费差额需自理。港澳台及海外地区暂不发货。', 7, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (33, '运费与包邮门槛', '全店满 199 元包邮，新疆、西藏、内蒙古、青海、宁夏满 299 元包邮，未满收取 10 元运费。不支持下单到付。多笔订单寄同一地址可以联系客服合并发货，合并后只收一次运费。', 7, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (34, '修改收货地址', '未发货前可以在「我的订单」里自助修改收货地址，也可以联系客服修改。已发货的订单无法修改地址，可以联系快递改派，产生的费用需自理。收货人电话填错也请尽快联系客服，避免派送失败被退回。', 7, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (35, '物流异常处理', '物流超过 48 小时不更新，或显示签收但实际未收到，请联系客服，我们会在 24 小时内向快递发起查询。确认丢件后按订单金额全额补发或退款。派送失败通常是电话不通或地址不详，更新信息后会重新派送。', 7, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (36, '支付方式', '支持微信、支付宝、银行卡和花呗，部分商品支持 3 期、6 期、12 期分期，以结算页显示为准，不支持货到付款。支付失败请先检查余额、限额和网络，多次失败可以更换支付方式或稍后重试。', 8, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (37, '发票开具', '支持开具电子普通发票：下单时在结算页填写抬头和税号，发货后 3 个工作日内发送到预留邮箱。企业增值税专用发票需要提供一般纳税人资料，联系客服单独开具。已完成订单可在 90 天内申请补开。', 8, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (38, '优惠券与活动规则', '优惠券在结算页的「优惠」处选择使用，单笔订单限用一张，不与部分特价活动叠加。券面会标注有效期和使用门槛，过期不补。会员价可以与优惠券叠加，最终以结算页实付金额为准。', 8, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (39, '质量问题处理', '收到商品如有破损、开线、明显污渍、错发漏发，请在签收 48 小时内拍照（整体加问题细节）联系客服，核实后优先补发或全额退款，运费由本店承担。轻微线头属于正常工艺，剪掉即可；有异味建议通风晾晒 24 小时。', 8, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus` (`id`, `name`, `answer`, `group_id`, `enabled`, `created_at`, `updated_at`) VALUES (40, '人工客服与投诉', '人工客服在线时间 9:00-21:00，直接回复「转人工」即可接入，也可以拨打售后电话 400-800-1234。非工作时间请留言，次日 10:00 前回复。投诉与建议可以发送到售后邮箱 service@yijian-demo.com，我们会在 24 小时内跟进。', 8, 1, '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for corpus_group
-- ----------------------------
DROP TABLE IF EXISTS `corpus_group`;
CREATE TABLE `corpus_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分组名称',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_corpus_group_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语料分组';

-- ----------------------------
-- Records of corpus_group
-- ----------------------------
BEGIN;
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (1, '尺码与版型', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (2, '材质与面料', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (3, '颜色与色差', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (4, '库存与补货', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (5, '洗涤与保养', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (6, '退换货政策', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (7, '发货与物流', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
INSERT INTO `corpus_group` (`id`, `name`, `created_at`, `updated_at`) VALUES (8, '支付发票与售后', '2026-11-10 10:00:00', '2026-11-10 10:00:00');
COMMIT;

-- ----------------------------
-- Table structure for corpus_question
-- ----------------------------
DROP TABLE IF EXISTS `corpus_question`;
CREATE TABLE `corpus_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `corpus_id` bigint NOT NULL COMMENT '所属语料 id',
  `group_id` bigint DEFAULT NULL COMMENT '冗余的所属分组 id，未分组为 NULL',
  `question` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '问题文本（替代原标准问）',
  PRIMARY KEY (`id`),
  KEY `idx_corpus_question_corpus_id` (`corpus_id`),
  KEY `idx_corpus_question_group_id` (`group_id`)
) ENGINE=InnoDB AUTO_INCREMENT=201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='语料问题（一条语料可有多条）';

-- ----------------------------
-- Records of corpus_question
-- ----------------------------
BEGIN;
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (1, 1, 1, '尺码表在哪看');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (2, 1, 1, '有没有尺码对照表');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (3, 1, 1, '尺码参数怎么看');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (4, 1, 1, 'S码对应多少厘米');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (5, 1, 1, '每个码的衣长胸围是多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (6, 2, 1, '我165身高100斤穿什么码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (7, 2, 1, '按身高体重推荐尺码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (8, 2, 1, '我该选M还是L');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (9, 2, 1, '100斤选哪个码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (10, 2, 1, '170身高120斤穿多大');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (11, 3, 1, '衣服偏大还是偏小');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (12, 3, 1, '是不是宽松版型');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (13, 3, 1, '码数正不正');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (14, 3, 1, '需要买大一码吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (15, 3, 1, '修身款要不要买大一点');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (16, 4, 1, '肩宽怎么选码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (17, 4, 1, '胸围大选什么码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (18, 4, 1, '胯宽穿什么版型');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (19, 4, 1, '个子高穿会不会短');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (20, 4, 1, '微胖穿哪个码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (21, 5, 1, '均码适合多高多重');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (22, 5, 1, '均码是不是都能穿');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (23, 5, 1, '男女同款怎么选码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (24, 5, 1, '情侣款尺码一样吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (25, 5, 1, '女款和男款码数差多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (26, 6, 2, '什么面料的');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (27, 6, 2, '材质是什么');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (28, 6, 2, '含棉量多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (29, 6, 2, '是不是纯棉的');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (30, 6, 2, '成分表在哪里看');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (31, 7, 2, '夏天穿闷不闷');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (32, 7, 2, '透气吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (33, 7, 2, '贴身穿扎不扎');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (34, 7, 2, '敏感肌能穿吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (35, 7, 2, '会不会闷汗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (36, 8, 2, '会起球吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (37, 8, 2, '洗了会不会掉色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (38, 8, 2, '穿久了会不会变形');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (39, 8, 2, '深色会掉色吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (40, 8, 2, '针织容易起球吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (41, 9, 2, '厚不厚');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (42, 9, 2, '适合什么季节穿');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (43, 9, 2, '冬天能穿吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (44, 9, 2, '是薄款还是加厚');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (45, 9, 2, '夏天穿热不热');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (46, 10, 2, '有弹性吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (47, 10, 2, '面料会不会透');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (48, 10, 2, '白色透不透');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (49, 10, 2, '垂感怎么样');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (50, 10, 2, '面料会不会很硬');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (51, 11, 3, '实物和图片颜色一样吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (52, 11, 3, '会有色差吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (53, 11, 3, '颜色准不准');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (54, 11, 3, '实物颜色偏深吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (55, 11, 3, '图片是不是修过');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (56, 12, 3, '有几个颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (57, 12, 3, '有其他颜色吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (58, 12, 3, '有没有黑色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (59, 12, 3, '藏青色有吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (60, 12, 3, '米白色还有吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (61, 13, 3, '模特穿的是什么颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (62, 13, 3, '主图是哪个颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (63, 13, 3, '买家秀是哪个颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (64, 13, 3, '视频里那件是什么颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (65, 13, 3, '拍摄灯光会影响颜色吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (66, 14, 3, '哪个颜色更百搭');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (67, 14, 3, '黑色显瘦吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (68, 14, 3, '黄皮适合什么颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (69, 14, 3, '白色显不显黑');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (70, 14, 3, '肤色偏暗选哪个颜色');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (71, 15, 3, '颜色牢不牢');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (72, 15, 3, '洗几次会发白吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (73, 15, 3, '深色会染到别的衣服吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (74, 15, 3, '牛仔裤会掉色吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (75, 15, 3, '要和别的衣服分开洗吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (76, 16, 4, '还有货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (77, 16, 4, '这个码有库存吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (78, 16, 4, 'L码还有吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (79, 16, 4, '显示缺货是不是卖完了');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (80, 16, 4, '库存什么时候更新');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (81, 17, 4, '什么时候补货');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (82, 17, 4, '缺货的码多久到');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (83, 17, 4, '会补货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (84, 17, 4, '补货能通知我吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (85, 17, 4, '到货提醒怎么设置');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (86, 18, 4, '断码了怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (87, 18, 4, '没有我的码了');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (88, 18, 4, 'XXL卖完了吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (89, 18, 4, '能不能加做一码');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (90, 18, 4, '同款别的颜色有我的码吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (91, 19, 4, '预售什么时候发货');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (92, 19, 4, '预售款要等多久');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (93, 19, 4, '预售和现货有什么区别');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (94, 19, 4, '预售能退吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (95, 19, 4, '预售定金能退吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (96, 20, 4, '限购几件');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (97, 20, 4, '下单会锁库存吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (98, 20, 4, '购物车里的会没货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (99, 20, 4, '拍下没付款会占库存吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (100, 20, 4, '活动款库存多吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (101, 21, 5, '可以机洗吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (102, 21, 5, '怎么洗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (103, 21, 5, '要手洗吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (104, 21, 5, '能不能干洗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (105, 21, 5, '洗衣机用什么模式');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (106, 22, 5, '多少度水洗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (107, 22, 5, '能用漂白剂吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (108, 22, 5, '用什么洗衣液');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (109, 22, 5, '可以泡多久');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (110, 22, 5, '洗之前要翻面吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (111, 23, 5, '可以烘干吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (112, 23, 5, '能暴晒吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (113, 23, 5, '怎么晾不变形');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (114, 23, 5, '要平铺晾干吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (115, 23, 5, '阴干还是晒干');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (116, 24, 5, '能熨吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (117, 24, 5, '熨斗温度多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (118, 24, 5, '可以蒸汽熨吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (119, 24, 5, '皱了怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (120, 24, 5, '能挂烫吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (121, 25, 5, '羊毛怎么洗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (122, 25, 5, '真丝能水洗吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (123, 25, 5, '羽绒服怎么保养');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (124, 25, 5, '针织会拉长吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (125, 25, 5, '收纳要注意什么');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (126, 26, 6, '支持七天无理由吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (127, 26, 6, '可以退货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (128, 26, 6, '不想要了能退吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (129, 26, 6, '吊牌剪了还能退吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (130, 26, 6, '退货有什么条件');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (131, 27, 6, '可以换尺码吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (132, 27, 6, '换颜色怎么操作');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (133, 27, 6, '换货要多久');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (134, 27, 6, '换货运费谁出');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (135, 27, 6, '可以换同款其他码吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (136, 28, 6, '退货运费谁承担');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (137, 28, 6, '有运费险吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (138, 28, 6, '退货要自己付邮费吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (139, 28, 6, '质量问题运费怎么算');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (140, 28, 6, '上门取件收费吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (141, 29, 6, '退款多久到账');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (142, 29, 6, '退回后几天退款');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (143, 29, 6, '退款退到哪里');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (144, 29, 6, '能退到支付宝吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (145, 29, 6, '退款失败怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (146, 30, 6, '怎么申请退货');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (147, 30, 6, '退货地址在哪');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (148, 30, 6, '退货要原包装吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (149, 30, 6, '退货单号怎么填');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (150, 30, 6, '能只退一件吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (151, 31, 7, '多久发货');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (152, 31, 7, '什么时候发货');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (153, 31, 7, '当天能发吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (154, 31, 7, '48小时发货是真的吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (155, 31, 7, '节假日发货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (156, 32, 7, '发什么快递');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (157, 32, 7, '可以指定快递吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (158, 32, 7, '能发顺丰吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (159, 32, 7, '偏远地区发什么快递');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (160, 32, 7, '支持邮政吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (161, 33, 7, '包邮吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (162, 33, 7, '满多少包邮');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (163, 33, 7, '运费多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (164, 33, 7, '新疆西藏包邮吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (165, 33, 7, '能到付吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (166, 34, 7, '能改地址吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (167, 34, 7, '发货后还能改地址吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (168, 34, 7, '地址填错了怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (169, 34, 7, '能改收货人电话吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (170, 34, 7, '能合并发货吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (171, 35, 7, '物流不更新了');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (172, 35, 7, '快递丢了怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (173, 35, 7, '显示签收没收到');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (174, 35, 7, '派送失败怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (175, 35, 7, '能催一下物流吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (176, 36, 8, '支持哪些支付方式');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (177, 36, 8, '可以用花呗吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (178, 36, 8, '能货到付款吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (179, 36, 8, '可以分期吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (180, 36, 8, '支付失败怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (181, 37, 8, '能开发票吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (182, 37, 8, '开票要多久');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (183, 37, 8, '可以开专票吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (184, 37, 8, '发票内容怎么开');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (185, 37, 8, '发票怎么补开');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (186, 38, 8, '优惠券怎么用');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (187, 38, 8, '能用两张券吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (188, 38, 8, '活动什么时候结束');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (189, 38, 8, '券过期了能补吗');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (190, 38, 8, '会员价怎么算');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (191, 39, 8, '衣服有破损');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (192, 39, 8, '线头很多');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (193, 39, 8, '做工有问题');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (194, 39, 8, '收到货不对版');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (195, 39, 8, '有异味怎么办');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (196, 40, 8, '转人工');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (197, 40, 8, '人工客服电话是多少');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (198, 40, 8, '客服几点上班');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (199, 40, 8, '售后怎么联系');
INSERT INTO `corpus_question` (`id`, `corpus_id`, `group_id`, `question`) VALUES (200, 40, 8, '投诉找谁');
COMMIT;

-- ----------------------------
-- Table structure for demo_logistics
-- ----------------------------
DROP TABLE IF EXISTS `demo_logistics`;
CREATE TABLE `demo_logistics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tracking_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物流单号，业务唯一',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '关联订单号',
  `carrier` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '承运商',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物流状态：已揽收/运输中/派送中/已签收',
  `location` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '最新位置',
  `expected_time` datetime DEFAULT NULL COMMENT '预计送达时间，NULL 表示待定',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tracking_no` (`tracking_no`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='物流跟踪演示';

-- ----------------------------
-- Records of demo_logistics
-- ----------------------------
BEGIN;
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (1, 'SF7712345678001', 'SO202611100001', '顺丰速运', '已签收', '上海市浦东新区，本人签收', '2026-11-03 18:00:00', '2026-11-01 18:20:00', '2026-11-03 18:05:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (2, 'SF7712345678002', 'SO202611100002', '顺丰速运', '运输中', '杭州萧山中转场', '2026-11-11 18:00:00', '2026-11-09 11:10:00', '2026-11-09 11:10:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (3, 'YT7712345678003', 'SO202611100006', '圆通速递', '派送中', '上海市徐汇区漕河泾派送点', '2026-11-10 20:00:00', '2026-11-08 10:40:00', '2026-11-09 09:30:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (4, 'JD7712345678004', 'SO202611100007', '京东物流', '已签收', '北京市朝阳区望京，本人签收', '2026-11-02 20:00:00', '2026-10-29 09:00:00', '2026-11-02 19:40:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (5, 'YD7712345678005', 'SO202611100009', '韵达快递', '已签收', '广州市天河区菜鸟驿站', '2026-11-01 18:00:00', '2026-10-31 10:00:00', '2026-11-01 17:50:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (6, 'SF7712345678006', 'SO202611100010', '顺丰速运', '已揽收', '深圳市宝安区仓库已揽收', NULL, '2026-11-09 09:05:00', '2026-11-09 09:05:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (7, 'YT7712345678007', 'SO202611100011', '圆通速递', '运输中', '广州转运中心', '2026-11-12 12:00:00', '2026-11-09 14:30:00', '2026-11-09 22:10:00');
INSERT INTO `demo_logistics` (`id`, `tracking_no`, `order_no`, `carrier`, `status`, `location`, `expected_time`, `created_at`, `updated_at`) VALUES (8, 'JD7712345678008', 'SO202611100012', '京东物流', '已签收', '成都市武侯区，本人签收', '2026-11-03 21:00:00', '2026-11-01 09:30:00', '2026-11-03 20:30:00');
COMMIT;

-- ----------------------------
-- Table structure for demo_order
-- ----------------------------
DROP TABLE IF EXISTS `demo_order`;
CREATE TABLE `demo_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单号，业务唯一',
  `user_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '下单人',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单状态：待支付/已支付/备货中/已发货/已完成',
  `total_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '订单金额（元）',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '备注',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单查询演示';

-- ----------------------------
-- Records of demo_order
-- ----------------------------
BEGIN;
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (1, 'SO202611100001', '王雨桐', '已完成', 399.00, '米白色 / M / 纯棉圆领长袖T恤', '2026-11-01 10:12:00', '2026-11-05 16:20:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (2, 'SO202611100002', '李思远', '已发货', 268.00, '藏青色 / L / 弹力休闲西裤', '2026-11-06 09:30:00', '2026-11-09 11:05:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (3, 'SO202611100003', '张甜甜', '备货中', 159.00, '黑色 / S / 针织开衫', '2026-11-09 14:22:00', '2026-11-09 18:40:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (4, 'SO202611100004', '陈默', '待支付', 599.00, '卡其色 / XL / 中长款风衣', '2026-11-10 08:50:00', '2026-11-10 08:50:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (5, 'SO202611100005', '周雅', '已支付', 129.00, '浅蓝色 / M / 牛津纺衬衫', '2026-11-09 20:05:00', '2026-11-09 20:06:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (6, 'SO202611100006', '刘子航', '已发货', 88.00, '灰色 / 均码 / 纯棉短袖T恤', '2026-11-07 13:15:00', '2026-11-08 10:30:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (7, 'SO202611100007', '赵敏', '已完成', 459.00, '黑色 / L / 羊毛混纺大衣', '2026-10-28 11:00:00', '2026-11-03 09:12:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (8, 'SO202611100008', '孙悦', '待支付', 199.00, '白色 / M / 真丝衬衫', '2026-11-10 09:40:00', '2026-11-10 09:40:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (9, 'SO202611100009', '吴桐', '已完成', 79.00, '黑色 / 3双装 / 纯棉船袜', '2026-10-30 15:26:00', '2026-11-02 12:00:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (10, 'SO202611100010', '郑一诺', '已发货', 329.00, '墨绿色 / XL / 三合一冲锋衣', '2026-11-08 10:10:00', '2026-11-09 09:00:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (11, 'SO202611100011', '吴倩', '已发货', 259.00, '粉色 / S / 收腰连衣裙', '2026-11-08 16:45:00', '2026-11-09 14:20:00');
INSERT INTO `demo_order` (`id`, `order_no`, `user_name`, `status`, `total_amount`, `remark`, `created_at`, `updated_at`) VALUES (12, 'SO202611100012', '何俊', '已完成', 149.00, '黑色 / 32码 / 直筒牛仔裤', '2026-10-31 19:30:00', '2026-11-04 20:10:00');
COMMIT;

-- ----------------------------
-- Table structure for sys_login_session
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_session`;
CREATE TABLE `sys_login_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `token` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话令牌（uuid），全局唯一',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录名',
  `expire_time` datetime NOT NULL COMMENT '过期时间，每次有效访问滑动续期',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录态会话';

-- ----------------------------
-- Records of sys_login_session
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_secret
-- ----------------------------
DROP TABLE IF EXISTS `sys_secret`;
CREATE TABLE `sys_secret` (
  `id` bigint NOT NULL COMMENT '固定为 1',
  `secret` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'JWT 签名密钥',
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='密钥（单行）';

-- ----------------------------
-- Records of sys_secret
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '登录名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'BCrypt 密文',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '显示名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 启用 0 停用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
BEGIN;
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `status`, `created_at`, `updated_at`) VALUES (1, 'admin', '$2a$10$HUYPJ7QVbjyIOZOTlojDMOwjIoupy0uIRDKyIkb.bFpbGSlH8XfAm', '系统管理员', 1, '2026-09-19 13:50:47', '2026-10-19 13:21:28');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
