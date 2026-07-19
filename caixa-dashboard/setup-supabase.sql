-- ============================================
-- CaixaCombo - Reset Completo do Supabase
-- Executar no: Supabase Dashboard > SQL Editor
-- ============================================

-- PASSO 1: Desabilitar RLS e dropar todas as tabelas
ALTER TABLE IF EXISTS clientes DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS auditoria DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS dispositivos DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS operacoes DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS vendas DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS produtos DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS categorias DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS funcionarios DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS usuarios DISABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS empresas DISABLE ROW LEVEL SECURITY;

DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS auditoria CASCADE;
DROP TABLE IF EXISTS dispositivos CASCADE;
DROP TABLE IF EXISTS operacoes CASCADE;
DROP TABLE IF EXISTS vendas CASCADE;
DROP TABLE IF EXISTS produtos CASCADE;
DROP TABLE IF EXISTS categorias CASCADE;
DROP TABLE IF EXISTS funcionarios CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TABLE IF EXISTS empresas CASCADE;

-- Dropar tabelas de outro projeto que possam conflitar
DROP TABLE IF EXISTS cash_register_movements CASCADE;
DROP TABLE IF EXISTS token_balance CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS vehicle_types CASCADE;
DROP TABLE IF EXISTS profiles CASCADE;
DROP TABLE IF EXISTS parking_exit_passwords CASCADE;
DROP TABLE IF EXISTS tickets CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS parking_spots CASCADE;
DROP TABLE IF EXISTS cash_register_sessions CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS parking_vouchers CASCADE;
DROP TABLE IF EXISTS cash_registers CASCADE;
DROP TABLE IF EXISTS returns CASCADE;
DROP TABLE IF EXISTS rate_limit_events CASCADE;
DROP TABLE IF EXISTS events CASCADE;
DROP TABLE IF EXISTS parking_voucher_passwords CASCADE;

DROP FUNCTION IF EXISTS public.current_empresa_id() CASCADE;
DROP FUNCTION IF EXISTS public.open_cash_register CASCADE;
DROP FUNCTION IF EXISTS public.close_cash_register CASCADE;
DROP FUNCTION IF EXISTS public.generate_exit_password CASCADE;
DROP FUNCTION IF EXISTS public.validate_exit_password CASCADE;
DROP FUNCTION IF EXISTS public.rls_auto_enable CASCADE;

-- Limpar policies antigas
DO $$ DECLARE r RECORD; BEGIN
  FOR r IN (SELECT schemaname, tablename, policyname FROM pg_policies WHERE schemaname = 'public') LOOP
    EXECUTE 'DROP POLICY IF EXISTS "' || r.policyname || '" ON ' || r.schemaname || '.' || r.tablename;
  END LOOP;
END $$;

-- PASSO 2: Criar tabelas do CaixaCombo
CREATE TABLE empresas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  local_id BIGINT UNIQUE,
  nome TEXT NOT NULL,
  slug TEXT UNIQUE NOT NULL,
  cnpj TEXT,
  email TEXT,
  telefone TEXT,
  login TEXT UNIQUE NOT NULL,
  senha TEXT NOT NULL,
  permissoes JSONB DEFAULT '{}',
  primary_color TEXT DEFAULT '#3b82f6',
  secondary_color TEXT DEFAULT '#06b6d4',
  accent_color TEXT DEFAULT '#10b981',
  logo_url TEXT DEFAULT '',
  paginas_permitidas TEXT[] DEFAULT ARRAY['dashboard','vendas','caixa'],
  impressao_template JSONB DEFAULT '{}',
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE usuarios (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  username TEXT NOT NULL,
  nome TEXT NOT NULL,
  email TEXT,
  password TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'funcionario',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  empresa_nome TEXT,
  empresa_slug TEXT,
  permissoes JSONB DEFAULT '{}',
  paginas_permitidas TEXT[] DEFAULT ARRAY['dashboard','vendas','caixa'],
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE(empresa_id, username)
);

