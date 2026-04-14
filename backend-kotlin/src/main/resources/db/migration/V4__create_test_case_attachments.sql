CREATE TABLE IF NOT EXISTS test_case_attachments (
  id UUID PRIMARY KEY,
  project_id UUID NOT NULL,
  test_case_id UUID NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  file_data BYTEA NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_test_case_attachments_project FOREIGN KEY (project_id) REFERENCES projects(id),
  CONSTRAINT fk_test_case_attachments_test_case FOREIGN KEY (test_case_id) REFERENCES test_cases(id)
);

CREATE INDEX IF NOT EXISTS idx_test_case_attachments_project_id ON test_case_attachments(project_id);
CREATE INDEX IF NOT EXISTS idx_test_case_attachments_test_case_id ON test_case_attachments(test_case_id);
