# SupportWizard — Guia de Configuração

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| Java | 17+ | JDK Temurin recomendado |
| Node.js | 18+ | Necessário para build do frontend |
| Git | qualquer | |
| Ollama | acessível na rede | `http://172.16.24.85:11434` com modelo `qwen2.5:7b` |
| Docker | opcional | Apenas se usar SQL Server local via compose |

**Maven global não é obrigatório** — o projeto inclui Maven Wrapper em `backend/mvnw.cmd`.

Verificar instalação:

```powershell
java -version
node -v
npm -v
git --version
```

Testar Ollama:

```powershell
Invoke-RestMethod -Uri "http://172.16.24.85:11434/api/tags" -TimeoutSec 5
# Deve listar "qwen2.5:7b" entre os modelos
```

---

## Modos de execução

| Modo | Quando usar | URL |
|---|---|---|
| **Dev separado** | Desenvolvimento do frontend com hot-reload | Frontend `http://localhost:4200`, API `http://localhost:8080` |
| **JAR único** | Deploy / teste integrado / produção | Tudo em `http://localhost:8080` |

---

## Opção A — Desenvolvimento (backend + frontend separados)

### 1. Subir o backend

```powershell
cd scripts
.\run-backend.ps1
```

Por padrão usa o perfil **`local`** (H2 em memória, sem Docker).

Para SQL Server:

```powershell
docker compose up -d db          # na raiz do projeto
.\run-backend.ps1 --sqlserver
```

Aguarde no console:

```
Started SupportWizardApplication in X seconds
```

- API: **http://localhost:8080**
- Swagger: **http://localhost:8080/swagger-ui.html**

### 2. Subir o frontend

Em outro terminal:

```powershell
cd frontend
npm install --legacy-peer-deps
npm start
```

Acesse **http://localhost:4200**.

> O frontend chama `/api/consulta` na porta 8080. Em dev separado, o Angular faz a requisição para `http://localhost:8080` via URL relativa — certifique-se de que o backend está rodando.

---

## Opção B — JAR único (backend + frontend embutidos)

### 1. Gerar o JAR

```powershell
cd backend
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd clean package -DskipTests
```

O build executa automaticamente:
1. `npm install --legacy-peer-deps` e `npm run build` no frontend
2. Copia `frontend/dist/supportwizard/browser` para dentro do JAR
3. Gera `target/supportwizard-backend-0.0.1-SNAPSHOT.jar`

Para compilar só o backend (sem frontend):

```powershell
.\mvnw.cmd compile "-Dskip.frontend.build=true"
```

### 2. Executar

```powershell
# Perfil local (H2)
java -jar target/supportwizard-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local

# Perfil SQL Server
$env:DB_USER = "sa"
$env:DB_PASS = "SupportWizard@123"
java -jar target/supportwizard-backend-0.0.1-SNAPSHOT.jar
```

Acesse **http://localhost:8080** — interface de chat e API no mesmo servidor.

### 3. Deploy em servidor Windows

Copie para a mesma pasta (ex.: `C:\SupportWizard`):

- `supportwizard-backend-0.0.1-SNAPSHOT.jar`
- `knowledge.csv` (obrigatório para chat e editor)
- `scripts\run-jar-server.ps1` (opcional)

Execute na pasta do JAR:

```powershell
cd C:\SupportWizard
.\run-jar-server.ps1
```

Ou manualmente (use o perfil **server**, não `local`):

```powershell
cd C:\SupportWizard
java -jar supportwizard-backend-0.0.1-SNAPSHOT.jar `
  --spring.profiles.active=server `
  --knowledge.csv.path=C:\SupportWizard\knowledge.csv `
  --ollama.api.url=http://localhost:11434/api/generate
```

Acesse `http://<IP-DO-SERVIDOR>:8080` (porta **8080**). O login vem antes do chat.

**Checklist se não abrir:**

