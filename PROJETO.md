# SupportBot — Instruções do Projeto

## O que é este projeto

SupportBot é um assistente de suporte técnico com IA. O técnico digita a descrição de um problema, o sistema lê uma base de conhecimento em CSV e envia tudo para uma IA que retorna a solução mais adequada de forma organizada.

Este arquivo contém todas as decisões de arquitetura, stack e fluxo do projeto. Use-o como referência principal durante o desenvolvimento.

---

## Objetivo

Construir uma aplicação web fullstack onde o técnico de suporte:
1. Acessa uma interface estilo chat
2. Digita a descrição de um problema
3. Recebe uma solução estruturada gerada por IA, baseada numa base de conhecimento real

---

## Stack tecnológica

| Camada | Tecnologia | Observação |
|---|---|---|
| Frontend | Angular 17+ | Interface estilo chat com Angular Material |
| Backend | Java 17 + Spring Boot 3 | API REST |
| IA | Groq API | Gratuito — modelo llama-3.1-8b-instant |
| Base de conhecimento | Arquivo CSV | Duas colunas: problema, solucao |
| Banco de dados | SQL Server | Apenas histórico de consultas e feedbacks |
| Container | Docker + docker-compose | Backend + banco juntos |
| CI | GitHub Actions | Roda testes no push para main |

---

## Arquitetura e fluxo

```
Técnico digita o problema
        ↓
    Angular (frontend)
    envia POST /api/consulta
        ↓
    Spring Boot (backend)
    1. Lê o arquivo knowledge.csv inteiro
    2. Monta o prompt com o CSV + problema
    3. Chama a Groq API
    4. Salva a consulta no SQL Server
    5. Retorna a resposta
        ↓
    Groq API
    recebe o CSV como contexto
    identifica a solução mais adequada
    retorna resposta formatada
        ↓
    Angular exibe a resposta no chat
```

### Ponto importante da arquitetura

A IA (Groq) **não acessa o banco de dados diretamente**. Quem faz isso é o Spring Boot. O Groq recebe apenas uma mensagem de texto com o conteúdo do CSV colado dentro do prompt. Isso é simples, eficiente e suficiente para o tamanho da base de conhecimento de suporte técnico.

---

## Base de conhecimento (knowledge.csv)

Arquivo CSV com duas colunas. Deve ficar em `backend/src/main/resources/knowledge.csv`.

```
problema,solucao
"Usuário não consegue conectar na VPN","1. Verificar se o cliente VPN está atualizado\n2. Verificar credenciais\n3. Reiniciar o serviço VPN"
"Computador não liga após queda de energia","1. Verificar cabo de força\n2. Testar a tomada\n3. Verificar fonte de alimentação"
```

O Spring Boot lê esse arquivo a cada consulta e inclui o conteúdo inteiro no prompt enviado ao Groq.

---

## Estrutura de pastas do projeto

```
supportbot/
├── PROJETO.md                         ← este arquivo
├── docker-compose.yml
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/supportbot/
│           │   ├── SupportBotApplication.java
│           │   ├── controller/
│           │   │   └── ConsultaController.java    ← POST /api/consulta
│           │   ├── service/
│           │   │   ├── ConsultaService.java       ← orquestra o fluxo
│           │   │   ├── GroqService.java           ← chama a Groq API
│           │   │   └── KnowledgeService.java      ← lê o CSV
│           │   ├── repository/
│           │   │   └── ConsultaRepository.java
│           │   └── model/
│           │       └── Consulta.java              ← entidade JPA
│           └── resources/
│               ├── application.properties
│               └── knowledge.csv                  ← base de conhecimento
│
└── frontend/
    ├── src/
    │   └── app/
    │       ├── app.component.ts
    │       ├── pages/
    │       │   └── chat/
    │       │       ├── chat.component.ts
    │       │       ├── chat.component.html
    │       │       └── chat.component.scss
    │       └── services/
    │           └── consulta.service.ts            ← HttpClient para o backend
    └── angular.json
```

---

## Backend — detalhes de implementação

### Dependências do pom.xml

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>com.microsoft.sqlserver</groupId>
        <artifactId>mssql-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
</dependencies>
```

### application.properties

```properties
# Servidor
server.port=8080

# Banco de dados
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=supportbot;encrypt=false
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=update

# Groq API
groq.api.url=https://api.groq.com/openai/v1/chat/completions
groq.api.key=${GROQ_API_KEY}
groq.api.model=llama-3.1-8b-instant

