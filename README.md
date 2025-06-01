# open-disk ☁️

![License](https://img.shields.io/github/license/your‑org/open‑disk)
![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen)
![Build](https://img.shields.io/github/actions/workflow/status/your‑org/open‑disk/ci.yml)
![Docker](https://img.shields.io/badge/Container-ready-important)

> 毕业设计项目 —— **Open‑Disk**：基于 Spring Boot 3 / Java 17 的分布式对象存储 & 私有网盘解决方案，支持冷热分层、Vault 密钥管理、MinIO + Ceph RGW、Elasticsearch 全文检索以及前端 Vue 3 + Vite + Ant Design Vue。

---

## 🌟 特性一览

| 功能                       | 说明                                       |
| ------------------------ | ---------------------------------------- |
| 🔥 **HOT/COLD 分层**       | 热端 MinIO、冷端 Ceph RGW 按访问频次自动迁移           |
| 🔐 **Vault 密钥管理**        | 上传文件名/路径对用户侧加密，密钥存储在 HashiCorp Vault     |
| 🗃️ **Elasticsearch 检索** | 文件元数据实时索引，支持全文搜索                         |
| 🪝 **WebSocket 实时进度**    | 上传进度条、在线预览、秒传                            |
| 🦾 **微服务架构**             | Spring Cloud OpenFeign 调用，Gateway 统一入口   |
| 🚀 **容器化 & DevOps**      | Docker Compose 一键启动，本地 ⚖️ ↔️ 生产 K8s 轻松切换 |

---

## 🏗️ 模块说明

| 模块              | 类型      | 端口        | 主要职责                             |
| --------------- | ------- | --------- | -------------------------------- |
| `common-lib`    | Library | –         | 通用工具、JWT 封装、全局异常                 |
| `user-api`      | Library | –         | 用户服务 Feign 接口声明                  |
| `uid-generator` | Service | 8003      | 分布式雪花 ID 生成                      |
| `user-svc`      | Service | 8001      | 认证、用户管理、JPA+MySQL                |
| `file-svc`      | Service | 8002      | 文件上传、MinIO & ES 操作、进度推送          |
| `download-svc`  | Service | 8005      | 附件/内联下载、限速、签名 URL                |
| `cold-tier-svc` | Service | 8006      | 定时迁移至 Ceph 冷端、回调                 |
| `gateway`       | Service | 8080      | Spring Cloud Gateway 路由 & JWT 网关 |
| `KES-Vault`     | Service | 8200      | Vault Server（二次封装：KES + Vault）   |
| `mysql`         | Infra   | 3306      | 元数据存储                            |
| `minio`         | Infra   | 9000/9001 | S3‑兼容热端对象存储                      |
| `elasticsearch` | Infra   | 9200/9300 | 文件搜索                             |
| `vault`         | Infra   | 8200      | 密钥&配置中心                          |
| `ceph`          | Infra   | 8000      | RGW 冷存储（独立 compose）              |

---

## 📚 目录结构

```
open-disk/
├── ceph/                 # 冷端存储独立 compose
├── cold-tier-svc/        # 冷数据服务
├── common-lib/           # 公共工具包
├── config/               # 通用配置、脚本
├── download-svc/         # 下载微服务
├── file-svc/             # 上传/索引微服务
├── gateway/              # API 网关
├── KES-Vault/            # 内嵌 Vault 镜像上下文
├── minio/                # MinIO Dockerfile
├── mysql/                # MySQL Dockerfile
├── open-disk-frontend/   # Vue 3 + Vite 前端
├── uid-generator/        # UID 服务
├── user-api/             # 用户 API
├── user-svc/             # 用户微服务
├── docker-compose.yaml   # 主服务编排
└── pom.xml               # Maven 父工程
```

---

## 🚀 快速开始

### 1. 本地开发 (Dev)

```bash
# 克隆仓库
$ git clone https://github.com/your-org/open-disk.git
$ cd open-disk

# 启动后端基础设施 & 微服务
$ docker compose up --build -d

# 启动前端
$ cd open-disk-frontend
$ pnpm i && pnpm dev  # 或 npm / yarn
```

> 默认端口：8080 (Gateway) · 8001 (user-svc) · 8002 (file-svc) · 8003 (uid) · 8005 (download) · 9200 (ES) · 9000 (MinIO) · 3306 (MySQL) · 8200 (Vault)

### 2. 生产部署 (Docker Compose)

```bash
# 仅示意：将 SPRING_PROFILES_ACTIVE=prod 写入 .env 或 compose 环境块
$ docker compose -f docker-compose.yaml --profile prod up -d
```

### 3. 扩展部署 (Kubernetes)

* 使用 `helm/` 目录中的 Helm chart，或 `k8s/` 清单：

  ```bash
  kubectl apply -f k8s/
  ```
* Service 名与 `application-prod.yaml` 中的 DNS 保持一致 (`mysql`, `minio`, `vault`, `elasticsearch` …)。
* MinIO & Ceph 可换成真正的 AWS S3 / OSS / Rook‑Ceph Operator。

---

## 🐳 docker-compose.yaml（核心片段）

```yaml
version: "3.8"
services:
  mysql:
    build: ./mysql
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: openDisk
    volumes: [mysql-data:/var/lib/mysql]
  minio:
    build: ./minio
    environment:
      MINIO_ROOT_USER: root
      MINIO_ROOT_PASSWORD: 123456789@A
    command: server /data --console-address ":9001"
    ports: ["9000:9000", "9001:9001"]
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.1
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports: ["9200:9200", "9300:9300"]
  vault:
    build: ./KES-Vault/docker
    command: ["server"]
    cap_add: ["IPC_LOCK"]
    ports: ["8200:8200"]
    volumes: [./vault_data:/vault/data]

volumes:
  mysql-data:
  minio-data:
  vault_data:
  es-data:
```

> **Ceph 冷端**：位于 `ceph/docker-compose.yaml`，独立启动以便灵活接裸盘或外部集群。

---

## 🛠️ 前端 (Vue 3 + Vite + AntD Vue)

```bash
cd open-disk-frontend
pnpm build            # 打包到 dist/
```

* 与 Gateway 同源，通过 `/api/**` 代理后端。
* 登录 / 上传 / 在线预览 / 实时进度条 / 文件分享短链。

---

## 🤝 贡献

欢迎 PR、Issue 或 Star ⭐！请遵循 `CONTRIBUTING.md`。

## 📄 License

[MIT](LICENSE)
