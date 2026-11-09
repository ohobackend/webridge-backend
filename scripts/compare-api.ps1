param (
    [string]$DjangoUrl = "http://localhost:8000",
    [string]$JavaUrl = "http://localhost:8080",
    [string]$Email = "test@example.com",
    [string]$Password = "password123!",
    [string]$Token = ""
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " WeBridge API Parity Comparison Tool (Python vs Java)     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Django (Python): $DjangoUrl"
Write-Host "Spring Boot (Java): $JavaUrl"
Write-Host ""

$Passed = 0
$Failed = 0

function Compare-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers,
        [string]$Body
    )

    Write-Host "Testing [$Method] $Path ($Name)..." -NoNewline

    $pyStatus = $null
    $pyContent = $null
    $javaStatus = $null
    $javaContent = $null

    try {
        $pyParams = @{
            Uri = "$DjangoUrl$Path"
            Method = $Method
            ErrorAction = "Stop"
        }
        if ($Headers) { $pyParams.Headers = $Headers }
        if ($Body) {
            $pyParams.Body = $Body
            $pyParams.ContentType = "application/json"
        }
        $pyRes = Invoke-WebRequest @pyParams
        $pyStatus = $pyRes.StatusCode
        $pyContent = $pyRes.Content
    } catch {
        if ($_.Exception.Response) {
            $pyStatus = [int]$_.Exception.Response.StatusCode
        } else {
            $pyStatus = "CONN_ERR"
        }
    }

    try {
        $javaParams = @{
            Uri = "$JavaUrl$Path"
            Method = $Method
            ErrorAction = "Stop"
        }
        if ($Headers) { $javaParams.Headers = $Headers }
        if ($Body) {
            $javaParams.Body = $Body
            $javaParams.ContentType = "application/json"
        }
        $javaRes = Invoke-WebRequest @javaParams
        $javaStatus = $javaRes.StatusCode
        $javaContent = $javaRes.Content
    } catch {
        if ($_.Exception.Response) {
            $javaStatus = [int]$_.Exception.Response.StatusCode
        } else {
            $javaStatus = "CONN_ERR"
        }
    }

    if ($pyStatus -eq $javaStatus -and $javaStatus -ne "CONN_ERR") {
        Write-Host " [PASS] (Status: $javaStatus)" -ForegroundColor Green
        $script:Passed++
    } else {
        Write-Host " [FAIL] (Python: $pyStatus, Java: $javaStatus)" -ForegroundColor Red
        $script:Failed++
    }
}

# 1. Public Endpoints
Compare-Endpoint -Name "Ping" -Method "GET" -Path "/api/v1/ping/"
Compare-Endpoint -Name "Health" -Method "GET" -Path "/api/v1/health/"
Compare-Endpoint -Name "Current Terms" -Method "GET" -Path "/api/v1/terms/current/"

# 2. Authenticated Endpoints
$authHeaders = @{}
if ($Token) {
    $authHeaders["Authorization"] = "Bearer $Token"
    Compare-Endpoint -Name "User Profile" -Method "GET" -Path "/api/v1/user/me/" -Headers $authHeaders
    Compare-Endpoint -Name "Feedback List" -Method "GET" -Path "/api/v1/feedback/" -Headers $authHeaders
    Compare-Endpoint -Name "Scan List" -Method "GET" -Path "/api/v1/scans/" -Headers $authHeaders
} else {
    Write-Host "`n[Notice] Pass -Token '<jwt_access_token>' to test authenticated endpoints." -ForegroundColor Yellow
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " Summary: Passed=$Passed, Failed=$Failed" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
