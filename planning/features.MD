# Plano Detalhado do Produto: Amazon QA (Test Case Management)

O **Amazon QA** é uma plataforma de gestão de testes orientada a rastreabilidade ponta a ponta: Requisito → Caso de Teste → Execução → Defeito → Métricas. O objetivo do produto é reduzir retrabalho, aumentar cobertura e dar visibilidade operacional para squads de QA, Produto e Engenharia.

> **Diretriz estratégica:** nesta etapa, o foco é **MVP sem IA**. Recursos de Inteligência Artificial ficam para a Fase 2.

---

## 1) Objetivos de Negócio e Princípios de Produto

### Objetivos (12 meses)
- Aumentar a cobertura rastreável de requisitos para acima de 90%.
- Reduzir tempo de preparação de ciclo de testes em pelo menos 30%.
- Melhorar previsibilidade de release com indicadores confiáveis de execução.

### Princípios
- **Rastreabilidade primeiro:** cada artefato deve ser conectável e auditável.
- **Produtividade sem fricção:** fluxos rápidos para criar, planejar e executar.
- **Governança por perfil:** RBAC forte desde o início.
- **Escalabilidade de dados:** arquitetura pronta para analytics e IA futura.

---

## 2) Ordem Recomendada de Implementação (priorização realista)

## Fase 1 — MVP (sem IA)

### Bloco P0 (fundação obrigatória)
1. **Identidade, RBAC e Configurações**
   - Login, sessão, perfil, papéis (Guest, Tester, Senior Tester, Leader, Admin).
   - Preferências de usuário (tema, idioma, timezone).
   - Auditoria de ações críticas (quem alterou o quê e quando).

2. **Domínio base e modelo de dados**
   - Entidades principais: Projeto, Requisito, Suite, Caso de Teste, Plano, Build, Run, Execução, Defeito.
   - Versionamento de caso de teste (snapshot por versão).
   - Relacionamentos para matriz de rastreabilidade.

3. **Test Cases (CRUD + árvore de suites)**
   - Criação/edição com campos: pré-condição, passos, resultado esperado, prioridade, tags, status.
   - Estrutura em árvore por módulo.
   - Busca/filtro por prioridade, tag, status e responsável.


**Criar Casos de Testes** (Create Test Case) deve ser composta pelos seguintes elementos e seções:

*   **Informações Básicas e Metadados:**
    *   **Title (Título):** Campo de texto obrigatório para nomear e identificar o caso de teste.
    *   **Priority (Prioridade):** Menu suspenso para definir a relevância do teste, contendo opções como "High" (Alta) ou "Medium" (Média).
    *   **Tags:** Campo dinâmico para buscar, selecionar ou inserir etiquetas que facilitam a categorização (como `@login`, `ios`, `desktop`).
    *   **Requirement (Requisito / Requirement URL):** Campo dedicado a vincular o caso de teste a um requisito específico do projeto, permitindo buscar um existente ou criar um novo.

*   **Anexos:**
    *   Abaixo dos campos iniciais, a interface oferece botões de acesso rápido para **+ Attach file** (Anexar arquivo) e **Attach link** (Anexar link).

*   **Corpo do Teste e Editor de Texto Rico:**
    *   A estruturação do teste é dividida em três blocos de texto principais: **Precondition** (Pré-condições), **Action** (Ações) e **Expected Result** (Resultado Esperado).
    *   **Barra de Ferramentas:** Ao digitar nestes campos, o sistema apresenta um menu de formatação avançada contendo opções para: negrito, itálico, sublinhado, tachado, aspas, listas com marcadores ou numeradas, links, emojis, inserção de imagens, vídeos, blocos de código (`</>`) e tabelas.
    *   **Suporte a Sintaxe BDD:** O editor suporta nativamente a formatação de cenários (ex: BDD/Gherkin), aplicando cores e destaques específicos a palavras-chave e marcações como `@TS01`, `Scenario:`, `Given`, `When`, `Then` e `And` dentro do campo de "Action".

*   **Rodapé e Ações Finais:**
    *   Ao final do formulário, há um botão de **"+"** e um menu suspenso para acessar **"Shared Steps"** (Passos Compartilhados).
    *   Os botões de controle para concluir a operação ficam no canto inferior da tela e incluem: **Cancel** (Cancelar), **Create and Add Another** (Criar e Adicionar Outro - ideal para criação em massa), **Save as Draft** (Salvar como Rascunho) e o botão principal **Create** (Criar) em destaque roxo.

