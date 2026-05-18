const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { MongoClient, ObjectId } = require('mongodb');

// Endpoint de login que aceita apenas email + senha.
// Suporta senhas hasheadas (`password`) e PIN legado (`pin`) para compatibilidade.
export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { email, password } = req.body;
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  const providedEmail = (email || '').toString().trim().toLowerCase();
  const providedPassword = (password || '').toString().trim();

  if (!providedEmail || !providedPassword) {
    return res.status(400).json({ error: 'Forneça email e senha' });
  }

  const client = new MongoClient(process.env.MONGODB_URI || 'mongodb://localhost:27017');
  try {
    await client.connect();
    const db = client.db();

    // Tentar funcionário primeiro
    const funcionario = await db.collection('funcionarios').findOne({ email: providedEmail, ativo: true });

    if (funcionario) {
      // aceita campo `password` (hash) ou `pin` legado
      let ok = false;
      if (funcionario.password) {
        ok = bcrypt.compareSync(providedPassword, funcionario.password);
      } else if (funcionario.pin) {
        ok = providedPassword === funcionario.pin.toString();
      }

      if (!ok) {
        await client.close();
        return res.status(401).json({ error: 'Credenciais inválidas' });
      }

      // buscar config da empresa
      let empresaConfig = {};
      if (funcionario.empresaId) {
        const empresa = await db.collection('empresas').findOne({ _id: new ObjectId(funcionario.empresaId) });
        if (empresa) {
          empresaConfig = {
            primaryColor: empresa.primaryColor || '#3b82f6',
            secondaryColor: empresa.secondaryColor || '#06b6d4',
            accentColor: empresa.accentColor || '#10b981',
            logoUrl: empresa.logoUrl || '',
            nome: empresa.nome || '',
            empresaId: empresa._id.toString()
          };
        }
      }

      const token = jwt.sign(
        { id: funcionario._id.toString(), email: funcionario.email, role: 'funcionario', funcionarioId: funcionario._id.toString(), empresaId: funcionario.empresaId },
        JWT_SECRET,
        { expiresIn: '24h' }
      );

      await client.close();
      return res.json({
        token,
        user: {
          id: funcionario._id.toString(),
          email: funcionario.email,
          nome: funcionario.nome,
          role: 'funcionario',
          permissoes: funcionario.permissoes,
          codigo: funcionario.codigo,
          empresaId: funcionario.empresaId,
          branding: empresaConfig
        }
      });
    }

    // Se não for funcionário, tentar usuário/admin na coleção 'usuarios'
    const admin = await db.collection('usuarios').findOne({ email: providedEmail, ativo: true });
    if (!admin) {
      await client.close();
      return res.status(401).json({ error: 'Credenciais inválidas' });
    }

    const okAdmin = admin.password ? bcrypt.compareSync(providedPassword, admin.password) : (providedPassword === (admin.password_plain || '').toString());
    if (!okAdmin) {
      await client.close();
      return res.status(401).json({ error: 'Credenciais inválidas' });
    }

    const token = jwt.sign(
      { id: admin._id.toString(), email: admin.email, role: admin.role || 'admin' },
      JWT_SECRET,
      { expiresIn: '24h' }
    );

    await client.close();
    return res.json({ token, user: { id: admin._id.toString(), email: admin.email, nome: admin.nome || admin.username, role: admin.role || 'admin' } });
  } catch (err) {
    console.error('Erro no login:', err);
    try { await client.close(); } catch (e) {}
    return res.status(500).json({ error: 'Erro ao fazer login' });
  }
}
