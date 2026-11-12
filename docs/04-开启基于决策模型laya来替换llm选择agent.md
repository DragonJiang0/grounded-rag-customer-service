# 开启基于决策模型 laya 来替换 LLM 选择 Agent

本文讲如何把「多 Agent 选择」从 **大模型排序** 切换成 **laya 决策模型**（进程内推理），
以及开启后如何配置模型位置、如何在网络受限环境下把模型构建进 Docker 镜像。

> 前置：先看完 `03-开发自定义的Agent.md` 的第 5.3 节，了解两个选择器是怎么互斥注册的。

---

## 1. 背景：laya 替换了什么

项目里「选哪个 Agent 回答用户」由 `AgentSelector` 接口负责，两个实现**互斥注册**，
切换只改配置、不改代码：

| 开关 | 生效的选择器 | 机制 | 特点 |
| --- | --- | --- | --- |
| `LAYA_ENABLED=false`（默认） | `LlmRankingAgentSelector` | 大模型给每个 Agent 各打一个分，取超过阈值（默认 0.6）的最高分 | 一次 LLM 调用，较慢 |
| `LAYA_ENABLED=true` | `LayaAgentSelector` | 进程内 laya 决策模型，一道 `choice` 题直接返回「在候选里挑一个」的概率分布 | 毫秒级、结果确定、不占模型调用额度 |

所以「开启 laya」=「用本地决策模型替换大模型排序」，目的就是省掉那次 LLM 调用、让选择更快更确定。

---

## 2. 开启方式（一个开关）

编辑 `deploy/.env`，把 laya 开关打开：

```bash
# ---------- laya 决策模型开关 ----------
LAYA_ENABLED=true
```

它映射到 `application.yaml` 的 `portal.ai.laya.enabled`：
- 为 `true` 时，`LayaAgentSelector` 注册、`LlmRankingAgentSelector` 不注册；
- 为 `false` 时相反（laya 模型不加载，不占堆内存）。

改完重新启动 portal 容器即可。

---

## 3. 资源要求（必须先满足）

laya 模型常驻内存，且加载发生在容器启动阶段，开启前要同步调整两处：

1. **堆内存**：模型约占 2.5G 堆，`JAVA_OPTS` 的 `-Xmx` 要调到 **3g 以上**；
2. **JVM 模块**：需要 `--add-modules=jdk.incubator.vector`（laya 的 GEMM 加速用）。

把 `.env` 里的 `JAVA_OPTS` 改成（示例）：

```bash
JAVA_OPTS="-Xms512m -Xmx3g --add-modules=jdk.incubator.vector"
```

> 加载模型会让 portal 启动变慢几十秒。compose 里 portal 的 `healthcheck.start_period` 已是 120s，够用；
> 如果模型很大，注意观察 `docker compose logs -f portal`，别在还没加载完时误判为不健康。

---

## 4. 配置模型位置

模型目录由 `portal.ai.laya.model-source` 决定，`.env` 里用 `LAYA_MODEL_SOURCE` 覆盖。

| 项 | 值 |
| --- | --- |
| 默认路径 | `./models/laya-multilingual-mlx` |
| 相对基准 | 进程工作目录；容器内 WORKDIR 是 `/app`，即实际是 `/app/models/laya-multilingual-mlx` |
| 覆盖方式 | `.env` 里 `LAYA_MODEL_SOURCE=/app/models/laya-multilingual-mlx`（按需改） |

一个完整可加载的 laya checkpoint 目录必须包含这 5 个文件：

```
models/laya-multilingual-mlx/
├── model.safetensors
├── rl_agent_config.json
├── encoder/config.json
├── tokenizer/tokenizer.json
└── tokenizer/tokenizer_config.json
```

**目录不存在时的自动下载**（`LayaConfig` 的逻辑）：
本地模型目录不在时，会按 `portal.ai.laya.fallback-model-source`（HF repo id `aac6fef/laya-multilingual-mlx`）
自动下载一份，缓存到 `$LAYA_CACHE`（默认 `~/.cache/laya`），再软链回 `model-source` 指向的路径。

下载走 Hugging Face，可用环境变量换镜像源 / 带令牌：

| 环境变量 | 作用 | 示例 |
| --- | --- | --- |
| `HF_ENDPOINT` | 下载地址的根（默认 `https://huggingface.co`），网络不好时换国内镜像 | `https://hf-mirror.com` |
| `HF_TOKEN` | 仓库需要鉴权时带上 | 你的 HF token |

---

## 5. 网络考虑：本地先下载，再打进镜像（推荐）

容器运行时联网下载模型不可控（网络、代理、每次启动重复下载）。推荐做法：
**先在本地把模型下载好，再把它构建进 portal 镜像**，容器启动时直接读本地目录、全程离线。

### 5.1 方式一：让程序自己下载（最简单）

1. 本地（宿主机）临时设置镜像源并启动一次，让它自动下载：

   ```bash
   export HF_ENDPOINT=https://hf-mirror.com
   ```

