# SupportWizard — Instruções do Projeto

## O que é este projeto

SupportWizard é um assistente de suporte técnico com IA para a equipe de suporte da ICI. O técnico descreve um problema no chat; o sistema filtra a base de conhecimento em CSV, monta um prompt e consulta um modelo de linguagem local (Ollama) para devolver a solução mais adequada.

**Este arquivo é a referência principal de arquitetura.** Leia-o antes de qualquer implementação.

---

## Identificadores do projeto

| Item | Valor |
|---|---|
| Pacote Java | `com.supportwizard` |
| Classe principal | `SupportWizardApplication` |
| Artifact Maven | `supportwizard-backend` |
| Projeto Angular | `supportwizard` |
| Build frontend | `frontend/dist/supportwizard/browser` |
| Banco SQL Server | `databaseName=supportwizard` |
| Banco H2 (local) | `jdbc:h2:mem:supportwizard` |
| Senha padrão SQL Server (dev) | `SupportWizard@123` |

---

## Stack tecnológica

| Camada | Tecnologia | Observação |
|---|---|---|
| Frontend | Angular 17+ | Standalone components, Angular Material, ngx-markdown |
| Backend | Java 17 + Spring Boot 3.2.5 | API REST + servidor de arquivos estáticos |
| IA | **Ollama** (local) | Modelo `qwen2.5:7b` em `http://172.16.24.85:11434` |
| Base de conhecimento | Arquivo CSV | Colunas `problema,solucao` — ~30 entradas ICI |
| Banco de dados | SQL Server (prod) / H2 (dev local) | Histórico de consultas e feedback |
| Build | Maven Wrapper 3.9.6 | `backend/mvnw.cmd` — não exige Maven global |
| Container | Docker Compose | Apenas SQL Server (`docker-compose.yml`) |

> **Histórico:** o projeto iniciou com Groq API (`llama-3.1-8b-instant`). Foi migrado para Ollama local. Não há mais `GroqService` no código.

---

## Arquitetura e fluxo

```
Técnico digita o problema
        ↓
    Angular (chat) — POST /api/consulta  (URL relativa, mesmo servidor em produção)
        ↓
    ConsultaController → ConsultaService
        1. KnowledgeService filtra linhas do CSV por palavras-chave da pergunta
        2. Monta prompt com trechos filtrados + instrução de copiar exatamente
        3. OllamaService chama POST /api/generate no Ollama
        4. Salva consulta no banco (H2 ou SQL Server)
        5. Retorna { resposta, consultaId }
        ↓
    Angular exibe resposta (Markdown) + botões 👍 👎
        ↓
    PATCH /api/consulta/{id}/feedback
```

### Pontos importantes

- A IA **não acessa o banco** nem o CSV diretamente — o Spring Boot monta o prompt.
- O `KnowledgeService` **filtra** o CSV por palavras-chave extraídas da pergunta (não envia o CSV inteiro quando há matches). Se nenhuma linha pontuar, envia todas.
- O prompt instrui o modelo a **copiar exatamente** o trecho relevante (senhas, fatos simples, etc.).
- Em produção (JAR único), frontend e backend rodam no **mesmo processo** na porta 8080.

---

## Base de conhecimento (knowledge.csv)

### Formato

```csv
problema,solucao
"Descrição do problema","Solução ou informação (pode ter \\n para quebras de linha)"
```

### Localização dos arquivos

| Arquivo | Uso |
|---|---|
| `knowledge.csv` (raiz do repo) | Cópia de trabalho / referência |
| `backend/src/main/resources/knowledge.csv` | Embutido no JAR (`classpath:knowledge.csv`) |
| Caminho externo (opcional) | Configurável via `knowledge.csv.path` |

### Propriedade configurável

```properties
# Padrão — lê do classpath (dentro do JAR)
knowledge.csv.path=classpath:knowledge.csv

# Produção — CSV externo editável sem recompilar
knowledge.csv.path=C:\SupportWizard\knowledge.csv
```

