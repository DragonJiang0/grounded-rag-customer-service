# 生产部署配置核验清单

本文件用于核对部署相关的全部配置项。所有可调值集中在两处：

1. `deploy/.env`：环境相关（密码、模型地址、端口、开关），由人填写。
2. `backend/{portal,manage}/src/main/resources/application.yaml`：后端配置模板，值形如 `${VAR:默认值}`。

两个模块各自只有一份 `application.yaml`；容器内设置 `SPRING_PROFILES_ACTIVE=prod`，若日后新增 `application-prod.yaml` 会自动覆盖同名项。

---

## 一、文件分工

| 文件 | 作用 | 是否需改 |
|---|---|---|
| `deploy/.env` | 实际生效的环境变量 | 是（至少改必填项） |
| `deploy/.env.example` | 配置模板，用 `cp .env.example .env` 生成 `.env` | 否 |
| `deploy/docker-compose.yml` | 服务编排（本地构建镜像），启动 mysql / portal / manage / front，注入 `SPRING_PROFILES_ACTIVE=prod` | 一般否 |
| `deploy/docker-compose-quickstart.yml` | 快速启动编排（仓库镜像），三个应用镜像已填好，`pull_policy: always` 每次强制拉取 | 一般否 |
| `deploy/Dockerfile.portal` | portal 运行镜像；构建上下文是**仓库根目录**，COPY `backend/portal/target/portal.jar` | 否 |
| `deploy/Dockerfile.manage` | manage 运行镜像；构建上下文是**仓库根目录**，COPY `backend/manage/target/manage.jar` | 否 |
| `.dockerignore`（仓库根） | 白名单：只放行两个 jar 与前端产物进构建上下文 | 否 |
| `deploy/Dockerfile.front` | 前端 Nginx 镜像；构建上下文是**仓库根目录**，COPY `front/dist`、`chatfront/dist`、`deploy/nginx/front.conf` | 否 |
| `backend/portal/src/main/resources/application.yaml` | portal 配置模板（端口 9090 / gRPC 9091） | 核对默认值 |
| `backend/manage/src/main/resources/application.yaml` | manage 配置模板（端口 8080、gRPC 客户端指向 portal） | 核对默认值 |
| `deploy/nginx/front.conf` | 单入口：`/apps/` 管理后台 + `/chat/` 聊天；`/manage` → manage:8080、`/portal` → portal:9090 | 否 |

---

## 二、环境拓扑

```
浏览器
  └─ :8081  front（单入口）
       ├─ /apps/*    管理后台静态
       ├─ /chat/*    聊天窗口静态
       ├─ /manage/*  ──▶ manage:8080（去 /manage 前缀）
       └─ /portal/*  ──▶ portal:9090（去 /portal 前缀）

manage ──gRPC:9091──▶ portal（compose 服务名）
portal / manage  ──MySQL:3306──▶  mysql

只有 front 映射主机端口；portal / manage / mysql 仅在 compose 内网。
```

---

## 三、必填变量（`deploy/.env`）

| 变量 | 当前值 | 说明 |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | `mysql_root` | MySQL root 密码，后端用同一账号连接（`MYSQL_PWD`） |
| `LLM_BASE_URL` | `http://172.16.1.200:8788` | 模型服务地址，必须是**容器可达**地址 |
| `LLM_MODEL` | `deepseek-v4.1-flash` | 网关上的模型名（可选 `deepseek-v4-pro`） |
| `LLM_API_KEY` | 已填真实 key（仅本地 `.env`，不随仓库提交） | 代码里**没有**内置默认 key，必须给值 |
| `FRONT_HOST_PORT` | `8081` | 前端对外端口（`/apps/` 管理后台、`/chat/` 聊天窗口） |
| `MYSQL_DATABASE` | `server` | 库名，与 `mysql/init` 的建库名一致 |

---

## 四、可选变量（有默认值）

| 变量 | 默认值 | 作用 | 生效位置 |
|---|---|---|---|
| `JAVA_OPTS` | `-Xms512m -Xmx1g` | JVM 参数，portal / manage 各自一个容器，分别生效 | 两个 Dockerfile 的 ENTRYPOINT |
| `LOG_LEVEL` | `info` | 业务日志级别 | portal / manage |
| `LLM_LOG_ENABLED` | `false` | 是否打印模型请求/响应体 | portal |
| `LAYA_ENABLED` | `false` | laya 决策模型开关，开启需把 `-Xmx` 调到 3g 以上 | portal |
| `RPC_CLIENT_DEADLINE_SECONDS` | `240` | manage 调 portal 的 gRPC 超时（秒） | manage |

