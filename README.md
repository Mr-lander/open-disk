# Open-Disk ☁️

![License](https://img.shields.io/github/license/your-org/open-disk)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)
![Build](https://img.shields.io/github/actions/workflow/status/your-org/open-disk/ci.yml)
![Docker](https://img.shields.io/badge/Container-ready-important)

> 毕业设计项目 —— **Open-Disk**：基于 Spring Boot 3 / Java 17 的分布式对象存储 & 私有网盘解决方案，支持冷热分层、Vault 密钥管理、MinIO + Ceph RGW、Elasticsearch 全文检索以及前端 Vue 3 + Vite + Ant Design Vue。

---

## 🌟 特性一览

| 功能                       | 说明                                        |
| ------------------------ | ----------------------------------------- |
| 🔥 **HOT/COLD 分层**       | 热端 MinIO、冷端 Ceph RGW 按访问频次自动迁移            |
| 🔐 **Vault 密钥管理**        | 上传文件名/路径对用户侧加密，密钥存储在 HashiCorp Vault      |
| 🗃️ **Elasticsearch 检索** | 文件元数据实时索引                       |
| 🪝 **WebSocket 实时进度**    | 上传进度条、在线预览、秒传                             |
| 🦾 **微服务架构**             | Spring Cloud OpenFeign 调用，Gateway 统一入口    |
| 🚀 **容器化**      | Docker Compose 一键启动 |

---

## 🏗️ 模块说明

| 模块              | 类型      | 端口        | 主要职责                           |
| --------------- | ------- | --------- | ------------------------------ |
| `common-lib`    | Library | –         | 通用工具、JWT 封装、全局异常               |
| `user-api`      | Library | –         | 用户服务 Feign 接口声明                |
| `uid-generator` | Service | 8003      | 分布式雪花 ID 生成                    |
| `user-svc`      | Service | 8001      | 认证、用户管理、JPA+MySQL              |
| `file-svc`      | Service | 8002      | 文件上传、MinIO & ES 操作、进度推送        |
| `download-svc`  | Service | 8005      | 附件/内联下载、签名 URL                 |
| `cold-tier-svc` | Service | 8006      | 定时迁移至 Ceph 冷端                  |
| `gateway`       | Service | 8080      | Spring Cloud Gateway JWT 路由    |
| `KES-Vault`     | Service | 8200      | Vault Server（二次封装：KES + Vault） |
| `mysql`         | Infra   | 3306      | 元数据存储                          |
| `minio`         | Infra   | 9000/9001 | S3-兼容热端存储                      |
| `elasticsearch` | Infra   | 9200/9300 | 文件搜索                           |
| `vault`         | Infra   | 8200      | 密钥&配置中心                        |
| `ceph`          | Infra   | 8000      | RGW 冷存储（独立 compose）            |
| `nginx`         | Infra   | 80/443    | 前端静态资源 + 反向代理                  |

---

## 📚 目录结构

```
open-disk/
├── ceph/                 # 冷端存储独立 compose
├── cold-tier-svc/        # 冷数据服务
├── common-lib/           # 公共工具包
├── download-svc/         # 下载微服务
├── file-svc/             # 上传/索引微服务
├── gateway/              # API 网关
├── KES-Vault/            # Vault 镜像上下文
├── minio/                # MinIO Dockerfile
├── mysql/                # MySQL Dockerfile
├── open-disk-frontend/   # Vue 3 + Vite 前端
├── uid-generator/        # UID 服务
├── user-api/             # 用户 API
├── user-svc/             # 用户微服务
├── docker-compose.yaml   # 主服务编排
└── pom.xml               # Maven 父工程
```

---

## 🔧 构建流程

### 后端 (Maven)

```bash
# 一次编译所有微服务
mvn clean package -DskipTests
# 生成的可执行 JAR 位于 每个模块/target/*-exec.jar
```

> GitHub Actions 在 `ci.yml` 中缓存 Maven 依赖并自动执行同样流程。

### 前端 (Vue 3 + Vite)

```bash
cd open-disk-frontend
pnpm i               # 安装依赖
pnpm build           # 生成 dist/
```

构建产物将由下方 docker-compose 中的 **nginx** 容器挂载到 `/usr/share/nginx/html`。

---

## 🧑‍💻 开发流程 (Live Dev)

> 👇 以下步骤面向本地调试：IDE + 热重载 + 桌面浏览器。

1. **克隆并导入项目**

   ```bash
   git clone https://github.com/your-org/open-disk.git
   cd open-disk
   ```

   在 IntelliJ IDEA / Eclipse 2024+ 中直接 *Open as Maven Project*，所有微服务自动以 Maven module 形式识别。

2. **启动基础设施容器**
   仅启动数据库、MinIO、ES、Vault（不开微服务）：

   ```bash
   docker compose -f docker-compose-dev.yaml up -d mysql minio elasticsearch vault
   ```

3. **第一次初始化 Vault**

   ```bash
   # 生成初始密钥 & root token（只需一次）
   docker exec -it vault /bin/sh -c "vault operator init -key-shares=1 -key-threshold=1"
   # 复制输出的 Unseal Key 和 Root Token
   # 解封 Vault（同样只需一次）
   vault operator unseal <Unseal-Key>
   export VAULT_ADDR=http://127.0.0.1:8200 && vault login <Root-Token>
   ```
   或直接打开http://127.0.0.1:8200进行初始操作

5. **IDE 启动微服务**

   * 选中模块，如 `file-svc` -> *Run FileApplication*，确保 VM Options：`-Dspring.profiles.active=dev`。
   * 其他服务（`user-svc`, `uid-generator`, `gateway`…）同理，IDE Console 输出即日志。

6. **前端热重载**

   ```bash
   cd open-disk-frontend
   pnpm i
   pnpm dev        # 默认 http://localhost:5173
   ```

   Vite 代理 `/api/**` -> `http://localhost:8080` (Gateway)。

7. **文件上传/下载**
   访问 `http://localhost:5173`，登录→上传→控制台可见实时进度；MinIO Console `http://localhost:9001` 可检查对象。

> 当需要检查冷数据迁移，可手动 `docker compose up -d ceph-rgw cold-tier-svc`，并在 `cold-tier-svc` 日志查看迁移记录。

---


### 一键启动

```bash
# 后端 & 前端
mvn clean package -DskipTests
pnpm --filter "open-disk-frontend" --workspace-root build
# 启动 compose
docker compose up --build -d
```

访问 [http://localhost](http://localhost) 即获得前端页面，通过 Nginx 反向代理到 Gateway (`/api/**`)。

---

## 🤝 贡献

提交 PR / Issue 前请阅读 `CONTRIBUTING.md`。

## 📄 License

[MIT](LICENSE)
