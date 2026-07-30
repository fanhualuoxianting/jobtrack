# 简历文件存储

上传只接受 PDF/DOCX，并联合校验扩展名、Content-Type 和真实结构；PDF 使用 PDFBox 解析，DOCX 要求 ZIP 内存在 `[Content_Types].xml` 与 `word/document.xml`。

文件先写临时目录，数据库事务成功后保留正式文件；事务回滚或数据库失败会清理临时/正式文件，失败删除进入补偿队列。数据库只保存相对 `storageKey`，所有路径通过根目录归一化校验，拒绝 `..`、绝对路径、反斜杠和 URL 编码穿越。

生产使用独立 `jobtrack_prod_uploads` volume，Nginx 不直接暴露上传目录；下载必须先通过当前用户归属校验。上传大小由 Spring `12MB` 请求限制和 Nginx `12m` 限制共同约束。
