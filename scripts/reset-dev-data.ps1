param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not $Force) {
    Write-Warning '即将删除当前 JobTrack 开发环境的 MySQL 和 Redis Docker volume。'
    Write-Warning '这不会删除项目文件，但会删除本地开发数据；不要在生产环境执行。'
    $confirmation = Read-Host '输入 RESET-DEV-DATA 继续'
    if ($confirmation -cne 'RESET-DEV-DATA') {
        Write-Host '已取消。'
        exit 1
    }
}

docker compose down -v
docker compose up -d mysql redis
Write-Host '开发数据已重置，等待 MySQL/Redis 健康检查完成后即可启动后端。'