# CORS (permitir chamadas do Angular)
spring.web.cors.allowed-origins=http://localhost:4200
```

### Prompt que o Spring Boot monta e envia ao Groq

```
Você é um assistente especializado em suporte técnico de TI.
Use APENAS as soluções abaixo como referência para responder.
Não invente informações que não estejam na base.

=== BASE DE CONHECIMENTO ===
[conteúdo inteiro do knowledge.csv]

=== PROBLEMA RELATADO ===
[texto digitado pelo técnico]

Responda com:
1. Diagnóstico provável
2. Passo a passo da solução
3. O que fazer se não resolver
```

### Endpoint principal

```
POST /api/consulta
Content-Type: application/json

{
  "problema": "usuário não consegue conectar na VPN após atualização"
}

Resposta:
{
  "resposta": "...",
  "consultaId": 1
}
```

---

## Banco de dados — SQL Server

### Tabela de histórico

```sql
CREATE TABLE consultas (
    id          INT PRIMARY KEY IDENTITY,
    problema    TEXT NOT NULL,
    resposta    TEXT NOT NULL,
    util        BIT NULL,
    criado_em   DATETIME DEFAULT GETDATE()
)
```

O campo `util` é atualizado pelo feedback do técnico (👍 ou 👎) após receber a resposta.

### Endpoint de feedback

```
PATCH /api/consulta/{id}/feedback
Content-Type: application/json

{
  "util": true
}
```

---

## Frontend — detalhes de implementação

### Interface

A tela única do SupportBot deve ter:
- Saudação inicial centralizada quando não há mensagens
- Campo de texto na parte inferior para digitar o problema
- Botão enviar ao lado do campo
- Área de conversa exibindo o problema enviado e a resposta da IA
- Indicador de carregamento enquanto aguarda resposta
- Botões 👍 👎 abaixo de cada resposta para feedback

### Tecnologias Angular

- Angular Material para componentes visuais
- ReactiveFormsModule para o campo de texto
- HttpClient para chamadas ao backend
- RxJS para gerenciar o estado de carregamento

---

## Docker

### docker-compose.yml

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - DB_USER=sa
      - DB_PASS=SupportBot@123
      - GROQ_API_KEY=${GROQ_API_KEY}
    depends_on:
      - db

  db:
    image: mcr.microsoft.com/mssql/server:2022-latest
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=SupportBot@123
    ports:
      - "1433:1433"
```

A `GROQ_API_KEY` deve ser definida em um arquivo `.env` local (nunca commitar no GitHub).

---

## GitHub Actions — CI

```yaml
name: CI

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build e testes
        run: mvn test
        working-directory: ./backend
```

---

## Ordem de desenvolvimento recomendada

### Fase 0 — Preparação
- [ ] Criar conta no Groq e gerar API key em console.groq.com
- [ ] Criar repositório no GitHub
- [ ] Criar o arquivo knowledge.csv com 10 a 20 casos reais do suporte
- [ ] Montar a estrutura de pastas do projeto

### Fase 1 — Backend
- [ ] Criar projeto Spring Boot via Spring Initializr
- [ ] Configurar conexão com SQL Server
- [ ] Criar entidade Consulta e repository JPA
- [ ] Implementar KnowledgeService (leitura do CSV)
- [ ] Implementar GroqService (chamada à API)
- [ ] Implementar ConsultaService (orquestração do fluxo)
- [ ] Implementar ConsultaController (endpoints REST)
- [ ] Testar endpoint com Postman ou curl

### Fase 2 — Frontend
- [ ] Criar projeto Angular com Angular Material
- [ ] Criar ConsultaService com HttpClient
- [ ] Implementar tela de chat
- [ ] Conectar com o backend

### Fase 3 — Infraestrutura
- [ ] Criar Dockerfile do backend
- [ ] Criar docker-compose.yml
- [ ] Configurar GitHub Actions
- [ ] Escrever README com instruções e screenshots

---

## Observações importantes

- A `GROQ_API_KEY` nunca deve ser commitada no repositório. Usar variável de ambiente ou arquivo `.env` no `.gitignore`.
- O arquivo `knowledge.csv` deve ser preenchido com casos reais do trabalho de suporte técnico. Quanto mais detalhadas as soluções, melhor a qualidade das respostas da IA.
- O Spring Boot lê o CSV a cada requisição. Se a base crescer muito (milhares de linhas), considerar cache ou leitura na inicialização.
- O Groq foi testado e confirmado com acesso liberado (HTTP 200) no ambiente de desenvolvimento.
