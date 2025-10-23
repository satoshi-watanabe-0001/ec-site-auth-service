CREATE SCHEMA IF NOT EXISTS auth_schema;

CREATE TABLE auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_users_email ON auth_schema.users(email);

CREATE INDEX idx_users_status ON auth_schema.users(status);

CREATE OR REPLACE FUNCTION auth_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON auth_schema.users
    FOR EACH ROW EXECUTE FUNCTION auth_schema.update_updated_at_column();
