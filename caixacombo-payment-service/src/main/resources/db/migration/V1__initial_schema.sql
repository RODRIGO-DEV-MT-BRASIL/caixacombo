-- =============================================================================
-- CAIXA COMBO - Migration V1: Schema Inicial (PostgreSQL)
-- =============================================================================

-- Tabela de pagamentos
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id TEXT UNIQUE,
    amount DECIMAL(15,2) NOT NULL,
    amount_in_cents DECIMAL(15,2) NOT NULL,
    payment_method TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    empresa_id UUID,
    device_id TEXT,
    device_name TEXT,
    venda_id UUID,
    numero_venda TEXT,
    operator_name TEXT,
    operator_id TEXT,
    stone_transaction_id TEXT,
    stone_atk TEXT,
    card_brand TEXT,
    card_last_digits TEXT,
    installments TEXT DEFAULT '1',
    status_message TEXT,
    stone_response_raw TEXT,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    refunded_at TIMESTAMP,
    refund_amount DECIMAL(15,2),
    refund_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tabela de transações (auditoria)
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status TEXT NOT NULL,
    payment_id UUID REFERENCES payments(id),
    empresa_id UUID,
    stone_transaction_id TEXT,
    response_data TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tabela de auditoria
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    empresa_id UUID,
    status TEXT NOT NULL,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_payments_empresa ON payments(empresa_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_stone_id ON payments(stone_transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_stone_atk ON payments(stone_atk);
CREATE INDEX IF NOT EXISTS idx_transactions_empresa ON transactions(empresa_id);
CREATE INDEX IF NOT EXISTS idx_transactions_payment ON transactions(payment_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_empresa ON audit_logs(empresa_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs(created_at DESC);

-- Função para atualizar updated_at
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers
CREATE TRIGGER trg_payments_updated_at 
    BEFORE UPDATE ON payments 
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_transactions_updated_at 
    BEFORE UPDATE ON transactions 
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