CREATE TABLE funcionarios (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  nome TEXT NOT NULL,
  codigo TEXT NOT NULL,
  cpf TEXT,
  telefone TEXT,
  email TEXT,
  password TEXT,
  pin TEXT,
  cargo TEXT DEFAULT 'FUNCIONARIO',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  permissoes JSONB DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE categorias (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  nome TEXT NOT NULL,
  descricao TEXT,
  cor TEXT,
  icone TEXT,
  ordem INTEGER DEFAULT 0,
  ativa BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE produtos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  categoria_id UUID,
  nome TEXT NOT NULL,
  descricao TEXT,
  preco NUMERIC(15,2) NOT NULL DEFAULT 0,
  codigo_barras TEXT,
  estoque NUMERIC(15,3) DEFAULT 0,
  unidade TEXT DEFAULT 'UN',
  imagem TEXT,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vendas (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  numero TEXT NOT NULL,
  data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  subtotal NUMERIC(15,2) NOT NULL DEFAULT 0,
  desconto NUMERIC(15,2) NOT NULL DEFAULT 0,
  total NUMERIC(15,2) NOT NULL DEFAULT 0,
  forma_pagamento TEXT NOT NULL,
  valor_recebido NUMERIC(15,2) DEFAULT 0,
  troco NUMERIC(15,2) DEFAULT 0,
  status TEXT DEFAULT 'FINALIZADA',
  cancelada BOOLEAN DEFAULT FALSE,
  cancelada_em TIMESTAMPTZ,
  cancelada_por TEXT,
  motivo_cancelamento TEXT,
  stone_atk TEXT,
  device_id TEXT,
  empresa_nome TEXT,
  empresa_slug TEXT,
  vendedor_id UUID,
  vendedor_nome TEXT,
  observacao TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE operacoes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  tipo TEXT NOT NULL,
  nome_operador TEXT NOT NULL,
  operador_id UUID,
  data_hora TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  valor NUMERIC(15,2) DEFAULT 0,
  valor_inicial NUMERIC(15,2),
  observacao TEXT,
  device_id TEXT,
  empresa_nome TEXT,
  empresa_slug TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE dispositivos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id TEXT UNIQUE NOT NULL,
  device_name TEXT NOT NULL DEFAULT 'Dispositivo',
  device_type TEXT DEFAULT 'Android',
  serial_number TEXT,
  status TEXT DEFAULT 'offline',
  lock_password TEXT,
  empresa_id UUID REFERENCES empresas(id) ON DELETE SET NULL,
  empresa_nome TEXT,
  empresa_slug TEXT,
  usage_time_limit INTEGER,
  usage_start_time TIMESTAMPTZ,
  last_poll TIMESTAMPTZ,
  last_login TIMESTAMPTZ,
  last_login_user TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE auditoria (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  tipo TEXT NOT NULL,
  device_id TEXT,
  device_name TEXT,
  detalhes TEXT,
  usuario TEXT,
  ip TEXT,
  empresa_id UUID REFERENCES empresas(id) ON DELETE SET NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE clientes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  empresa_id UUID REFERENCES empresas(id) ON DELETE CASCADE,
  nome TEXT NOT NULL,
  cpf_cnpj TEXT DEFAULT '',
  telefone TEXT DEFAULT '',
  email TEXT DEFAULT '',
  endereco TEXT DEFAULT '',
  cidade TEXT DEFAULT '',
  cep TEXT DEFAULT '',
  observacao TEXT DEFAULT '',
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- PASSO 3: Índices
CREATE INDEX idx_empresas_slug ON empresas(slug);
CREATE INDEX idx_empresas_login ON empresas(login);
CREATE INDEX idx_usuarios_empresa ON usuarios(empresa_id);
CREATE INDEX idx_usuarios_username ON usuarios(username);
CREATE INDEX idx_funcionarios_empresa ON funcionarios(empresa_id);
CREATE INDEX idx_funcionarios_codigo ON funcionarios(codigo);
CREATE INDEX idx_categorias_empresa ON categorias(empresa_id);
CREATE INDEX idx_produtos_empresa ON produtos(empresa_id);
CREATE INDEX idx_vendas_empresa ON vendas(empresa_id);
CREATE INDEX idx_vendas_data ON vendas(empresa_id, data_hora DESC);
CREATE INDEX idx_operacoes_empresa ON operacoes(empresa_id);
CREATE INDEX idx_operacoes_data ON operacoes(empresa_id, data_hora DESC);
CREATE INDEX idx_dispositivos_empresa ON dispositivos(empresa_id);
CREATE INDEX idx_auditoria_empresa ON auditoria(empresa_id);
CREATE INDEX idx_auditoria_timestamp ON auditoria(timestamp DESC);
CREATE INDEX idx_clientes_empresa ON clientes(empresa_id);

-- PASSO 4: Habilitar RLS
ALTER TABLE empresas ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE funcionarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE categorias ENABLE ROW LEVEL SECURITY;
ALTER TABLE produtos ENABLE ROW LEVEL SECURITY;
ALTER TABLE vendas ENABLE ROW LEVEL SECURITY;
ALTER TABLE operacoes ENABLE ROW LEVEL SECURITY;
ALTER TABLE dispositivos ENABLE ROW LEVEL SECURITY;
ALTER TABLE auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE clientes ENABLE ROW LEVEL SECURITY;

-- PASSO 5: Policies (acesso total via service_role do backend)
CREATE POLICY "service_all_empresas" ON empresas FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_usuarios" ON usuarios FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_funcionarios" ON funcionarios FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_categorias" ON categorias FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_produtos" ON produtos FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_vendas" ON vendas FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_operacoes" ON operacoes FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_dispositivos" ON dispositivos FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_auditoria" ON auditoria FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "service_all_clientes" ON clientes FOR ALL USING (true) WITH CHECK (true);

-- PASSO 6: Inserir admin padrão
INSERT INTO usuarios (username, nome, password, role, ativo) VALUES ('admin', 'Administrador', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin', true) ON CONFLICT DO NOTHING;
-- Senha: admin123
