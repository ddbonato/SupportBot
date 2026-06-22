$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$mvn = Join-Path $root ".tools\apache-maven-3.9.6\bin\mvn.cmd"

if (-not (Test-Path $mvn)) {
    Write-Error "Maven não encontrado em .tools. Execute: winget install Apache.Maven ou use o Maven instalado no sistema."
}

$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            Set-Item -Path "env:$($matches[1].Trim())" -Value $matches[2].Trim()
        }
    }
}

if (-not $env:GROQ_API_KEY) {
    $env:GROQ_API_KEY = [Environment]::GetEnvironmentVariable('GROQ_API_KEY', 'User')
}
if (-not $env:GROQ_API_KEY) {
    $env:GROQ_API_KEY = [Environment]::GetEnvironmentVariable('GROQ_API_KEY', 'Machine')
}

if (-not $env:GROQ_API_KEY) {
    Write-Warning "GROQ_API_KEY não definida. Defina em .env ou: `$env:GROQ_API_KEY='sua-chave'"
}

$profile = if ($args -contains "--sqlserver") { "default" } else { "local" }

if ($profile -eq "default") {
    if (-not $env:DB_USER) { $env:DB_USER = "sa" }
    if (-not $env:DB_PASS) { $env:DB_PASS = "SupportBot@123" }
    Write-Host "Perfil: SQL Server (localhost:1433). Certifique-se de que o banco está rodando: docker compose up -d db"
} else {
    Write-Host "Perfil: local (H2 em memória)"
}

Write-Host "Iniciando backend em http://localhost:8080 ..."
Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html"

# Usa o repositório de certificados do Windows (necessário em redes corporativas)
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"

& $mvn -f (Join-Path $backend "pom.xml") spring-boot:run "-Dspring-boot.run.profiles=$profile" "-Dspring-boot.run.jvmArguments=-Djavax.net.ssl.trustStoreType=Windows-ROOT"
