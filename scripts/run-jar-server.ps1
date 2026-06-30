$ErrorActionPreference = "Stop"

# Pasta onde estão o JAR e o knowledge.csv (ajuste se necessário)
$AppDir = if ($args -contains "--dir") {
    $idx = [array]::IndexOf($args, "--dir")
    $args[$idx + 1]
} else {
    "C:\SupportWizard"
}

$JarName = "supportwizard-backend-0.0.1-SNAPSHOT.jar"
$JarPath = Join-Path $AppDir $JarName
$KnowledgeCsv = Join-Path $AppDir "knowledge.csv"
$OllamaUrl = if ($env:OLLAMA_URL) { $env:OLLAMA_URL } else { "http://localhost:11434/api/generate" }

if (-not (Test-Path $JarPath)) {
    Write-Error "JAR não encontrado: $JarPath"
}

if (-not (Test-Path $KnowledgeCsv)) {
    Write-Warning "Arquivo knowledge.csv não encontrado em: $KnowledgeCsv"
    Write-Warning "Copie knowledge.csv para a pasta do JAR antes de iniciar."
}

New-Item -ItemType Directory -Force -Path (Join-Path $AppDir "data") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $AppDir "logs") | Out-Null

Set-Location $AppDir

Write-Host "Pasta: $AppDir"
Write-Host "JAR:   $JarPath"
Write-Host "CSV:   $KnowledgeCsv"
Write-Host "Ollama: $OllamaUrl"
Write-Host ""
Write-Host "Acesse: http://localhost:8080  ou  http://<IP-DO-SERVIDOR>:8080"
Write-Host "Login: senhas em `chat.senha` (chat) e `knowledge.senha` (editor)"
Write-Host "Log:  $AppDir\logs\supportwizard.log"
Write-Host ""

java -jar $JarPath `
  --spring.profiles.active=server `
  --knowledge.csv.path="$KnowledgeCsv" `
  --ollama.api.url="$OllamaUrl"
