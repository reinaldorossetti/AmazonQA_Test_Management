# Tests Guide — backend-kotlin

Este documento resume os testes implementados no backend e como executá-los no terminal.

## Stack de testes

- JUnit 5
- Rest-Assured (testes de API reais, sem mock de HTTP)
- DataFaker (dados dinâmicos para cenários)
- Spring Boot Test
- Testcontainers (smoke test de PostgreSQL)
- Ktlint + Detekt (qualidade estática)
- Allure Report (relatório visual de execução)

## O que foi testado

### 1) Testes de integração de API por feature

Pacote: `src/test/kotlin/com/amazonqa/api`

- `AuthAndSelfApiIntegrationTest.kt`
	- Login, refresh, logout
	- `GET /users/me` e `PATCH /users/me/preferences`

- `AdminUserAccessApiIntegrationTest.kt`
	- CRUD de usuários admin
	- Papéis/permissões globais e escopadas
	- Cenários de autorização (403)

- `ProjectRequirementApiIntegrationTest.kt`
	- CRUD de projetos com restore
	- CRUD de requisitos, import, coverage e traceability

- `SuiteTestCaseApiIntegrationTest.kt`
	- CRUD de suites com restore
	- CRUD de test cases, versionamento, bulk edit e search

- `PlanningExecutionApiIntegrationTest.kt`
	- CRUD de builds/plans
	- Criação de runs
	- Regras de negócio de execução (evidência obrigatória em FAILED, build fechado imutável)

- `DefectReportingApiIntegrationTest.kt`
	- CRUD de defeitos
	- Configuração/verificação de integração Jira
	- Métricas e jobs de relatório

### 2) Testes de contrato, segurança e regras

- `EndpointContractCoverageTest.kt`
	- Garante que todos endpoints v1 esperados estão mapeados

- `OpenApiValidationTest.kt`
	- Valida parsing do OpenAPI (`docs/api/openapi.yaml`)

- `SecurityPolicyTest.kt`
	- Valida políticas de acesso e bloqueios de mutação

- `BusinessRulesServiceTest.kt`
	- Regras críticas de domínio (last admin, draft-only delete, etc.)

- `PostgresContainerSmokeTest.kt`
	- Smoke do container PostgreSQL via Testcontainers (depende de Docker)

## Como rodar os testes no terminal

No diretório `backend-kotlin`:

- Rodar todos os testes:
	- `./gradlew.bat test`

- Rodar apenas os testes de API:
	- `./gradlew.bat test --tests "com.amazonqa.api.*"`

- Rodar task dedicada de testes de API:
	- `./gradlew.bat apiTest`

- Gerar relatório Allure dos testes de API:
	- `./gradlew.bat apiAllureReport`

- Rodar suíte completa de qualidade:
	- `./gradlew.bat clean test ktlintCheck detekt`

- Rodar só análise estática:
	- `./gradlew.bat ktlintCheck detekt`

## Pré-requisitos

- Java 23 (toolchain do projeto)
- Gradle Wrapper (`gradlew.bat`)
- Docker (opcional, necessário para `PostgresContainerSmokeTest`)

## Tokens de autenticação usados nos testes

Via header `Authorization: Bearer <token>`:

- `admin-token`
- `leader-token`
- `tester-token`
- `guest-token`

## Resultado esperado

Ao final da execução sem falhas:

- `BUILD SUCCESSFUL`

Se algum teste falhar, consulte o relatório HTML gerado em:

- `build/reports/tests/test/index.html`

## Allure Reports (API)

### Task principal

- `apiAllureReport`

Essa task executa automaticamente:

1. Limpeza dos resultados antigos (`cleanAllureResults`)
2. Execução dos testes de API (`apiTest`)
3. Geração do relatório (`allureReport`)

### Diretórios gerados

- Resultados brutos Allure:
	- `build/allure-results`
- Relatório HTML Allure:
	- `build/reports/allure-report/allureReport/index.html`

### Publicação automática no GitHub Pages (CI)

O workflow `.github/workflows/backend-kotlin-ci.yml` publica os relatórios no branch `gh-pages` quando há `push` na branch `main`.

- Job de build/testes gera `apiAllureReport`
- Artefato Allure também é salvo no workflow (`backend-kotlin-allure-report`)
- Job de deploy publica para `gh-pages` via `peaceiris/actions-gh-pages`
- A página inicial publicada é um hub HTML com 2 opções:
	- `API` → `tests-report/api/index.html` (Allure)
	- `Unitários` → `tests-report/unit/index.html` (Gradle Test Report)
- URL pública do hub: https://reinaldorossetti.github.io/AmazonQA_Test_Management/tests-report/index.html

Para funcionar na primeira vez:

1. Vá em **Settings > Pages** do repositório
2. Em **Build and deployment / Source**, selecione branch **gh-pages** (root)

### Abrir relatório no navegador

Após rodar `./gradlew.bat apiAllureReport`, abra:

- `build/reports/allure-report/allureReport/index.html`

### Servir o relatório com servidor local do Allure

- `./gradlew.bat allureServe`

## Comandos Allure (copiar e colar)

### Opção A — Executando dentro de `backend-kotlin`

No PowerShell/CMD:

1. Entrar na pasta do backend:

- `cd .\backend-kotlin`

2. Gerar resultados + relatório de API:

- `.\gradlew.bat apiAllureReport`

3. Subir servidor local do Allure:

- `.\gradlew.bat allureServe`

### Opção B — Executando da raiz do repositório

Se você estiver em `AmazonQA_Test_Management` (raiz), use `-p` para apontar o projeto Gradle:

- `.\backend-kotlin\gradlew.bat -p .\backend-kotlin apiAllureReport`
- `.\backend-kotlin\gradlew.bat -p .\backend-kotlin allureServe`

### Observação importante

Se rodar `allureServe` na pasta errada, o Gradle retorna erro parecido com:

- `Directory '...AmazonQA_Test_Management' does not contain a Gradle build.`

Nesse caso, use a Opção A (entrar em `backend-kotlin`) ou a Opção B (usar `-p .\\backend-kotlin`).