> 基础镜像一律固定为官方镜像，不通过 `.env` 配置：MySQL `mysql:8.0`（compose）、
> JRE `eclipse-temurin:21.0.12_8-jre-ubi10-minimal`（portal / manage Dockerfile）、Nginx `nginx:1.30.6-trixie-perl`（front Dockerfile）。
> 拉取不到时由使用者自行解决网络/镜像源，仓库里不保留镜像地址配置项。

> 注：容器拓扑相关的 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_PWD`、`PORTAL_RPC_HOST`、`PORTAL_RPC_PORT` 由
> `docker-compose.yml` 固定注入，**不从 .env 读取**；其中 `PORTAL_RPC_HOST` 只注入 manage，值为 `portal`。
> `SERVER_PORT` 刻意不注入——portal 与 manage 分属两个容器，端口分别取各自配置文件的默认值（9090 / 8080）。
> `.env` 中已废弃的 `MYSQL_HOST_PORT` / `MANAGE_HOST_PORT` / `PORTAL_HOST_PORT` 已注释，
> 因为 portal / manage 与 mysql 不再映射主机端口。

---

## 五、容器内配置要点（重点核对）

两个模块各自只有一份 `application.yaml`，其中与容器部署相关的项如下：

| 配置 | 默认值 | 容器内实际值 | 原因 |
|---|---|---|---|
| 数据源 host | `${MYSQL_HOST:mysql}` | `mysql` | compose 注入服务名 |
| manage 的 gRPC host | `${PORTAL_RPC_HOST:127.0.0.1}` | `portal` | portal 与 manage 是两个容器，不能再走回环 |
| 服务端口 | portal 9090 / manage 8080 | 同左 | compose 不注入 `SERVER_PORT` |
| 数据库、LLM、laya 开关、日志级别 | `${VAR:默认值}` | 由 `.env` 注入 | 支持按环境覆盖 |
| `LLM_BASE_URL` | 无默认值（必填） | `.env` 中填写的容器可达地址 | 容器内 127.0.0.1 指向容器自身 |

其余值（Hikari 连接池、Jackson、MyBatis-Plus、laya 的阈值/候选、日志引用关系）均保持原样。

---

## 六、部署步骤

以下命令按行执行，一条一条来。

**1) 准备镜像（二选一）**

用已发布镜像：`deploy/docker-compose-quickstart.yml` 里 `portal`、`manage`、`front` 三个 `image` 已填好，且配置了 `pull_policy: always`，每次 `up` 都会强制拉取最新镜像；换仓库时改这三行。

或用源码构建（在仓库根目录逐行执行，需要 JDK 21 + Maven + Node.js 20+）：

```bash
mvn -f backend/pom.xml -pl portal,manage -am clean package -DskipTests
```

```bash
docker build -f deploy/Dockerfile.portal -t portal:latest .
```

```bash
docker build -f deploy/Dockerfile.manage -t manage:latest .
```

```bash
pnpm -C front install
```

```bash
pnpm -C front run build
```

```bash
npm --prefix chatfront install
```

```bash
npm --prefix chatfront run build
```

```bash
docker build -f deploy/Dockerfile.front -t front:latest .
```

**2) 配置环境变量**

```bash
cd deploy
```

```bash
cp .env.example .env
```

```bash
vi .env
```

至少修改 `MYSQL_ROOT_PASSWORD`、`LLM_BASE_URL`、`LLM_API_KEY`。

**3) 启动**

用已发布镜像：

```bash
docker compose -f docker-compose-quickstart.yml up -d
```

```bash
docker compose -f docker-compose-quickstart.yml ps
```

用本地构建镜像：

```bash
docker compose up -d
```

```bash
docker compose ps
```

访问：`http://<host>:8081/`（自动跳转到 `/apps/` 管理后台），聊天窗口 `http://<host>:8081/chat/`。

---

## 七、部署前必须确认

