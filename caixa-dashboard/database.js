// =============================================================================
// CAIXA COMBO - Camada de Banco de Dados (Supabase PostgreSQL)
// =============================================================================

const { Pool } = require('pg');

let pool = null;

// Conectar ao Supabase
async function connectDatabase() {
  const databaseUrl = process.env.DATABASE_URL;
  
  if (!databaseUrl) {
    console.warn('⚠️ DATABASE_URL não definido — rodando em modo offline (data.json)');
    return false;
  }

  try {
    pool = new Pool({
      connectionString: databaseUrl,
      ssl: { rejectUnauthorized: false },
      max: 20,
      idleTimeoutMillis: 30000,
      connectionTimeoutMillis: 5000,
    });

    // Testar conexão
    const client = await pool.connect();
    console.log('✅ Conectado ao Supabase PostgreSQL');
    client.release();

    // Criar tabelas se não existirem
    await createTables();
    
    return true;
  } catch (err) {
    console.error('❌ Erro ao conectar ao Supabase:', err.message);
    return false;
  }
}

// Criar tabelas
async function createTables() {
  const client = await pool.connect();
  try {
    await client.query(`
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
    `);
    
    // Habilitar RLS em todas as tabelas
    const tables = ['empresas','usuarios','funcionarios','categorias','produtos','vendas','operacoes','dispositivos','auditoria'];
    for (const t of tables) {
      await client.query(`ALTER TABLE ${t} ENABLE ROW LEVEL SECURITY`);
    }

    // Funcao helper para RLS
    await client.query(`
      CREATE OR REPLACE FUNCTION public.current_empresa_id()
      RETURNS UUID LANGUAGE sql STABLE SECURITY DEFINER
      SET search_path = public
      AS $$ SELECT empresa_id FROM public.usuarios WHERE id::text = auth.uid()::text LIMIT 1; $$;
    `);

    // Policies de tenant isolation
    for (const t of ['usuarios','funcionarios','categorias','produtos','vendas','operacoes','dispositivos','auditoria']) {
      await client.query(`DROP POLICY IF EXISTS "tenant_select" ON ${t}`);
      await client.query(`
        CREATE POLICY tenant_select ON ${t}
          FOR SELECT TO authenticated
          USING (empresa_id = public.current_empresa_id());
      `);
    }

    // Empresa:select
    await client.query('DROP POLICY IF EXISTS "tenant_select" ON empresas');
    await client.query(`
      CREATE POLICY tenant_select ON empresas
        FOR SELECT TO authenticated
        USING (id = public.current_empresa_id());
    `);

    console.log('✅ Tabelas criadas/verificadas + RLS habilitado');
  } catch (err) {
    console.error('❌ Erro ao criar tabelas:', err.message);
    throw err;
  } finally {
    client.release();
  }
}

// Executar query
async function query(text, params) {
  if (!pool) throw new Error('Database não conectado');
  
  const start = Date.now();
  try {
    const result = await pool.query(text, params);
    const duration = Date.now() - start;
    if (duration > 1000) {
      console.warn(`⚠️ Query lenta (${duration}ms):`, text.substring(0, 100));
    }
    return result;
  } catch (err) {
    console.error('❌ Erro na query:', err.message);
    throw err;
  }
}

// Buscar uma linha
async function queryOne(text, params) {
  const result = await query(text, params);
  return result.rows[0] || null;
}

// Buscar múltiplas linhas
async function queryMany(text, params) {
  const result = await query(text, params);
  return result.rows || [];
}

// Transação
async function transaction(callback) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await callback({
      query: (text, params) => client.query(text, params),
      queryOne: async (text, params) => {
        const res = await client.query(text, params);
        return res.rows[0] || null;
      },
      queryMany: async (text, params) => {
        const res = await client.query(text, params);
        return res.rows || [];
      }
    });
    await client.query('COMMIT');
    return result;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

// Fechar conexões
async function closeDatabase() {
  if (pool) {
    await pool.end();
    console.log('🔌 Conexões do banco fechadas');
  }
}

// Verificar se está conectado
function isConnected() {
  return pool !== null;
}

// Seed do admin padrão (se não existir)
async function seedAdmin() {
  const adminUsername = process.env.ADMIN_USERNAME;
  const adminPassword = process.env.ADMIN_PASSWORD;
  
  if (!adminUsername || !adminPassword) {
    console.warn('⚠️ ADMIN_USERNAME e ADMIN_PASSWORD não definidos');
    return;
  }

  const bcrypt = require('bcryptjs');
  
  try {
    const existingAdmin = await queryOne(
      'SELECT id FROM usuarios WHERE username = $1',
      [adminUsername]
    );

    if (!existingAdmin) {
      // Criar empresa padrão se não existir
      const empresa = await queryOne(
        'SELECT id FROM empresas WHERE login = $1',
        [adminUsername]
      );

      let empresaId;
      if (!empresa) {
        const novaEmpresa = await queryOne(
          `INSERT INTO empresas (nome, slug, login, senha, ativa) 
           VALUES ($1, $2, $3, $4, TRUE) 
           RETURNING id`,
          ['Empresa Padrão', 'empresa-padrao', adminUsername, bcrypt.hashSync(adminPassword, 10)]
        );
        empresaId = novaEmpresa.id;
      } else {
        empresaId = empresa.id;
      }

      // Criar admin
      await query(
        `INSERT INTO usuarios (empresa_id, username, nome, password, role, ativo)
         VALUES ($1, $2, $3, $4, 'admin', TRUE)`,
        [empresaId, adminUsername, 'Administrador', bcrypt.hashSync(adminPassword, 10)]
      );
      
      console.log(`👤 Admin criado: ${adminUsername}`);
    } else {
      console.log(`👤 Admin "${adminUsername}" já existe`);
    }
  } catch (err) {
    console.error('❌ Erro ao criar admin:', err.message);
  }
}

module.exports = {
  connectDatabase,
  query,
  queryOne,
  queryMany,
  transaction,
  closeDatabase,
  isConnected,
  seedAdmin
};
