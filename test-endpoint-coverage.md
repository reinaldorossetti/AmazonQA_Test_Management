# Cobertura de Testes por Endpoint (API v1)

_Data da verificação:_ 2026-04-12  
_Repositório:_ `AmazonQA_Test_Management`  
_Escopo:_ Endpoints dos controllers em `backend-kotlin/src/main/kotlin/com/amazonqa/api/v1`

## Resumo executivo

- **Total de endpoints mapeados:** `76`
- **Endpoints cobertos por testes:** `76`
- **Endpoints não cobertos:** `0`
- **Cobertura atual:** **100%**

## Como a cobertura foi medida

A cobertura foi calculada cruzando:

1. Rotas anotadas nos controllers (`@GetMapping`, `@PostMapping`, `@PatchMapping`, `@DeleteMapping` + `@RequestMapping` de classe)
2. Chamadas HTTP feitas nos testes em `backend-kotlin/src/test/kotlin/com/amazonqa/api/*.kt`
3. Matching de rotas dinâmicas por segmentos (ex.: `{userId}` casa com `$userId` nos testes)

Além disso, foi validado o contrato de mapeamento com:

- `com.amazonqa.EndpointContractCoverageTest`

## Cobertura por controller

| Controller | Endpoints | Cobertos | Não cobertos |
|---|---:|---:|---:|
| `AccessManagementController.kt` | 8 | 8 | 0 |
| `AdminUserController.kt` | 4 | 4 | 0 |
| `AuthController.kt` | 3 | 3 | 0 |
| `BuildAndPlanController.kt` | 10 | 10 | 0 |
| `DefectAndReportingController.kt` | 14 | 14 | 0 |
| `ExecutionController.kt` | 2 | 2 | 0 |
| `ProjectController.kt` | 4 | 4 | 0 |
| `RequirementController.kt` | 9 | 9 | 0 |
| `SuiteAndTestCaseController.kt` | 15 | 15 | 0 |
| `UserRegistrationController.kt` | 5 | 5 | 0 |
| `UserSelfController.kt` | 2 | 2 | 0 |

## Suites de teste que exercitam os endpoints

- `AuthAndSelfApiIntegrationTest.kt`
- `AdminUserAccessApiIntegrationTest.kt`
- `ProjectRequirementApiIntegrationTest.kt`
- `SuiteTestCaseApiIntegrationTest.kt`
- `PlanningExecutionApiIntegrationTest.kt`
- `DefectReportingApiIntegrationTest.kt`
- `UserRegistrationApiIntegrationTest.kt`
- `ApiIntegrationTestBase.kt` (helpers reutilizados por múltiplas suítes)

## Evidência de execução

Verificações executadas com sucesso:

- `./backend-kotlin/gradlew.bat -p backend-kotlin test --tests "com.amazonqa.api.*" --tests "com.amazonqa.EndpointContractCoverageTest"`
- Resultado: **BUILD SUCCESSFUL**

## Observações

- A medição considera cobertura de rota+método HTTP (endpoint atingido por teste).
- A qualidade de validação do payload (read-back/assertions de conteúdo) foi reforçada nos testes de integração nesta iteração.
- Para rastreabilidade contínua, manter este documento atualizado sempre que novos endpoints v1 forem adicionados.
