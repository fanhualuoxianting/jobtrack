[CmdletBinding()]
param(
    [string]$BaseUrl = $(if ($env:SMOKE_BASE_URL) { $env:SMOKE_BASE_URL } else { 'http://localhost:8080' }),
    [string]$Account = $(if ($env:SMOKE_ACCOUNT) { $env:SMOKE_ACCOUNT } else { 'smoke_' + (Get-Date -Format 'yyyyMMddHHmmss') }),
    [string]$Password = $(if ($env:SMOKE_PASSWORD) { $env:SMOKE_PASSWORD } else { 'SmokeRun@' + (Get-Random -Minimum 100000 -Maximum 999999) })
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$headers = @{}
$passed = 0
$tempDocx = Join-Path ([IO.Path]::GetTempPath()) ("jobtrack-smoke-{0}.docx" -f [guid]::NewGuid())

function Assert-Step([string]$Name, [scriptblock]$Action) {
    try {
        & $Action | Out-Null
        $script:passed++
        Write-Host "PASS $Name" -ForegroundColor Green
    } catch {
        Write-Host "FAIL $Name" -ForegroundColor Red
        throw
    }
}

function Invoke-Api([string]$Method, [string]$Path, $Body = $null) {
    $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
    }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 'SUCCESS') { throw "API $Path returned $($response.code)" }
    return $response.data
}

function New-DemoDocx([string]$Path) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $stream = [IO.File]::Open($Path, [IO.FileMode]::Create)
    $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in @{
            '[Content_Types].xml' = '<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"></Types>'
            'word/document.xml' = '<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body><w:p><w:r><w:t>JobTrack smoke resume</w:t></w:r></w:p></w:body></w:document>'
        }.GetEnumerator()) {
            $item = $archive.CreateEntry($entry.Key)
            $writer = [IO.StreamWriter]::new($item.Open())
            try { $writer.Write($entry.Value) } finally { $writer.Dispose() }
        }
    } finally { $archive.Dispose(); $stream.Dispose() }
}

try {
    Assert-Step 'health check' { $health = Invoke-RestMethod "$BaseUrl/actuator/health"; if ($health.status -ne 'UP') { throw 'health is not UP' } }
    Assert-Step 'register smoke user' { Invoke-Api POST '/api/v1/auth/register' @{ username = $Account; email = "$Account@example.com"; password = $Password; confirmPassword = $Password; nickname = 'Smoke User' } }
    Assert-Step 'login' { $login = Invoke-Api POST '/api/v1/auth/login' @{ account = $Account; password = $Password; deviceName = 'smoke-script' }; $headers.Authorization = "Bearer $($login.accessToken)" }
    Assert-Step 'current user' { Invoke-Api GET '/api/v1/auth/me' }

    $suffix = Get-Date -Format 'yyyyMMddHHmmssfff'
    $company = Invoke-Api POST '/api/v1/companies' @{ name = "Smoke 公司 $suffix"; shortName = 'Smoke'; industry = '软件服务'; city = '测试城'; website = 'https://smoke.example.com' }
    $companyId = $company.id
    Assert-Step 'create company' { if (-not $companyId) { throw 'company id missing' } }

    $position = Invoke-Api POST '/api/v1/positions' @{ companyId = $companyId; title = "Smoke Java 岗位 $suffix"; department = '研发'; city = '测试城'; workType = 'INTERNSHIP'; workplaceType = 'REMOTE'; salaryMin = 100; salaryMax = 200; salaryUnit = 'DAY'; currency = 'CNY'; source = 'Smoke' }
    $positionId = $position.id
    Assert-Step 'create position' { if (-not $positionId) { throw 'position id missing' } }

    New-DemoDocx $tempDocx
    $upload = Invoke-RestMethod -Uri "$BaseUrl/api/v1/resumes" -Method Post -Headers $headers -Form @{ file = Get-Item $tempDocx; versionName = 'Smoke 版本'; note = 'smoke test' } -TimeoutSec 30
    if ($upload.code -ne 'SUCCESS') { throw "API /resumes returned $($upload.code)" }
    $resumeId = $upload.data.id
    Write-Host 'PASS upload resume' -ForegroundColor Green; $passed++

    $application = Invoke-Api POST '/api/v1/applications' @{ companyId = $companyId; positionId = $positionId; resumeId = $resumeId; priority = 'HIGH'; source = 'Smoke'; appliedAt = (Get-Date).ToUniversalTime().ToString('o'); note = 'smoke application' }
    $applicationId = $application.id; $version = $application.version
    Assert-Step 'create application' { if (-not $applicationId) { throw 'application id missing' } }

    $transition = Invoke-Api POST "/api/v1/applications/$applicationId/transitions" @{ targetStatus = 'APPLIED'; reason = 'smoke flow'; expectedVersion = $version; idempotencyKey = "smoke-$suffix" }
    $version = $transition.version
    Assert-Step 'transition application' { if ($transition.status -ne 'APPLIED') { throw 'application did not transition' } }
    Assert-Step 'timeline' { if ((Invoke-Api GET "/api/v1/applications/$applicationId/timeline").Count -lt 2) { throw 'timeline is incomplete' } }

    $interview = Invoke-Api POST '/api/v1/interviews' @{ applicationId = $applicationId; roundNumber = 1; roundName = 'Smoke 面试'; interviewType = 'VIDEO'; scheduledStartAt = (Get-Date).ToUniversalTime().AddDays(2).ToString('o'); scheduledEndAt = (Get-Date).ToUniversalTime().AddDays(2).AddHours(1).ToString('o'); timezone = 'Asia/Shanghai'; meetingUrl = 'https://meeting.example.com/smoke'; interviewer = 'Smoke Interviewer' }
    Assert-Step 'create interview' { if (-not $interview.id) { throw 'interview id missing' } }
    Assert-Step 'notifications' { Invoke-Api GET '/api/v1/notifications?readState=UNREAD&page=1&pageSize=10' }
    Assert-Step 'dashboard' { $dashboard = Invoke-Api GET '/api/v1/dashboard/summary?startDate=2026-07-01&endDate=2026-08-31&timezone=Asia/Shanghai&granularity=MONTH'; if ($null -eq $dashboard.totalApplications) { throw 'dashboard summary missing' } }

    Assert-Step 'archive application' { Invoke-Api POST "/api/v1/applications/$applicationId/archive" }
    Assert-Step 'restore application' { Invoke-Api POST "/api/v1/applications/$applicationId/restore" }
    Write-Host "Smoke completed: $passed checks passed. Account and password are intentionally not printed." -ForegroundColor Cyan
} finally {
    if (Test-Path $tempDocx) { Remove-Item -LiteralPath $tempDocx -Force }
}
