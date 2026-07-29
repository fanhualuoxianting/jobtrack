# JobTrack 实习投递与面试管理平台

面向高校学生求职场景的前后端分离管理平台，支持公司与岗位管理、简历版本追踪、投递状态流转、多轮面试日程、站内提醒和求职数据统计。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security、MyBatis-Plus、MySQL 8.4、Redis
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、ECharts
- 部署：Docker Compose、Nginx

## 快速启动

### 1. 基础设施

```bash
cp .env.example .env
docker compose up -d mysql redis
```

### 2. 后端

```bash
cd jobtrack-server
mvn clean test
mvn spring-boot:run
```

### 3. 前端

```bash
cd jobtrack-web
npm install
npm run dev
```

### 4. 访问

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/actuator/health
- Swagger：http://localhost:8080/swagger-ui/index.html

## 演示账号

```
邮箱：demo@jobtrack.local
密码：JobTrack@123456
```

## 项目结构

```
jobtrack/
├── jobtrack-server/     # Spring Boot 后端
├── jobtrack-web/        # Vue 3 前端
├── deploy/              # Docker、Nginx 部署配置
├── docs/                # 接口、数据库、截图
├── sql/                 # 建表与初始化数据脚本
├── docker-compose.yml
├── .env.example
└── README.md
```

## 开发状态

当前处于阶段1（工程骨架），后续阶段逐步实现认证、业务模块、统计和部署。