2. 本地裸跑 portal（`LAYA_ENABLED=true`、`LAYA_MODEL_SOURCE=./models/laya-multilingual-mlx`）。
   `LayaConfig` 会下载到 `~/.cache/laya` 并在 `./models/laya-multilingual-mlx` 建软链。

3. 下载完成后的真实文件在缓存目录，把它拷贝成实体目录（去掉软链）：

   ```bash
   rm -f models/laya-multilingual-mlx
   ```

   ```bash
   cp -rL ~/.cache/laya/aac6fef--laya-multilingual-mlx@main models/laya-multilingual-mlx
   ```

   > 缓存目录名规则是 `{owner--repo}@{revision}`，`aac6fef/laya-multilingual-mlx` → `aac6fef--laya-multilingual-mlx@main`。
   > 不确定名字时直接看 `~/.cache/laya/` 下的目录。

### 5.2 方式二：手动下载 5 个文件

不跑程序，直接按 `ModelStore` 的下载规则拉取这 5 个文件（rev 默认 `main`）：

```bash
BASE=https://hf-mirror.com/aac6fef/laya-multilingual-mlx/resolve/main
```

```bash
mkdir -p models/laya-multilingual-mlx/encoder models/laya-multilingual-mlx/tokenizer
```

```bash
curl -L -o models/laya-multilingual-mlx/model.safetensors "$BASE/model.safetensors"
```

```bash
curl -L -o models/laya-multilingual-mlx/rl_agent_config.json "$BASE/rl_agent_config.json"
```

```bash
curl -L -o models/laya-multilingual-mlx/encoder/config.json "$BASE/encoder/config.json"
```

```bash
curl -L -o models/laya-multilingual-mlx/tokenizer/tokenizer.json "$BASE/tokenizer/tokenizer.json"
```

```bash
curl -L -o models/laya-multilingual-mlx/tokenizer/tokenizer_config.json "$BASE/tokenizer/tokenizer_config.json"
```

> 把 `BASE` 换成 `https://huggingface.co/...` 或你的镜像站均可。

---

## 6. 把模型构建进 Docker

模型准备好（在仓库根目录 `models/laya-multilingual-mlx/`）后，做三步。

### 6.1 `.dockerignore` 白名单放行

根目录 `.dockerignore` 是白名单（`*` 排除一切、`!` 逐项放行），需要放行模型目录，
否则模型文件进不了构建上下文，`COPY` 会失败。在文件里加一行：

```dockerignore
# laya 决策模型：开启 laya 并把模型打进镜像时需要放行
!models
```

### 6.2 `Dockerfile.portal` 加一行 COPY

在 `deploy/Dockerfile.portal` 里，取消示例注释（示例已随本文档加好，见该文件）：

```dockerfile
# 开启 laya 决策模型时，把模型目录一并打进镜像，避免容器运行时联网下载。
# 取消下面一行注释并重新构建，运行时 model-source 保持默认 ./models/laya-multilingual-mlx 即可。
# COPY models/laya-multilingual-mlx /app/models/laya-multilingual-mlx
```

### 6.3 重新构建并启动

```bash
docker build -f deploy/Dockerfile.portal -t portal:latest .
```

```bash
cd deploy
```

```bash
docker compose up -d portal
```

容器内模型就位在 `/app/models/laya-multilingual-mlx`，启动时直接加载、不再联网。

---

## 7. 可选：调整选择阈值与候选

开启 laya 后，选择行为由 `application.yaml` 的 `portal.ai.laya.*` 控制（默认值已够用）：

| 配置 | 默认 | 作用 |
| --- | --- | --- |
| `min-score` | `0.5` | 选中所需的最低概率（choice 的 top1） |
| `margin` | `0.15` | top1 与 top2 的最小差距，太小不选（回落默认 Agent） |
| `candidates` | `[]` | 候选 Agent 白名单，留空 = 平台内全部 Agent |
| `exclude-prefixes` | `[Debug]` | 前缀黑名单，默认排除调试变体 |
| `fallback-label` / `fallback-description` | `None` / 闲聊话术 | 兜底选项，闲聊、无关问题落到它（等价于不选） |

这些值是否需要改取决于你自己的话术，一般先按默认跑，看日志里的概率分布再标定。

---

## 8. 验证是否生效

启动后看 portal 日志：

```bash
docker compose logs -f portal
```

看到这行说明模型加载成功、选择器已切换：

```
[laya] 使用模型 /app/models/laya-multilingual-mlx
```

用户提问时（多 Agent 开关打开的应用），会出现 laya 的决策日志：

```
[laya] 意图=我的快递到哪了, 候选数=2, 分布={LogisticsTrackAgent=0.59, None=0.96...}, 推理耗时=12ms
[laya] 选中 Agent: LogisticsTrackAgent
```

- 命中业务 Agent → 走该 Agent；
- 命中 `None`、或 top1 低于 `min-score`、或 top1-top2 差距小于 `margin` → 回落默认 Agent。

若日志里仍是 `[llm] ...` 前缀，说明 `LAYA_ENABLED` 没生效（检查 `.env` 是否被 compose 读入、portal 是否已重建/重启）。