O `KnowledgeService` injeta `@Value("${knowledge.csv.path}")`:
- Se começa com `classpath:` → `ResourceLoader`
- Caso contrário → `Files.readString(Path.of(caminho))`

### Busca por palavras-chave

1. Extrai palavras da pergunta (remove stop words em português, mínimo 3 caracteres)
2. Pontua cada linha: +3 se palavra aparece em `problema`, +2 se em `solucao`
3. Retorna linhas com pontuação > 0, ordenadas por relevância
4. Log de debug temporário quando a pergunta contém `"qual a senha da bios"` (verificar linhas retornadas)

**Exemplo validado:** pergunta `"qual a senha da BIOS"` → retorna linha *"Senhas padrão dos sistemas ICI"* → resposta `BIOS: cmos98`.

---

## Estrutura de pastas

```
SupportWizard/
├── PROJETO.md
├── SETUP.md
├── knowledge.csv                    ← base de conhecimento (cópia na raiz)
├── docker-compose.yml               ← só SQL Server
├── .env.example
├── scripts/
│   └── run-backend.ps1
│
├── backend/
│   ├── mvnw / mvnw.cmd              ← Maven Wrapper
│   ├── .mvn/wrapper/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/supportwizard/
│       │   ├── SupportWizardApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java          ← CORS /api/** para 172.16.x.x
│       │   │   ├── OllamaConfig.java
│       │   │   ├── OllamaProperties.java
│       │   │   └── SpaResourceConfig.java   ← serve Angular + fallback index.html
│       │   ├── controller/
│       │   │   ├── ConsultaController.java  ← POST /api/consulta, PATCH feedback
│       │   │   └── GlobalExceptionHandler.java
│       │   ├── service/
│       │   │   ├── ConsultaService.java     ← orquestra fluxo + monta prompt
│       │   │   ├── KnowledgeService.java    ← lê/filtra CSV
│       │   │   └── OllamaService.java       ← chama Ollama /api/generate
│       │   ├── dto/  (ConsultaRequest, ConsultaResponse, FeedbackRequest)
│       │   ├── model/Consulta.java
│       │   └── repository/ConsultaRepository.java
│       └── resources/
│           ├── application.properties
│           ├── application-local.properties
│           └── knowledge.csv
│
└── frontend/
    ├── angular.json                   ← projeto "supportwizard", dist/supportwizard
    └── src/app/
        ├── pages/chat/                ← interface de chat
        └── services/consulta.service.ts ← POST /api/consulta (URL relativa)
```

---

## Backend — configuração

### application.properties (perfil default — SQL Server)

```properties
server.port=8080

spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=supportwizard;encrypt=false
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=update

ollama.api.url=http://172.16.24.85:11434/api/generate
ollama.api.model=qwen2.5:7b

spring.web.cors.allowed-origin-patterns=http://localhost:*,http://127.0.0.1:*,http://172.16.*.*:*

knowledge.csv.path=classpath:knowledge.csv
```

### application-local.properties (perfil `local` — H2 em memória)

Mesmas configs de Ollama, CORS e knowledge. Substitui datasource por H2:

```properties
spring.datasource.url=jdbc:h2:mem:supportwizard;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

### Prompt enviado ao Ollama

```
Você é um assistente especializado em suporte técnico de TI.
Use APENAS as soluções abaixo como referência para responder.
Não invente informações que não estejam na base.

Copy the answer EXACTLY as written. Do not explain, do not add steps, do not invent procedures.
If the information is a simple fact like a password, respond with just that fact.

=== BASE DE CONHECIMENTO ===
[linhas filtradas do CSV]

=== PROBLEMA RELATADO ===
[texto do técnico]

Responda copiando exatamente o trecho relevante da base de conhecimento, sem reformular.
```

### Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/consulta` | Envia problema, retorna resposta da IA |
| PATCH | `/api/consulta/{id}/feedback` | Registra feedback (`{ "util": true/false }`) |
| GET | `/swagger-ui.html` | Documentação OpenAPI |
| GET | `/` | Frontend Angular (quando empacotado no JAR) |

---

## Build — JAR único (backend + frontend)

