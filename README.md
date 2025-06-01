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
| 🗃️ **Elasticsearch 检索** | 文件元数据实时索引                         |
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

## 🐳 生产级 docker-compose

```yaml
version: "3.8"
services:
  ## 数据库 & 存储基础设施 ---------------------------------
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
    volumes: [minio-data:/data]

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.1
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports: ["9200:9200", "9300:9300"]
    volumes: [es-data:/usr/share/elasticsearch/data]

  vault:
    build: ./KES-Vault/docker
    command: ["server"]
    cap_add: ["IPC_LOCK"]
    ports: ["8200:8200"]
    volumes: [vault_data:/vault/data]

  ## 核心微服务 ---------------------------------------------
  uid-generator:
    image: uid-generator:latest
    build: ./uid-generator
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [mysql]

  user-svc:
    image: user-svc:latest
    build: ./user-svc
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATASOURCE_URL: jdbc:mysql://mysql:3306/openDisk
      DATASOURCE_USER: root
      DATASOURCE_PASSWORD: 123456
    depends_on: [mysql, uid-generator]

  file-svc:
    image: file-svc:latest
    build: ./file-svc
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DATASOURCE_URL: jdbc:mysql://mysql:3306/openDisk
      UID_ENDPOINT: uid-generator:8003
      MINIO_URL: http://minio:9000
      ELASTICSEARCH_URI: http://elasticsearch:9200
    depends_on: [mysql, minio, elasticsearch, uid-generator]

  download-svc:
    image: download-svc:latest
    build: ./download-svc
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [file-svc]

  cold-tier-svc:
    image: cold-tier-svc:latest
    build: ./cold-tier-svc
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [file-svc, ceph-rgw]

  gateway:
    image: gateway:latest
    build: ./gateway
    ports: ["8080:8080"]
    environment:
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [user-svc, file-svc, download-svc]

  ## 前端 + Nginx -------------------------------------------
  nginx:
    image: nginx:1.25-alpine
    ports:
      - "80:80"
    volumes:
      - ./open-disk-frontend/dist:/usr/share/nginx/html:ro
      - ./config/nginx.conf:/etc/nginx/conf.d/default.conf:ro
    depends_on: [gateway]

  ## 冷端 Ceph RGW 独立 -------------------------------------
  ceph-rgw:
    image: ceph/daemon:latest-luminous
    restart: unless-stopped
    environment:
      - RGW_NAME=rgw1
    ports: ["8000:8000"]
    volumes:
      - ceph-data:/var/lib/ceph

volumes:
  mysql-data:
  minio-data:
  es-data:
  vault_data:
  ceph-data:
```

> ⚠️ Ceph 集群镜像仅用于 DEMO，生产应使用 Rook-Ceph 或外部 RGW。

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