1. Java 17+: `java -version`
2. JAR subiu sem erro no console (ou veja `logs\supportwizard.log`)
3. Teste no próprio servidor: `http://localhost:8080` e `http://localhost:8080/swagger-ui.html`
4. Libere a porta 8080 no Firewall do Windows (rede interna)
5. `knowledge.csv` existe em `C:\SupportWizard\`
6. Ollama rodando no servidor se usar `localhost:11434`
7. Gere o JAR de novo após alterações: `.\mvnw.cmd clean package -DskipTests`

---

## Variáveis de ambiente

Crie `.env` na raiz (modelo em `.env.example`):

```env
DB_USER=sa
DB_PASS=SupportWizard@123
```

O script `run-backend.ps1` carrega `.env` automaticamente.

### Banco de dados

| Perfil | Banco | Como ativar |
|---|---|---|
| `local` | H2 em memória (`supportwizard`) | Padrão do `run-backend.ps1` |
| default | SQL Server (`supportwizard`) | `.\run-backend.ps1 --sqlserver` + Docker |

### Base de conhecimento externa

Para editar o CSV sem recompilar o JAR:

```properties
# application.properties ou variável de ambiente
knowledge.csv.path=C:\SupportWizard\knowledge.csv
```

Padrão: `classpath:knowledge.csv` (arquivo em `backend/src/main/resources/`).

---

## Testar a API

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Invoke-RestMethod -Uri "http://localhost:8080/api/consulta" `
  -Method POST -ContentType "application/json; charset=utf-8" `
  -Body '{"problema":"qual a senha da BIOS"}' `
  -TimeoutSec 120
```

Resposta esperada (exemplo):

```json
{
  "resposta": "BIOS: cmos98",
  "consultaId": 1
}
```

> Respostas do Ollama podem levar até 1 minuto na primeira consulta.

---

## Editar a base de conhecimento

### Arquivo no classpath (padrão)

Edite `backend/src/main/resources/knowledge.csv` (ou `knowledge.csv` na raiz e copie para lá).

Formato:

```csv
problema,solucao
"Descrição do problema","1. Passo um\n2. Passo dois"
```

Após editar:
- **Dev (`spring-boot:run`)**: reinicie o backend
- **JAR**: recompile com `.\mvnw.cmd package` ou use `knowledge.csv.path` externo

### Arquivo externo (recomendado em produção)

1. Copie o CSV para ex.: `C:\SupportWizard\knowledge.csv`
2. Configure `knowledge.csv.path=C:\SupportWizard\knowledge.csv`
3. Reinicie o backend — **sem recompilar**

---

## Problemas comuns

### "Verifique se o backend está rodando" (frontend)

- Backend não está na porta 8080
- Ollama inacessível → API retorna 500
- CSV desatualizado no classpath (faltando entradas) → rebuild necessário

### Porta 8080 em uso

```powershell
Get-NetTCPConnection -LocalPort 8080 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### Erro SSL no build Maven (rede corporativa)

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
```

### `npm install` falha com ERESOLVE

Use `--legacy-peer-deps` (já configurado no `pom.xml` para builds Maven).

### Acentos errados no PowerShell

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

### CSV com poucas entradas após deploy

Confirme que `backend/src/main/resources/knowledge.csv` tem ~30 linhas (não a versão antiga com 5 entradas). Rode `mvn clean package` para atualizar o JAR.

### Senha do SQL Server após renomeação

A senha padrão de desenvolvimento é `SupportWizard@123` (antes era `SupportBot@123`). Se o container foi criado com a senha antiga, recrie:

```powershell
docker compose down -v
docker compose up -d db
```

---

## Estado atual do projeto (jun/2026)

### Concluído
- ✅ Backend Spring Boot 3.2.5 (`com.supportwizard`)
- ✅ IA via Ollama (`qwen2.5:7b`)
- ✅ Frontend Angular 17 com chat, Markdown (ngx-markdown) e feedback 👍 👎
- ✅ KnowledgeService com busca por palavras-chave
- ✅ `knowledge.csv.path` configurável (classpath ou arquivo externo)
- ✅ JAR único com frontend embutido (`mvnw package`)
- ✅ Maven Wrapper (`backend/mvnw.cmd`)
- ✅ SPA servida na raiz `/` com `/api/**` preservado
- ✅ CORS para rede interna `172.16.x.x`
- ✅ Identificadores renomeados (SupportBot → SupportWizard)
- ✅ ~30 entradas reais ICI no `knowledge.csv`

### Pendente
- ⬜ Dockerizar backend no `docker-compose.yml`
- ⬜ GitHub Actions (CI)
- ⬜ Atualizar `run-backend.ps1` para usar `mvnw` nativo
- ⬜ Atualizar `.cursorrules` (ainda referencia Groq)
