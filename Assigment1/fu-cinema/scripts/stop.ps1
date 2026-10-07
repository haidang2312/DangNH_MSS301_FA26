param(
    [ValidateSet('customer-service', 'movie-service', 'booking-service', 'api-gateway')]
    [string[]]$ServiceNames = @('api-gateway', 'booking-service', 'movie-service', 'customer-service')
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtimeRoot = Join-Path $projectRoot '.runtime'
foreach ($serviceName in @('api-gateway', 'booking-service', 'movie-service', 'customer-service')) {
    if ($serviceName -notin $ServiceNames) { continue }
    $pidFile = Join-Path $runtimeRoot "$serviceName.pid"
    if (-not (Test-Path -LiteralPath $pidFile)) { continue }
    $serviceProcessId = [int](Get-Content -LiteralPath $pidFile)
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $serviceProcessId" -ErrorAction SilentlyContinue
    $expectedJar = "$serviceName-0.0.1-SNAPSHOT.jar"
    if ($process -and $process.Name -eq 'java.exe' -and $process.CommandLine.Contains($expectedJar)) {
        Stop-Process -Id $serviceProcessId
        Write-Host "$serviceName stopped."
    }
    Remove-Item -LiteralPath $pidFile
}
