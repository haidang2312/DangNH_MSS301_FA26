param(
    [int]$CustomerPort = 8081,
    [int]$MongoPort = 27017,
    [string]$JavaHome = $env:JAVA_HOME,
    [ValidateSet('customer-service', 'movie-service', 'booking-service', 'api-gateway')]
    [string[]]$ServiceNames = @('customer-service', 'movie-service', 'booking-service', 'api-gateway')
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$runtimeRoot = Join-Path $projectRoot '.runtime'
New-Item -ItemType Directory -Force -Path $runtimeRoot | Out-Null
$javaExecutable = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { (Get-Command java).Source }
$services = @(
    @{ Name = 'customer-service'; Args = @("--server.port=$CustomerPort") },
    @{ Name = 'movie-service'; Args = @("--spring.mongodb.uri=mongodb://root:password@localhost:$MongoPort/cinema_movie?authSource=admin") },
    @{ Name = 'booking-service'; Args = @() },
    @{ Name = 'api-gateway'; Args = @("--services.customer.url=http://localhost:$CustomerPort") }
)
foreach ($service in $services) {
    $serviceName = $service.Name
    if ($serviceName -notin $ServiceNames) { continue }
    $pidFile = Join-Path $runtimeRoot "$serviceName.pid"
    if (Test-Path -LiteralPath $pidFile) {
        $existingProcess = Get-Process -Id ([int](Get-Content -LiteralPath $pidFile)) -ErrorAction SilentlyContinue
        if ($existingProcess) { throw "$serviceName is already running. Run scripts/stop.ps1 first." }
    }
    $serviceRoot = Join-Path $projectRoot $serviceName
    $jar = Join-Path $serviceRoot "target/$serviceName-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Build $serviceName before starting it." }
    $arguments = @('-Xms64m', '-Xmx384m', '-Duser.timezone=Asia/Ho_Chi_Minh', '-jar', ('"' + $jar + '"')) + $service.Args
    $process = Start-Process -FilePath $javaExecutable -ArgumentList $arguments -WorkingDirectory $serviceRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtimeRoot "$serviceName.log") -RedirectStandardError (Join-Path $runtimeRoot "$serviceName.err.log")
    $process.Id | Set-Content -LiteralPath $pidFile
    Write-Host "$serviceName started (PID $($process.Id)). Logs: .runtime/$serviceName.log"
}
Write-Host 'Gateway: http://localhost:9000. Allow services to finish starting.'
