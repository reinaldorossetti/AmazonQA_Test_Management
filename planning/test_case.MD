# Test Case Management - Amazon QA
## 3. Detalhamento da Tela: Criar Casos de Testes (Create Test Case)

A tela de criação unifica dados técnicos com uma interface limpa e integração à Inteligência Artificial.

*   **Metadados do Teste:**
    *   **Title:** Campo obrigatório para definir o título claro do cenário.
    *   **Priority:** Menu suspenso definindo a importância do teste (`Critical`,`High`, `Medium`, `Low`).
    *   **Test Case Status:** Menu suspenso para definir o ciclo de vida do caso de teste (`Draft`, `Active`, `Archived`).
    *   **Execution Status:** Campo para registrar o resultado da execução (`Not Run`, `Passed`, `Failed`, `Blocked`).
    *   **Notes (Release/Execution):** Campo de observações por release e por execução para registrar contexto, riscos, evidências complementares e decisões do ciclo.
    *   **Bug Severity:** Menu suspenso definindo o impacto técnico do defeito (`Blocker`, `Critical`, `Major`, `Minor`, `Trivial`).
    *   **Tags:** Campo dinâmico para categorização rápida (ex: `@login`, `desktop`).
    *   **Requirement URL:** Caixa de seleção/busca para vincular o cenário diretamente a um requisito de negócio.

*   **Editor Rico e Corpo do Teste:** 
    *   **Precondition:** Bloco para descrever o estado inicial do sistema.
    *   **Action:** Bloco de edição para a sintaxe do cenário, com suporte nativo a Gherkin/BDD (coloração para `@TS01`, `Scenario:`, `Given`, `When`, `Then`, `And`).
    *   **Expected Result:** Bloco listando as validações finais. O editor exibe barras de ferramentas que permitem negrito, itálico, listas, emojis, marcações de código (`</>`) e tabelas.
*   **Anexos e Rodapé:** Botões rápidos de **+ Attach file** e **Attach link** ficam acima do editor. No rodapé, o testador pode reutilizar dados através do botão **Shared Steps** ou concluir a ação utilizando os botões **Cancel**, **Create and Add Another**, **Save as Draft** ou **Create**.

Mapeamento Completo de Status
1. Status de Execução (Execution Status)
 Estes são os status aplicados durante a execução do roteiro (Test Run):
Not Run (Não Executado): Estado inicial de qualquer teste assim que inserido em um Plano de Teste. Indica que o teste ainda está pendente de execução
.
Passed (Aprovado): O teste foi executado e o comportamento do software correspondeu perfeitamente ao resultado esperado
.
Failed (Falhou): O teste foi executado, mas apresentou um defeito, divergência ou comportamento inesperado
.
Blocked (Bloqueado): O teste não pôde ser executado devido a um impeditivo ou condição externa (ex: banco de dados inacessível, ou uma funcionalidade anterior falhou impedindo o avanço)
.
2. Status de Design / Criação (Workflow Status)
 Estes status controlam a qualidade e a governança da escrita do teste antes mesmo dele ir para execução:
Draft: Rascunho (o teste ainda está sendo escrito)
.
Ready for Review: Pronto para a revisão de um par ou líder
.
Review in Progress: A revisão está ocorrendo no momento
.
Rework: O teste foi revisado e precisa de correções
.
Final: O teste foi revisado, aprovado e está pronto para uso (Ativo)
.
Future: Funcionalidade mapeada, mas planejada para o futuro
.
Obsolete: O teste não é mais válido porque a funcionalidade mudou ou deixou de existir

## 4. Tabela de Cadastro de Casos de Teste (Mapeamento)

