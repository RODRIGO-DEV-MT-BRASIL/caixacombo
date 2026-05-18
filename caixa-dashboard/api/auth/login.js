const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { MongoClient, ObjectId } = require('mongodb');

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { username, password, email, pin } = req.body;
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  // Verificar se é login de funcionário (email + PIN)
  if (email && pin) {
    try {
      const client = new MongoClient(process.env.MONGODB_URI || 'mongodb://localhost:27017');
      await client.connect();
      const db = client.db();
      
      const funcionario = await db.collection('funcionarios').findOne({ email, pin, ativo: true });
      
      if (!funcionario) {
        await client.close();
        return res.status(401).json({ error: 'Credenciais inválidas' });
      }

      // Buscar configurações da empresa do funcionário
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
    } catch (error) {
      console.error('Erro ao fazer login de funcionário:', error);
      return res.status(500).json({ error: 'Erro ao fazer login' });
    }
  }

  // Login de admin (username + password) - consulta banco de dados
  const user = db.usuarios.find(u => u.username === username);
  
  if (!user) {
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }

  if (!bcrypt.compareSync(password, user.password)) {
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.json({
    token,
    user: { id: user.id, username: user.username, role: user.role }
  });
}
