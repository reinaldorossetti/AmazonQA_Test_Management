CREATE TABLE IF NOT EXISTS user_profiles (
  user_id UUID PRIMARY KEY,
  person_type VARCHAR(2) NOT NULL DEFAULT 'PF',
  first_name VARCHAR(120) NULL,
  last_name VARCHAR(120) NULL,
  phone VARCHAR(40) NULL,
  cpf VARCHAR(20) NULL,
  cnpj VARCHAR(30) NULL,
  company_name VARCHAR(255) NULL,
  address_zip VARCHAR(20) NULL,
  address_street VARCHAR(255) NULL,
  address_number VARCHAR(40) NULL,
  address_complement VARCHAR(255) NULL,
  address_neighborhood VARCHAR(150) NULL,
  address_city VARCHAR(120) NULL,
  address_state VARCHAR(40) NULL,
  residence_proof_filename VARCHAR(255) NULL,
  password_hash VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_user_profiles_person_type CHECK (person_type IN ('PF', 'PJ'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_profiles_cpf ON user_profiles(cpf) WHERE cpf IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_profiles_cnpj ON user_profiles(cnpj) WHERE cnpj IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_profiles_city_state ON user_profiles(address_city, address_state);
