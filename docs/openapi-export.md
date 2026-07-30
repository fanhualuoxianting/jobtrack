# OpenAPI 导出说明

JobTrack 使用 SpringDoc 提供 OpenAPI 文档。

开发环境：

```text
/v3/api-docs
/swagger-ui/index.html
```

## 建议导出

在本地启动后：

```powershell
Invoke-WebRequest http://localhost:8080/v3/api-docs -OutFile docs/openapi.json
```

导出的 JSON 可用于：

- 前端接口联调
- Postman 导入
- 接口评审
- 面试展示

生产环境默认关闭 Swagger/OpenAPI，需要通过配置显式开启：

```properties
SPRINGDOC_ENABLED=true
```

不要将包含内部环境信息的生产接口文档公开到公网。
