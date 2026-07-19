-- CaixaCombo - Criar tabelas no Supabase
-- Executar no: Supabase Dashboard > SQL Editor

CREATE TABLE IF NOT EXISTS empresas (
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

CREATE TABLE IF NOT EXISTS usuarios (
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

CREATE TABLE IF NOT EXISTS funcionarios (
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

CREATE TABLE IF NOT EXISTS categorias (
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

CREATE TABLE IF NOT EXISTS produtos (
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

CREATE TABLE IF NOT EXISTS vendas (
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

CREATE TABLE IF NOT EXISTS operacoes (
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

CREATE TABLE IF NOT EXISTS dispositivos (
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

CREATE TABLE IF NOT EXISTS auditoria (
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

CREATE TABLE IF NOT EXISTS clientes (
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

-- Índices
CREATE INDEX IF NOT EXISTS idx_empresas_slug ON empresas(slug);
CREATE INDEX IF NOT EXISTS idx_empresas_login ON empresas(login);
CREATE INDEX IF NOT EXISTS idx_usuarios_empresa ON usuarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios(username);
CREATE INDEX IF NOT EXISTS idx_funcionarios_empresa ON funcionarios(empresa_id);
CREATE INDEX IF NOT EXISTS idx_funcionarios_codigo ON funcionarios(codigo);
CREATE INDEX IF NOT EXISTS idx_categorias_empresa ON categorias(empresa_id);
CREATE INDEX IF NOT EXISTS idx_produtos_empresa ON produtos(empresa_id);
CREATE INDEX IF NOT EXISTS idx_vendas_empresa ON vendas(empresa_id);
CREATE INDEX IF NOT EXISTS idx_vendas_data ON vendas(empresa_id, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_operacoes_empresa ON operacoes(empresa_id);
CREATE INDEX IF NOT EXISTS idx_operacoes_data ON operacoes(empresa_id, data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_dispositivos_empresa ON dispositivos(empresa_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_empresa ON auditoria(empresa_id);
CREATE INDEX IF NOT EXISTS idx_auditoria_timestamp ON auditoria(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_clientes_empresa ON clientes(empresa_id);

-- Habilitar RLS
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

-- Policies para service_role (acesso total)
CREATE POLICY "service_all_empresas" ON empresas FOR ALL USING (true);
CREATE POLICY "service_all_usuarios" ON usuarios FOR ALL USING (true);
CREATE POLICY "service_all_funcionarios" ON funcionarios FOR ALL USING (true);
CREATE POLICY "service_all_categorias" ON categorias FOR ALL USING (true);
CREATE POLICY "service_all_produtos" ON produtos FOR ALL USING (true);
CREATE POLICY "service_all_vendas" ON vendas FOR ALL USING (true);
CREATE POLICY "service_all_operacoes" ON operacoes FOR ALL USING (true);
CREATE POLICY "service_all_dispositivos" ON dispositivos FOR ALL USING (true);
CREATE POLICY "service_all_auditoria" ON auditoria FOR ALL USING (true);
CREATE POLICY "service_all_clientes" ON clientes FOR ALL USING (true);
