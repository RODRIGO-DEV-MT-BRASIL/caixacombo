const jwt = require('jsonwebtoken');

export default async function handler(req, res) {
  if (req.method === 'GET') {
    // Depois migrar para banco de dados
    const produtos = [];
    res.json(produtos);
  } else if (req.method === 'POST') {
    const token = req.headers['authorization']?.split(' ')[1];
    const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

    if (!token) {
      return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
      jwt.verify(token, JWT_SECRET);
      
      const produto = {
        id: Date.now(),
        ...req.body,
        createdAt: new Date()
      };

      // Depois salvar no banco de dados
      res.json(produto);
    } catch (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
