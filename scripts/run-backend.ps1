$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$frontend = Join-Path $root "frontend"
$mvnw = Join-Path $backend "mvnw.cmd"
$frontendDist = Join-Path $frontend "dist\supportwizard\browser"
$staticDir = Join-Path $backend "target\classes\static"

$skipFrontend = $args -contains "--skip-frontend"
$profile = if ($args -contains "--sqlserver") { "default" } else { "local" }

$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
}

if ($profile -eq "default") {
    if (-not $env:DB_USER) { $env:DB_USER = "sa" }
    if (-not $env:DB_PASS) { $env:DB_PASS = "SupportWizard@123" }
    Write-Host "Perfil: SQL Server (localhost:1433). Certifique-se de que o banco está rodando: docker compose up -d db"
} else {
    Write-Host "Perfil: local (H2 em memória)"
}

if (-not $skipFrontend) {
    Write-Host "Compilando frontend..."
    Push-Location $frontend
    npm run build
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        Write-Error "Falha ao compilar o frontend."
    }
    Pop-Location

    if (-not (Test-Path $frontendDist)) {
        Write-Error "Build do frontend não encontrado em: $frontendDist"
    }

    Write-Host "Copiando frontend para o backend..."
    if (Test-Path $staticDir) {
        Remove-Item -Path (Join-Path $staticDir "*") -Recurse -Force
    } else {
        New-Item -ItemType Directory -Force -Path $staticDir | Out-Null
    }
    Copy-Item -Path (Join-Path $frontendDist "*") -Destination $staticDir -Recurse -Force
} else {
    Write-Host "Pulando build do frontend (--skip-frontend)."
}

Write-Host "Iniciando backend em http://localhost:8080 ..."
Write-Host "App: http://localhost:8080/admin"
Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html"

$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"

& $mvnw -f (Join-Path $backend "pom.xml") spring-boot:run "-Dspring-boot.run.profiles=$profile" "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStoreType=Windows-ROOT"