1. **数据库靠 init SQL 初始化，只在数据目录为空时执行**
   项目没有 Flyway/Liquibase。MySQL 容器首次启动（数据目录为空）会自动执行
   `deploy/mysql/init/*.sql`（已挂载到 `/docker-entrypoint-initdb.d`，无 `USE` 语句时
   由 entrypoint 自动切到 `MYSQL_DATABASE`）。
   数据目录用相对路径绑定挂载到 `deploy/mysql/data`（`./mysql/data:/var/lib/mysql`），
   不再使用 Docker 命名卷，因此数据直接落在项目目录里，容器删掉也不会丢。
   随仓库提交的 `deploy/mysql/init/server.sql` 只含演示数据（23 张表 + 服装店示例
   语料），`sys_secret` 表为空、不含真实密钥；正式部署请替换其中的默认账号与
   演示 AppKey（清单见第七节第 7 条）。
   起过之后再改 SQL 需先 `docker-compose down` 停服务、`rm -rf deploy/mysql/data` 清空
   数据目录，再 `up` 才会重新初始化（`down -v` 现在不起作用，已无命名卷）。

2. **`LLM_BASE_URL` 必须容器可达**
   `application.yaml` 中 `api-key` / `base-url` 都是 `${VAR}` 必填项，没有内置默认值。
   若模型服务在宿主机，需给 `portal` 增加 `extra_hosts: ["host.docker.internal:host-gateway"]`，并把地址改为 `http://host.docker.internal:8788`。

3. **本地开发库为远程 MySQL**
   `backend/docker-compose.yml` 已删除；本地开发直连远程库（`MYSQL_HOST/MYSQL_USER/MYSQL_PWD/MYSQL_DATABASE` 环境变量），容器化整套由本目录 `docker-compose.yml` 提供 MySQL。

4. **laya 开启需要额外资源**
   `LAYA_ENABLED=true` 时需把模型目录挂载到 portal 容器 `./models/laya-multilingual-mlx`，并把 `JAVA_OPTS` 的 `-Xmx` 调到 `3g` 以上。

5. **LLM key 只放在 `.env`**
   `application.yaml` 里 `api-key: ${LLM_API_KEY}` 没有内置默认值，必须由 `.env` 提供；
   `.env` 不随仓库提交，上线前请确认填的是自己的 key，并考虑轮换旧 key。

6. **front 启动依赖后端健康**
   `front` 的 `depends_on` 已改为等待 `manage` / `portal` 达到 `service_healthy`；后端未就绪时 nginx 不会启动，不会再出现启动期 502。
   若运行中仍 502，先看 `docker compose ps` 与 `docker compose logs -f front`。

7. **初始 SQL 自带演示账号与演示密钥，对外前必须替换**
   `deploy/mysql/init/server.sql` 的数据段是服装演示数据（应用「衣见服装旗舰店」，`app_id=1`），
   其中下列凭证**开箱即用**且随仓库公开，值本身不含真实业务信息，但对任何能拿到仓库的人都是可用的：

   | 用途 | 表 / 字段 | 演示值 | 轮换方式 |
   |---|---|---|---|
   | 管理后台登录 | `sys_user`.`username` / `password` | `admin` / `Admin@123`（BCrypt 落库） | 登录后「修改密码」，或替换 SQL 中该行的 hash |
   | 聊天窗口账号密码 | `app_password_window`.`username` / `password` | `cloth` / `Cloth@123`（BCrypt 落库） | 应用详情「账号密码窗口」里重置 |
   | 应用调用凭证 | `app`.`app_key` | `ak_clothingdemo01` | 重新创建应用，并同步前端使用的链接 |
   | API 接入 Key | `app_api_access`.`api_key` | `sk-clothingdemo202611100000000001`（`expire_at` 为 NULL，永久有效） | 应用详情「API 接入」重新生成 |
   | 免密窗口标识 | `app_access_window`.`window_key` | `wk-clothing-demo-2026` | 应用详情「免密窗口」重新生成 |

   `app_key` 与 `window_key` 都直接出现在聊天窗口链接里（`/chat/login/<appKey>`、`/chat/chat/<windowKey>`），
   等同于口令：拿到仓库即可免密进入演示应用。`api_key` 永久有效，可直接调 `/portal` 接口。
   对外开放前请先按上表逐项轮换。
