
CREATE TABLE IF NOT EXISTS auth_schema.email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_verification_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES auth_schema.users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_token 
    ON auth_schema.email_verification_tokens(token);

CREATE INDEX idx_email_verification_tokens_user_id 
    ON auth_schema.email_verification_tokens(user_id);

CREATE INDEX idx_email_verification_tokens_expires_at 
    ON auth_schema.email_verification_tokens(expires_at);

COMMENT ON TABLE auth_schema.email_verification_tokens IS 'メール認証トークン管理テーブル';
COMMENT ON COLUMN auth_schema.email_verification_tokens.id IS 'トークンID（UUID）';
COMMENT ON COLUMN auth_schema.email_verification_tokens.token IS '認証トークン文字列（一意）';
COMMENT ON COLUMN auth_schema.email_verification_tokens.user_id IS 'ユーザーID（外部キー）';
COMMENT ON COLUMN auth_schema.email_verification_tokens.expires_at IS 'トークン有効期限';
COMMENT ON COLUMN auth_schema.email_verification_tokens.verified_at IS '認証完了日時（NULL=未認証）';
COMMENT ON COLUMN auth_schema.email_verification_tokens.created_at IS 'トークン作成日時';
