param([int]$MongoPort = 27017, [string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtimeRoot = Join-Path $projectRoot '.runtime'
$environment = Get-Content -LiteralPath (Join-Path $runtimeRoot 'runner.environment.json') -Raw | ConvertFrom-Json
$variables = @{}
foreach ($entry in $environment.values) { $variables[$entry.key] = $entry.value }
$mongoQuery = 'JSON.stringify({genres:db.genres.countDocuments(),rooms:db.cinema_rooms.countDocuments(),movies:db.movies.countDocuments(),showtimes:db.showtimes.countDocuments()})'
function Get-CatalogCounts {
    $counts = docker exec cinema-mongo mongosh --quiet -u root -p password --authenticationDatabase admin cinema_movie --eval $mongoQuery
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read catalog counts.' }
    return $counts | ConvertFrom-Json | ConvertTo-Json -Compress
}
$before = Get-CatalogCounts
$checks = @()
try {
    & (Join-Path $PSScriptRoot 'stop.ps1') -ServiceNames movie-service
    $body = @{ items = @(@{ showtimeId = $variables.showtimeId; seatCode = 'B2' }) } | ConvertTo-Json -Depth 5
    foreach ($test in @(
        @{ Name = 'Booking returns 503 while Movie Service is stopped'; Method = 'POST'; Path = '/api/bookings'; Body = $body },
        @{ Name = 'Seat map returns 503 while Movie Service is stopped'; Method = 'GET'; Path = "/api/bookings/showtimes/$($variables.showtimeId)/seats"; Body = $null }
    )) {
        $status = 0
        try {
            $params = @{ Uri = ($variables.gateway + $test.Path); Method = $test.Method; Headers = @{ Authorization = "Bearer $($variables.customerToken)" }; TimeoutSec = 15 }
            if ($test.Body) { $params.Body = $test.Body; $params.ContentType = 'application/json' }
            Invoke-RestMethod @params | Out-Null
            $status = 200
        } catch {
            if (-not $_.Exception.Response) { throw }
            $status = [int]$_.Exception.Response.StatusCode
        }
        if ($status -ne 503) { throw "$($test.Name): actual status $status" }
        Write-Host "PASS $($test.Name)"
        $checks += @{ check = $test.Name; passed = $true; status = $status }
    }
} finally {
    & (Join-Path $PSScriptRoot 'start.ps1') -ServiceNames movie-service -MongoPort $MongoPort -JavaHome $JavaHome
}
$ready = $false
for ($attempt = 0; $attempt -lt 45; $attempt++) {
    try {
        Invoke-RestMethod -Uri "$($variables.gateway)/api/genres" -TimeoutSec 3 | Out-Null
        $ready = $true
        break
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ready) { throw 'Movie Service did not restart within the readiness timeout.' }
$after = Get-CatalogCounts
if ($before -ne $after) { throw "Catalog changed after restart: $before -> $after" }
Write-Host 'PASS Movie Service restart preserves collection counts'
$checks += @{ check = 'Movie Service restart preserves collection counts'; passed = $true; before = ($before | ConvertFrom-Json); after = ($after | ConvertFrom-Json) }
$checks | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $runtimeRoot 'outage-verification.json') -Encoding UTF8
