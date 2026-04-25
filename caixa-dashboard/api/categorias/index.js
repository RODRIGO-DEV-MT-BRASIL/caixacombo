const jwt = require('jsonwebtoken');

export default async function handler(req, res) {
  if (req.method === 'GET') {
    // Depois migrar para banco de dados
    const categorias = [];
    res.json(categorias);
  } else if (req.method === 'POST') {
    const token = req.headers['authorization']?.split(' ')[1];
    const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

    if (!token) {
      return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
      jwt.verify(token, JWT_SECRET);
      
      const categoria = {
        id: Date.now(),
        nome: req.body.nome,
        descricao: req.body.descricao,
        createdAt: new Date()
      };

      // Depois salvar no banco de dados
      res.json(categoria);
    } catch (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
