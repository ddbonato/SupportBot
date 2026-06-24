# SupportBot — Guia de Configuração

## Pré-requisitos

Antes de começar, instale:

- **Java 17+** — https://adoptium.net (baixe a versão Temurin 17)
- **Node.js 18+** — https://nodejs.org (baixe a versão LTS)
- **Git** — https://git-scm.com

Para confirmar que está tudo instalado, abra o PowerShell e rode:
```powershell
java -version
node -v
npm -v
git --version
```

---

## 1. Obter a GROQ_API_KEY

1. Acesse **https://console.groq.com**
2. Crie uma conta gratuita
3. Vá em **API Keys → Create API Key**
4. Copie a chave gerada — começa com `gsk_...`

---

## 2. Configurar a chave no projeto

Crie um arquivo `.env` na raiz do projeto:

```
GROQ_API_KEY=gsk_suachaveaqui
```

> O arquivo `.env` está no `.gitignore` — nunca será enviado ao GitHub.

---

## 3. Rodar o backend

Abra um terminal PowerShell na pasta `scripts/`:

```powershell
cd scripts
$env:GROQ_API_KEY = "gsk_suachaveaqui"
.\run-backend.ps1
```

Aguarde aparecer:
```
Started SupportBotApplication in X seconds
```

O backend estará disponível em **http://localhost:8080**
Swagger UI em **http://localhost:8080/swagger-ui.html**

---

## 4. Rodar o frontend

Abra **outro terminal** PowerShell na pasta `frontend/`:

```powershell
cd frontend
npm install
npm start
```

Aguarde aparecer:
```
✔ Compiled successfully
```

Acesse **http://localhost:4200** no navegador.

---

## 5. Testar que está funcionando

Com backend e frontend rodando, acesse http://localhost:4200 e digite um problema no campo de texto. A IA deve responder em alguns segundos.

Para testar só o backend via PowerShell:

```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Invoke-RestMethod -Uri http://localhost:8080/api/consulta `
  -Method POST -ContentType "application/json" `
  -Body '{"problema":"usuario nao consegue conectar na VPN"}'
```

Resposta esperada:
```json
{
  "resposta": "...",
  "consultaId": 1
}
```

---

## 6. Adicionar casos à base de conhecimento

Edite o arquivo `backend/src/main/resources/knowledge.csv` com os problemas e soluções do seu ambiente:

```
problema,solucao
"Descrição do problema","1. Passo um\n2. Passo dois\n3. Passo três"
```

Salve o arquivo e reinicie o backend para carregar as alterações.

---

## Problemas comuns

**Porta 8080 em uso:**
```powershell
netstat -ano | findstr :8080
taskkill /PID <numero_do_pid> /F
```

**Acentos errados no PowerShell:**
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

**Erro de certificado SSL (rede corporativa):**
Teste fora da rede corporativa ou pelo hotspot do celular. Se confirmar que é o proxy da empresa, solicite ao time de TI a importação do certificado no truststore do Java.

---

## Estado atual do projeto

- ✅ Fase 0 — Planejamento e estrutura
- ✅ Fase 1 — Backend Spring Boot completo
- ✅ Fase 2 — Frontend Angular com interface de chat
- ⬜ Fase 3 — Docker + GitHub Actions

**Próximos passos pendentes:**
- Renderizar Markdown na resposta da IA (ngx-markdown)
- Botões de feedback 👍 👎 (PATCH /api/consulta/{id}/feedback)
- Dockerizar o projeto
- Configurar GitHub Actions
