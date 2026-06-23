-- =============================================================================
-- CAIXA COMBO - SCHEMA UNIFICADO PARA NODE.JS + SPRING BOOT
-- =============================================================================
-- Execute este script no SQL Editor do Supabase
-- =============================================================================

-- Extensões
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- 1. EMPRESAS (multi-tenant)
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.empresas (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id        BIGINT UNIQUE,
    nome            TEXT NOT NULL,
    slug            TEXT UNIQUE NOT NULL,
    cnpj            TEXT,
    email           TEXT,
    telefone        TEXT,
    login           TEXT UNIQUE NOT NULL,
    senha           TEXT NOT NULL,
    permissoes      JSONB DEFAULT '{}',
    primary_color   TEXT DEFAULT '#3b82f6',
    secondary_color TEXT DEFAULT '#06b6d4',
    accent_color    TEXT DEFAULT '#10b981',
    logo_url        TEXT DEFAULT '',
    paginas_permitidas TEXT[] DEFAULT ARRAY['dashboard','vendas','caixa'],
    impressao_template JSONB DEFAULT '{}',
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_empresas_slug ON public.empresas(slug);
CREATE INDEX IF NOT EXISTS idx_empresas_login ON public.empresas(login);

-- =============================================================================
-- 2. USUARIOS (funcionarios / operadores)
-- =============================================================================
CREATE TYPE public.cargo_usuario AS ENUM ('FUNCIONARIO', 'GERENTE', 'ADMIN');

CREATE TABLE IF NOT EXISTS public.usuarios (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id        BIGINT UNIQUE,
    empresa_id      UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    username        TEXT NOT NULL,
    nome            TEXT NOT NULL,
    email           TEXT,
    password        TEXT NOT NULL,
    pin             TEXT,
    role            TEXT NOT NULL DEFAULT 'funcionario',
    cargo           public.cargo_usuario NOT NULL DEFAULT 'FUNCIONARIO',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    empresa_nome    TEXT,
    empresa_slug    TEXT,
    permissoes      JSONB DEFAULT '{}',
    paginas_permitidas TEXT[] DEFAULT ARRAY['dashboard','vendas','caixa'],
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_usuarios_empresa ON public.usuarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON public.usuarios(username);
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON public.usuarios(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_empresa_username ON public.usuarios(empresa_id, username);

-- =============================================================================
-- 3. FUNCIONARIOS (acesso via PIN/email)
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.funcionarios (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id        BIGINT UNIQUE,
    empresa_id      UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    nome            TEXT NOT NULL,
    codigo          TEXT NOT NULL,
    cpf             TEXT,
    telefone        TEXT,
    email           TEXT,
    password        TEXT,
    pin             TEXT,
    cargo           TEXT DEFAULT 'FUNCIONARIO',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    permissoes      JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_funcionarios_empresa ON public.funcionarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_codigo ON public.funcionarios(codigo);
CREATE INDEX IF NOT EXISTS idx_funcionarios_email ON public.funcionarios(email);

-- =============================================================================
-- 4. CATEGORIAS
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.categorias (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id        BIGINT UNIQUE,
    empresa_id      UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    nome            TEXT NOT NULL,
    descricao       TEXT,
    cor             TEXT,
    icone           TEXT,
    ordem           INTEGER DEFAULT 0,
    ativa           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_categorias_empresa ON public.categorias(empresa_id);

-- =============================================================================
-- 5. PRODUTOS
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.produtos (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id        BIGINT UNIQUE,
    empresa_id      UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    categoria_id    UUID,
    nome            TEXT NOT NULL,
    descricao       TEXT,
    preco           NUMERIC(15,2) NOT NULL DEFAULT 0,
    codigo_barras   TEXT,
    estoque         NUMERIC(15,3) DEFAULT 0,
    unidade         TEXT DEFAULT 'UN',
    imagem          TEXT,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_produtos_empresa ON public.produtos(empresa_id);
CREATE INDEX IF NOT EXISTS idx_produtos_categoria ON public.produtos(categoria_id);

-- =============================================================================
-- 6. VENDAS
-- =============================================================================
CREATE TYPE public.forma_pagamento AS ENUM (
    'DINHEIRO', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'PIX', 'FIADO'
);

CREATE TABLE IF NOT EXISTS public.vendas (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id            BIGINT UNIQUE,
    empresa_id          UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    numero              TEXT NOT NULL,
    data_hora           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    subtotal            NUMERIC(15,2) NOT NULL DEFAULT 0,
    desconto            NUMERIC(15,2) NOT NULL DEFAULT 0,
    total               NUMERIC(15,2) NOT NULL DEFAULT 0,
    forma_pagamento     public.forma_pagamento NOT NULL,
    valor_recebido      NUMERIC(15,2) DEFAULT 0,
    troco               NUMERIC(15,2) DEFAULT 0,
    status              TEXT DEFAULT 'FINALIZADA',
    cancelada           BOOLEAN DEFAULT FALSE,
    cancelada_em        TIMESTAMPTZ,
    cancelada_por       TEXT,
    motivo_cancelamento TEXT,
    stone_atk           TEXT,
    device_id           TEXT,
    empresa_nome        TEXT,
    empresa_slug        TEXT,
    vendedor_id         UUID,
    vendedor_nome       TEXT,
    observacao          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_vendas_empresa ON public.vendas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_vendas_data ON public.vendas(empresa_id, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_vendas_device ON public.vendas(device_id);

-- =============================================================================
-- 7. OPERACOES DE CAIXA
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.operacoes (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    local_id            BIGINT UNIQUE,
    empresa_id          UUID REFERENCES public.empresas(id) ON DELETE CASCADE,
    tipo                TEXT NOT NULL,
    nome_operador       TEXT NOT NULL,
    operador_id         UUID,
    data_hora           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valor               NUMERIC(15,2) DEFAULT 0,
    valor_inicial       NUMERIC(15,2),
    observacao          TEXT,
    device_id           TEXT,
    empresa_nome        TEXT,
    empresa_slug        TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_operacoes_empresa ON public.operacoes(empresa_id);
CREATE INDEX IF NOT EXISTS idx_operacoes_data ON public.operacoes(empresa_id, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_operacoes_device ON public.operacoes(device_id);

-- =============================================================================
-- 8. DISPOSITIVOS (terminais Android)
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.dispositivos (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    device_id           TEXT UNIQUE NOT NULL,
    device_name         TEXT NOT NULL DEFAULT 'Dispositivo',
    device_type         TEXT DEFAULT 'Android',
    serial_number       TEXT,
    status              TEXT DEFAULT 'offline',
    lock_password       TEXT,
    empresa_id          UUID REFERENCES public.empresas(id) ON DELETE SET NULL,
    empresa_nome        TEXT,
    empresa_slug        TEXT,
    usage_time_limit    INTEGER,
    usage_start_time    TIMESTAMPTZ,
    last_poll           TIMESTAMPTZ,
    last_login          TIMESTAMPTZ,
    last_login_user     TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_dispositivos_empresa ON public.dispositivos(empresa_id);

-- =============================================================================
-- 9. AUDITORIA
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.auditoria (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    timestamp           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tipo                TEXT NOT NULL,
    device_id           TEXT,
    device_name         TEXT,
    detalhes            TEXT,
    usuario             TEXT,
    ip                  TEXT,
    empresa_id          UUID REFERENCES public.empresas(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_auditoria_empresa ON public.auditoria(empresa_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_timestamp ON public.auditoria(timestamp DESC);

-- =============================================================================
-- 10. TRIGGERS para updated_at automático
-- =============================================================================
CREATE OR REPLACE FUNCTION public.fn_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN
        SELECT unnest(ARRAY['empresas','usuarios','funcionarios','categorias','produtos','vendas','operacoes','dispositivos'])
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON public.%I', t, t);
        EXECUTE format('CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.fn_set_updated_at()', t, t);
    END LOOP;
END $$;

-- =============================================================================
-- 11. FUNCOES RPC para o Node.js
-- =============================================================================

-- Funcao para buscar empresa por login
CREATE OR REPLACE FUNCTION public.get_empresa_by_login(p_login TEXT)
RETURNS public.empresas AS $$
    SELECT * FROM public.empresas WHERE login = p_login AND ativa = TRUE;
$$ LANGUAGE sql SECURITY DEFINER;

-- Funcao para verificar credenciais
CREATE OR REPLACE FUNCTION public.check_user_credentials(p_username TEXT, p_password TEXT)
RETURNS TABLE (
    id UUID,
    username TEXT,
    role TEXT,
    empresa_id UUID,
    empresa_nome TEXT,
    empresa_slug TEXT,
    permissoes JSONB,
    paginas_permitidas TEXT[]
) AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.username, u.role, u.empresa_id, u.empresa_nome, u.empresa_slug, u.permissoes, u.paginas_permitidas
    FROM public.usuarios u
    WHERE u.username = p_username
    AND u.ativo = TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =============================================================================
-- 12. DADOS INICIAIS (seed)
-- =============================================================================
INSERT INTO public.empresas (id, nome, slug, login, senha, ativa)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Empresa Padrão',
    'empresa-padrao',
    'admin',
    '$2a$10$YourHashedPasswordHere',
    TRUE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.usuarios (empresa_id, username, nome, password, role, cargo, ativo)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'Administrador',
    '$2a$10$YourHashedPasswordHere',
    'admin',
    'ADMIN',
    TRUE
) ON CONFLICT DO NOTHING;

-- =============================================================================
-- FIM
-- =============================================================================
