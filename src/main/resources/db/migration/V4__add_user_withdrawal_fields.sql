
ALTER TABLE auth_schema.users
    ADD COLUMN deletion_scheduled_at TIMESTAMP,
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN withdrawal_reason TEXT;

ALTER TABLE auth_schema.users
    DROP CONSTRAINT chk_status;

ALTER TABLE auth_schema.users
    ADD CONSTRAINT chk_status CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_DELETION', 'DELETED'));

CREATE INDEX idx_users_deletion_scheduled_at ON auth_schema.users(deletion_scheduled_at)
    WHERE deletion_scheduled_at IS NOT NULL;

CREATE INDEX idx_users_deleted_at ON auth_schema.users(deleted_at)
    WHERE deleted_at IS NOT NULL;
