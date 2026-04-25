const jwt = require('jsonwebtoken');
import redis from '../lib/db.js';

export default async function handler(req, res) {
  const token = req.headers['authorization']?.split(' ')[1];
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  if (!token) {
    return res.status(401).json({ error: 'Não autorizado' });
  }

  try {
    jwt.verify(token, JWT_SECRET);
    
    const { id } = req.query;
    const produtos = await redis.get('produtos') || [];

    if (req.method === 'PUT') {
      const index = produtos.findIndex(p => p.id == id);
      if (index !== -1) {
        produtos[index] = { ...produtos[index], ...req.body };
        await redis.set('produtos', produtos);
        res.json(produtos[index]);
      } else {
        res.status(404).json({ error: 'Produto não encontrado' });
      }
    } else if (req.method === 'DELETE') {
      const filtered = produtos.filter(p => p.id != id);
      await redis.set('produtos', filtered);
      res.json({ success: true });
    } else {
      res.status(405).json({ error: 'Method not allowed' });
    }
  } catch (err) {
    return res.status(403).json({ error: 'Token inválido' });
  }
}
