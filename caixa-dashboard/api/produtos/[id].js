const jwt = require('jsonwebtoken');

export default async function handler(req, res) {
  const token = req.headers['authorization']?.split(' ')[1];
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  if (!token) {
    return res.status(401).json({ error: 'Não autorizado' });
  }

  try {
    jwt.verify(token, JWT_SECRET);
    
    const { id } = req.query;

    if (req.method === 'PUT') {
      // Depois atualizar no banco de dados
      res.json({ id, ...req.body });
    } else if (req.method === 'DELETE') {
      // Depois deletar do banco de dados
      res.json({ success: true });
    } else {
      res.status(405).json({ error: 'Method not allowed' });
    }
  } catch (err) {
    return res.status(403).json({ error: 'Token inválido' });
  }
}
