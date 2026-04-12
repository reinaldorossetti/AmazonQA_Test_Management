CREATE TABLE IF NOT EXISTS test_cases (
  id UUID PRIMARY KEY,
  project_id UUID NOT NULL,
  test_id VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'Medium',
  bug_severity VARCHAR(20) NOT NULL DEFAULT 'Major',
  tags_keywords VARCHAR(255) NULL,
  requirement_link VARCHAR(255) NULL,
  execution_type VARCHAR(20) NOT NULL DEFAULT 'Manual',
  test_case_status VARCHAR(30) NOT NULL DEFAULT 'Draft',
  platform VARCHAR(100) NULL,
  test_environment VARCHAR(100) NULL,
  preconditions TEXT NULL,
  actions TEXT NULL,
  expected_result TEXT NULL,
  actual_result TEXT NULL,
  execution_status VARCHAR(20) NOT NULL DEFAULT 'Not Run',
  notes TEXT NULL,
  custom_fields JSONB NULL,
  attachments TEXT NULL,
  version INT NOT NULL DEFAULT 1,
  deleted_at TIMESTAMP NULL,
  executed_before BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_cases_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT uq_test_cases_project_test_id UNIQUE (project_id, test_id),
  CONSTRAINT chk_test_cases_priority CHECK (priority IN ('Critical', 'High', 'Medium', 'Low')),
  CONSTRAINT chk_test_cases_bug_severity CHECK (bug_severity IN ('Blocker', 'Critical', 'Major', 'Minor', 'Trivial')),
  CONSTRAINT chk_test_cases_execution_type CHECK (execution_type IN ('Manual', 'Automated')),
  CONSTRAINT chk_test_cases_status CHECK (test_case_status IN ('Draft', 'Ready for Review', 'Review in Progress', 'Rework', 'Final', 'Future', 'Obsolete')),
  CONSTRAINT chk_test_cases_execution_status CHECK (execution_status IN ('Not Run', 'Passed', 'Failed', 'Blocked'))
);

CREATE INDEX IF NOT EXISTS idx_test_cases_project_id ON test_cases(project_id);
CREATE INDEX IF NOT EXISTS idx_test_cases_priority ON test_cases(priority);
CREATE INDEX IF NOT EXISTS idx_test_cases_bug_severity ON test_cases(bug_severity);
CREATE INDEX IF NOT EXISTS idx_test_cases_execution_type ON test_cases(execution_type);
CREATE INDEX IF NOT EXISTS idx_test_cases_status ON test_cases(test_case_status);
CREATE INDEX IF NOT EXISTS idx_test_cases_execution_status ON test_cases(execution_status);