| Nome da Coluna | Tipo de Dado | Restrição (Constraint) | Descrição e Referência de Origem |
| :--- | :--- | :--- | :--- |
| `test_id` | VARCHAR(20) | PRIMARY KEY, NOT NULL | Identificador único composto de prefixo e contador (ex: `AMQA-001`) (TestLink). |
| `title` | VARCHAR(255) | NOT NULL | Título descritivo do caso de teste (QA Sphere / TestLink). |
| `priority` | ENUM | NOT NULL, DEFAULT 'Medium' | Impacto do teste para a organização: `Critical`,`High`, `Medium`, `Low` (QA Sphere / TestLink). |
| `bug_severity` | ENUM | NOT NULL, DEFAULT 'Major' | Severidade do bug para priorização técnica de correção: `Blocker`, `Critical`, `Major`, `Minor`, `Trivial`. |
| `tags_keywords` | VARCHAR(255) | NULL | Palavras-chave ou tags separadas por vírgula para execução direcionada (QA Sphere / TestLink). |
| `requirement_link` | VARCHAR(255) | NULL | Identificador/URL vinculando o teste à especificação de requisitos (QA Sphere / TestLink). |
| `execution_type` | ENUM | NOT NULL, DEFAULT 'Manual' | Define a natureza da execução: `Manual` ou `Automated` (TestLink). |
| `test_case_status` | ENUM | NOT NULL, DEFAULT 'Draft' | Estado de ciclo de vida do caso de teste: `Draft`, `Active`, `Archived`. |
| `platform` | VARCHAR(100) | NULL | Plataforma associada, fundamental para matriz de compatibilidade (ex: Web, Mobile, Desktop) (TestLink). |
| `test_environment` | VARCHAR(100) | NULL | Ambiente de execução, fundamental para matriz de compatibilidade (ex: Dev, QA, Staging, Production) (TestLink). |
| `preconditions` | TEXT | NULL | Estado sistêmico ou dados prévios exigidos para o teste (QA Sphere / TestLink). |
| `actions` | TEXT | NOT NULL | Instruções de execução passo a passo ou Gherkin/BDD (QA Sphere / TestLink). |
| `expected_result` | TEXT | NOT NULL | Resultado e estado final exigidos para aprovação (QA Sphere / TestLink). |
| `actual_result` | TEXT | NOT NULL | Resultado Atual (QA Sphere / TestLink). |
| `execution_status` | ENUM | NOT NULL, DEFAULT 'Not Run' | Status da execução do teste: `Not Run`, `Passed`, `Failed`, `Blocked`. |
| `notes` | TEXT | NULL | Observações por release/execução (ex: limitações conhecidas, dependências, links de evidência, decisões de triagem). |
| `custom_fields` | JSON | NULL | Estrutura dinâmica e flexível para armazenar os campos personalizados configurados pelo Admin (TestLink). |
| `attachments` | TEXT | NULL | Caminho, referências ou URLs dos arquivos e imagens inseridos (QA Sphere / TestLink). |
| `created_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Data e hora exatas de criação. |
| `updated_at` | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Data da última alteração do cenário (usado para exibir "less than a minute ago"). |

## 5. Script SQL para Criação da Tabela

```sql
CREATE TABLE test_cases (
    test_id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    priority ENUM('High', 'Medium', 'Low') NOT NULL DEFAULT 'Medium',
    bug_severity ENUM('Blocker', 'Critical', 'Major', 'Minor', 'Trivial') NOT NULL DEFAULT 'Major',
    tags_keywords VARCHAR(255) NULL COMMENT 'Armazena tags (QA Sphere) e keywords (TestLink) separadas por virgula ou em JSON array',
    requirement_link VARCHAR(255) NULL COMMENT 'Assegura a matriz de rastreabilidade (Requirements TestLink)',
    execution_type ENUM('Manual', 'Automated') NOT NULL DEFAULT 'Manual',
    test_case_status ENUM('Draft', 'Active', 'Archived') NOT NULL DEFAULT 'Draft' COMMENT 'Estado de ciclo de vida do caso de teste',
    platform VARCHAR(100) NULL COMMENT 'Mapeamento de plataformas de software ou hardware alvo',
    preconditions TEXT NULL COMMENT 'Precondições para a suite de testes iniciar',
    actions TEXT NOT NULL COMMENT 'Escopo do teste com suporte as marcações ricas (Gherkin/Listas/Código)',
    expected_result TEXT NOT NULL COMMENT 'Resultado final e esperado apos as ações',
    actual_result TEXT NOT NULL COMMENT 'Resultado atual do teste',
    execution_status ENUM('Not Run', 'Passed', 'Failed', 'Blocked') NOT NULL DEFAULT 'Not Run' COMMENT 'Status da execução do teste',
    notes TEXT NULL COMMENT 'Observações por release/execução',
    custom_fields JSON NULL COMMENT 'Campo flexivel no padrao de dados moderno para substituir os Custom Fields relacionais do TestLink',
    attachments TEXT NULL COMMENT 'Lista de anexos para evidencias ou scripts suportes',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Tabela central de Biblioteca de Testes para o Amazon QA';

-- Criação de índices para ganho de performance e rápida renderização do Dashboard
CREATE INDEX idx_priority ON test_cases(priority);
CREATE INDEX idx_bug_severity ON test_cases(bug_severity);
CREATE INDEX idx_execution_type ON test_cases(execution_type);
CREATE INDEX idx_test_case_status ON test_cases(test_case_status);
CREATE INDEX idx_execution_status ON test_cases(execution_status);
```