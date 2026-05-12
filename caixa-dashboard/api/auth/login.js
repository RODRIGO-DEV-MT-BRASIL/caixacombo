const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { MongoClient } = require('mongodb');

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

      const token = jwt.sign(
        { id: funcionario._id.toString(), email: funcionario.email, role: 'funcionario', funcionarioId: funcionario._id.toString() },
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
          codigo: funcionario.codigo
        }
      });
    } catch (error) {
      console.error('Erro ao fazer login de funcionário:', error);
      return res.status(500).json({ error: 'Erro ao fazer login' });
    }
  }

  // Login de admin (username + password)
  const user = {
    id: 1,
    username: 'rodrigodevmt',
    password: bcrypt.hashSync('1985', 10),
    role: 'admin'
  };

  if (username !== user.username) {
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