### Bloco P1 (operação de QA)
4. **Requirements + Matriz de Rastreabilidade**
   - Importação de requisitos via CSV/XML e endpoint de integração.
   - Visualização de cobertura e “gaps” de testes.

5. **Test Plan / Build / Run**
   - Criação de plano por release.
   - Seleção manual e por filtros de casos.
   - Atribuição em massa por testador/squad.

6. **Execução de Testes + Integração de Defeitos**
   - Registro de status: Passed, Failed, Blocked, Not Run.
   - Evidências (anexo, comentário, ambiente).
   - Criação de defeito via integração (Jira/Mantis) com payload padronizado.

### Bloco P2 (fechamento do MVP)
7. **Dashboard e Relatórios Básicos**
   - Indicadores operacionais (execução por status, cobertura por requisito, progresso por run).
   - Exportação CSV/PDF.

---

## Fase 2 — Evolução (com IA e analytics avançado)

1. **Write with AI (assistente de criação de casos)**
2. **Recomendação inteligente de seleção de testes por risco**
3. **Relatórios avançados com tendências preditivas e anomalias**

---

## 3) Melhorias Implementáveis (curto prazo) ✅

As melhorias abaixo podem ser implementadas **já no MVP** e trazem alto impacto:

1. **Template padronizado de caso de teste por tipo**
   - Ex.: funcional, regressão, integração, API.
   - Ganho: consistência e redução de tempo de escrita.

2. **Campos obrigatórios inteligentes por fluxo**
   - Ex.: ao marcar “Failed”, exigir “Actual Result” + evidência.
   - Ganho: melhor qualidade dos bugs abertos.

3. **“Health Check” do plano de testes**
   - Score com critérios: cobertura, testes sem executor, testes bloqueados.
   - Ganho: previsibilidade de release.

4. **Bulk edit com validações de segurança**
   - Alterar owner/prioridade/tag em lote com trilha de auditoria.
   - Ganho: ganho operacional do lead.

5. **Baseline de métricas por sprint/release**
   - Salvar snapshot no fechamento da build.
   - Ganho: comparação histórica confiável.

6. **Notificações orientadas a evento**
   - Ex.: run atribuído, falha crítica aberta, cobertura abaixo do alvo.
   - Ganho: resposta mais rápida da equipe.

7. **Catálogo de integrações com configuração guiada**
   - Wizard para Jira/Mantis e webhooks CI/CD.
   - Ganho: menor esforço de setup em novos projetos.

8. **Validação de duplicidade de caso de teste**
   - Checagem por similaridade de título + tags + módulo.
   - Ganho: evita redundância de suíte.

9. **Filtro salvo por usuário (views personalizadas)**
   - Ex.: “Meus testes críticos”, “Falhas da build atual”.
   - Ganho: produtividade diária.

10. **Definição de pronto (DoD) por artefato**
   - Caso de teste “pronto” só com vínculo de requisito + revisão.
   - Ganho: melhora qualidade de base.

---


## 4) BDD — Fluxos Principais da Fase 1

### 4.1 Alternância de tema com persistência
**Funcionalidade:** Preferência visual do usuário
- **Dado** que o usuário está autenticado no Amazon QA
- **Quando** ele alterna de Dark Mode para White Mode no cabeçalho
- **Então** a interface deve atualizar imediatamente sem recarregar a página
- **E** a preferência deve ser persistida localmente e no perfil do usuário

### 4.2 Importação de requisitos
**Funcionalidade:** Carga de requisitos para rastreabilidade
- **Dado** que o Leader acessa o módulo de Requirements
- **Quando** ele importa um arquivo CSV válido com Document ID, Título e Escopo
- **Então** o sistema deve criar os requisitos e registrar o lote importado
- **E** exibir erros de linha para registros inválidos

### 4.3 Criação de caso de teste versionado
**Funcionalidade:** Gestão de biblioteca de testes
- **Dado** que o Tester está no módulo Test Cases
- **Quando** ele cria um novo caso com pré-condição, passos e resultado esperado
- **Então** o sistema deve salvar o caso na versão 1.0 com status Ativo
- **E** permitir nova versão sem perda do histórico anterior

