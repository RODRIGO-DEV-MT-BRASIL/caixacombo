-- =============================================================================
-- CAIXA COMBO PAYMENT SERVICE - SCHEMA INICIAL
-- =============================================================================

-- Payments
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(100) UNIQUE NOT NULL,
    stone_transaction_id VARCHAR(100),
    stone_atk VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    amount_in_cents DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status_message VARCHAR(200),
    card_brand VARCHAR(500),
    card_last_digits VARCHAR(20),
    installments VARCHAR(20),
    empresa_id VARCHAR(50) NOT NULL,
    device_id VARCHAR(50),
    device_name VARCHAR(100),
    venda_id VARCHAR(50),
    numero_venda VARCHAR(50),
    operator_name VARCHAR(100),
    operator_id VARCHAR(50),
    stone_response_raw TEXT,
    refund_amount DECIMAL(15,2),
    refund_reason VARCHAR(200),
    refund_transaction_id VARCHAR(100),
    refunded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP
);

CREATE INDEX idx_payment_stone_id ON payments(stone_transaction_id);
CREATE INDEX idx_payment_empresa ON payments(empresa_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_created ON payments(created_at DESC);
CREATE INDEX idx_payment_method ON payments(payment_method);
CREATE INDEX idx_payment_empresa_created ON payments(empresa_id, created_at DESC);

-- Transactions
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    status_message VARCHAR(200),
    payment_id BIGINT REFERENCES payments(id),
    empresa_id VARCHAR(50) NOT NULL,
    stone_transaction_id VARCHAR(100),
    request_data TEXT,
    response_data TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(200),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_payment ON transactions(payment_id);
CREATE INDEX idx_txn_empresa ON transactions(empresa_id);
CREATE INDEX idx_txn_type ON transactions(type);
CREATE INDEX idx_txn_created ON transactions(created_at DESC);

-- Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50),
    empresa_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(50),
    username VARCHAR(100),
    device_id VARCHAR(50),
    old_values TEXT,
    new_values TEXT,
    metadata TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_empresa ON audit_logs(empresa_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);

-- Functions
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
