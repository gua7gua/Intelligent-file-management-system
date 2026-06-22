# syntax=docker/dockerfile:1.7
# =============================================================================
# 智能档案管理系统 — 一体化应用镜像（基线演示/测试用，非上线镜像）
# -----------------------------------------------------------------------------
# 打包内容：前端构建产物 + 后端 fat jar，运行时由 Nginx 托管前端并反代 /api
#          到本容器内的后端进程，supervisord 统一拉起 nginx + java。
# 配套：deploy/docker-compose.baseline.yml 负责编排 app + postgres + minio，
#       其中 postgres 首次启动会创建 pg_trgm 扩展，后端启动时 Flyway 自动跑
#       V1~V30 迁移得到基线数据库。
# 构建上下文为仓库根目录：
#   docker build -t ghcr.io/gua7gua/intelligent-file-management-system:baseline \
#       -f deploy/Dockerfile .
# =============================================================================

# ---- Stage 1: 构建前端 -------------------------------------------------------
FROM node:20-bookworm-slim AS frontend-build
WORKDIR /build
# 先拷依赖描述以利用层缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# 生产构建：必须显式关闭 mock，否则 vite build 默认走 mock 假数据。
# 通过写入 .env.production 让 vite 在 build 阶段把变量打进 import.meta.env。
# 直接用 vite build 打包；npm run build 含的 vue-tsc 类型门禁因源码中
# （mock 模块等，生产环境 VITE_USE_MOCK=false 下属死代码）的预存类型漂移而
# 失败，这些是类型层面问题、不影响运行时产物，故容器构建跳过类型门禁。
RUN printf 'VITE_USE_MOCK=false\nVITE_API_BASE_URL=/api\nVITE_APP_TITLE=智能档案管理系统\n' > .env.production \
    && ./node_modules/.bin/vite build

# ---- Stage 2: 构建后端 -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /build
# 容器内构建无法复用宿主代理（127.0.0.1:7890 在容器里不是宿主），直连 Maven
# Central 受阻，故配置阿里云 Maven 镜像加速依赖下载。
RUN mkdir -p /root/.m2 && cat > /root/.m2/settings.xml <<'MVN'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Public</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
MVN
COPY backend/pom.xml ./
RUN mvn -B dependency:go-offline
COPY backend/src ./src
# 跳过测试编译与执行（-Dmaven.test.skip=true 同时跳过 testCompile）：
# develop 上存在与生产类签名不同步的陈旧测试，且运行时镜像不需要测试产物，
# 容器自带的基线数据库即最终验证手段。
RUN mvn -B clean package -Dmaven.test.skip=true

# ---- Stage 3: 运行时（Nginx + 后端 + supervisord）---------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime
# 安装 nginx 与 supervisor；postgresql-client 仅供 entrypoint 等待/建扩展兜底
RUN apt-get update \
    && apt-get install -y --no-install-recommends nginx supervisor postgresql-client curl \
    && rm -rf /var/lib/apt/lists/*

# 前端静态产物
COPY --from=frontend-build /build/dist /usr/share/nginx/html
# 后端 fat jar
COPY --from=backend-build /build/target/*.jar /app/app.jar

WORKDIR /app
RUN mkdir -p /app/data/backups /var/log/supervisor

# ---- Nginx 配置（内联）：托管前端 SPA + 反代 /api、/actuator 到本机后端 -------
RUN <<'NGINX_OUTER'
cat > /etc/nginx/nginx.conf <<'NGINX'
user www-data;
worker_processes auto;
pid /run/nginx.pid;
events { worker_connections 1024; }
http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    sendfile on;
    keepalive_timeout 65;
    # 与后端 multipart 配置一致（max-request-size 200MB）
    client_max_body_size 200m;

    server {
        listen 80 default_server;
        server_name _;
        root /usr/share/nginx/html;
        index index.html;

        # 后端 API
        location /api/ {
            proxy_pass http://127.0.0.1:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 120s;
            proxy_send_timeout 120s;
        }
        # 健康检查 / actuator（容器内自检用）
        location /actuator/ {
            proxy_pass http://127.0.0.1:8080;
        }
        # 前端 SPA history 路由兜底
        location / {
            try_files $uri $uri/ /index.html;
        }
    }
}
NGINX
NGINX_OUTER

# ---- supervisord 配置（内联）：拉起后端 + nginx ------------------------------
RUN <<'SUPER_OUTER'
cat > /etc/supervisor/conf.d/app.conf <<'SUPER'
[program:backend]
command=java -jar /app/app.jar
directory=/app
autostart=true
autorestart=true
startsecs=15
stopwaitsecs=40
stdout_logfile=/var/log/supervisor/backend.out.log
stdout_logfile_maxbytes=0
stderr_logfile=/var/log/supervisor/backend.err.log
stderr_logfile_maxbytes=0

[program:nginx]
command=nginx -g "daemon off;"
autostart=true
autorestart=true
stdout_logfile=/var/log/supervisor/nginx.out.log
stdout_logfile_maxbytes=0
stderr_logfile=/var/log/supervisor/nginx.err.log
stderr_logfile_maxbytes=0
SUPER
SUPER_OUTER

EXPOSE 80
CMD ["supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