### 4.4 Vínculo requisito-caso
**Funcionalidade:** Matriz de rastreabilidade
- **Dado** que existe um requisito sem cobertura
- **Quando** um caso de teste é vinculado a esse requisito
- **Então** a matriz deve atualizar o status para “Coberto”
- **E** recalcular os indicadores de cobertura do projeto

### 4.5 Planejamento por build
**Funcionalidade:** Criação de Test Plan e Test Run
- **Dado** que existe a Build 2.0 cadastrada
- **Quando** o Leader cria um Test Plan e seleciona casos de prioridade alta
- **Então** o sistema deve gerar um Test Run associado à Build 2.0
- **E** permitir atribuição em massa aos testadores

### 4.6 Execução com falha e abertura de bug
**Funcionalidade:** Registro de falha e integração externa
- **Dado** que o Tester está executando um caso no Test Run
- **Quando** ele marca o resultado como Failed
- **Então** o sistema deve exigir preenchimento de Actual Result e evidência
- **Quando** ele aciona “Add Jira Issue”
- **Então** o sistema deve abrir o defeito com resumo, passos e contexto da execução

### 4.7 Controle de acesso por perfil
**Funcionalidade:** Segurança e governança
- **Dado** que um usuário Guest está autenticado
- **Quando** ele tenta editar um Test Plan
- **Então** o sistema deve negar a ação por permissão insuficiente
- **E** registrar tentativa em log de auditoria

### 4.8 Fechamento de build
**Funcionalidade:** Congelamento de resultados
- **Dado** que a Build foi marcada como “Closed” pelo Leader
- **Quando** qualquer usuário tenta alterar resultados de execução dessa Build
- **Então** o sistema deve bloquear a edição
- **E** orientar a criação de nova Build para ajustes

---

## 5) Critérios de Aceite do MVP (Definition of Done)

O MVP da Fase 1 será considerado pronto quando:
- RBAC ativo com perfis e permissões validadas.
- CRUD completo de Test Cases com versionamento funcional.
- Matriz de rastreabilidade com cálculo de cobertura.
- Planejamento de Test Plan/Run por Build ativo.
- Execução com ciclo completo de status e evidência.
- Integração com ao menos 1 bug tracker funcionando em ambiente de homologação.
- Dashboard operacional com métricas mínimas e exportação CSV.

---

## 6) Riscos e Mitigações

1. **Risco:** baixa qualidade de dados importados de requisitos.
   - **Mitigação:** validação de schema + relatório de erros por linha.

2. **Risco:** crescimento desordenado da biblioteca de testes.
   - **Mitigação:** padronização por template, revisão e políticas de versionamento.

3. **Risco:** falhas de integração com bug tracker externo.
   - **Mitigação:** retries com fila, idempotência e logging técnico.

4. **Risco:** resistência de adoção do time.
   - **Mitigação:** onboarding com playbook de uso e quick wins nas primeiras sprints.

---
## 7) Métricas de Sucesso

Persona Definition & Context
"You are a Senior Software Engineer specializing in TypeScript and Kotlin Spring Boot. Strictly follow software development best practices (Clean Code, SOLID, and Design Patterns). 
Write unit tests to validate the code and ensure that ESLint passes with zero warnings or errors."

## 8) Clean Code & Semantic Legibility
Descriptive Naming: No abbreviations. Use names that describe intent (e.g., isUserEligibleForDiscount instead of disc_ok).
Self-Explanatory Functions: Functions must follow the verb + noun pattern (e.g., calculateTotalWeight).
Boolean Prefixing: Boolean variables must use prefixes like is, has, should, or can.
The "Why" Comments: Comments should explain business logic decisions, not the code itself (the code must be readable).

## 8)  Definition of Done (DoD)
 1. Linting: Code passes ESLint with zero warnings.
 2. Typing: Strict TypeScript (no any types).
 3. Testing: 1. 100% coverage of the Requirements Traceability Matrix. 2. Create API tests or 
 4. End to End: e2e tests if applicable (web or mobile applications in TypeScript), E2E tests should not contain simulated data.
 5. Language: 100% English codebase.
 6. Documentation: Updated README or inline JSDoc where applicable.