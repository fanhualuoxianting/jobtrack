# 部署说明

## 开发模式

```powershell
Copy-Item .env.example .env
docker compose up -d mysql redis
cd jobtrack-server; mvn spring-boot:run
cd ..\jobtrack-web; npm ci; npm run dev
```

## 完整 Docker 模式

```powershell
Copy-Item .env.production.example .env.production
# 编辑 .env.production，替换所有 CHANGE_ME 值；JWT_SECRET 至少 32 字节
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

浏览器访问 `http://localhost:8080`。Web 等待 Server 健康，Server 等待 MySQL/Redis 健康；MySQL、Redis 不暴露宿主机端口，数据分别落在 named volume 和上传 volume。重启使用：

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml restart
```

生产不加载演示数据，Swagger 默认关闭。Nginx 提供 SPA history fallback、API 代理、gzip、安全响应头、静态资源长期缓存和 `index.html` 不缓存；SSE 路径关闭 `proxy_buffering`/`proxy_cache`。