O `pom.xml` usa `frontend-maven-plugin` na fase `prepare-package`:

1. `npm install --legacy-peer-deps`
2. `npm run build` (Angular → `frontend/dist/supportwizard/browser`)
3. Copia para `target/classes/static`
4. Spring Boot repackage → JAR executável

```powershell
cd backend
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd clean package -DskipTests
java -jar target/supportwizard-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

Acesse **http://localhost:8080** — frontend e API no mesmo servidor.

Propriedade `skip.frontend.build=true` pula o build Angular (útil para compilar só o backend).

### Servir SPA (SpaResourceConfig)

- Arquivos estáticos em `classpath:/static/`
- Rotas desconhecidas → `index.html` (roteamento Angular)
- Exclui `/api/**`, `/swagger-ui/**`, `/v3/api-docs/**`

---

## Banco de dados

### Entidade Consulta (JPA)

| Campo | Tipo | Descrição |
|---|---|---|
| id | Long | PK auto-increment |
| problema | String | Texto enviado pelo técnico |
| resposta | String | Resposta da IA |
| util | Boolean | Feedback (null = sem feedback) |
| criadoEm | LocalDateTime | Timestamp da consulta |

---

## Frontend

- Componente standalone `ChatComponent` com Angular Material
- Respostas renderizadas com **ngx-markdown**
- API via URL relativa `/api/consulta` (funciona no JAR único e com proxy em dev)
- Feedback 👍 👎 implementado (`PATCH /api/consulta/{id}/feedback`)
- Tema visual ICI (imagens em `frontend/src/assets/`)

### Desenvolvimento separado

```powershell
# Terminal 1 — backend (perfil local, H2)
cd scripts
.\run-backend.ps1

# Terminal 2 — frontend
cd frontend
npm install --legacy-peer-deps
npm start
# → http://localhost:4200
```

---

## Docker

`docker-compose.yml` sobe **apenas o SQL Server**:

```yaml
services:
  db:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      SA_PASSWORD: SupportWizard@123
    ports:
      - "1433:1433"
```

Subir: `docker compose up -d db`

Backend com SQL Server: `.\run-backend.ps1 --sqlserver` (define `DB_USER`/`DB_PASS` se ausentes).

---

## Regras de código (resumo)

- Injeção de dependência **via construtor**
- `@Service`, `@RestController`, `@Repository` nas classes corretas
- WebClient para chamadas HTTP ao Ollama (não RestTemplate)
- Configurações sensíveis via variáveis de ambiente — nunca hardcoded no repo
- Componentes Angular standalone; HttpClient apenas em Services
- Tratamento de erro com `@ControllerAdvice`

---

## Estado atual (jun/2026)

### Concluído
- Backend Spring Boot completo com Ollama
- Frontend Angular com chat, Markdown e feedback
- Renomeação completa SupportBot → SupportWizard
- Busca por palavras-chave no KnowledgeService
- CSV configurável (`knowledge.csv.path`)
- JAR único com frontend embutido
- Maven Wrapper
- CORS para rede interna 172.16.x.x
- Perfil local (H2) e perfil SQL Server

### Pendente / melhorias futuras
- Dockerizar o backend no `docker-compose.yml`
- GitHub Actions (CI)
- Atualizar `run-backend.ps1` para usar `mvnw` em vez de `.tools/apache-maven`
- Atualizar `.cursorrules` (ainda menciona Groq)
- Remover log de debug temporário do KnowledgeService quando estável
- Sincronizar automaticamente `knowledge.csv` da raiz para `backend/src/main/resources/`

---

## Observações importantes

- O servidor Ollama (`172.16.24.85:11434`) precisa estar acessível na rede. A primeira resposta pode levar ~30–60 s.
- Após editar `backend/src/main/resources/knowledge.csv`, é necessário **rebuild** do backend (ou usar `knowledge.csv.path` externo).
- Em redes corporativas, usar `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` para builds Maven/npm via plugin.
- O `npm install` no build Maven usa `--legacy-peer-deps` (conflito `marked` vs `ngx-markdown`).
