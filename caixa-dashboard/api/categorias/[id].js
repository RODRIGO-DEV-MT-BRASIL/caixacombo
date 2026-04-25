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
    const categorias = await redis.get('categorias') || [];

    if (req.method === 'PUT') {
      const index = categorias.findIndex(c => c.id == id);
      if (index !== -1) {
        categorias[index] = { ...categorias[index], ...req.body };
        await redis.set('categorias', categorias);
        res.json(categorias[index]);
      } else {
        res.status(404).json({ error: 'Categoria não encontrada' });
      }
    } else if (req.method === 'DELETE') {
      const filtered = categorias.filter(c => c.id != id);
      await redis.set('categorias', filtered);
      res.json({ success: true });
    } else {
      res.status(405).json({ error: 'Method not allowed' });
    }
  } catch (err) {
    return res.status(403).json({ error: 'Token inválido' });
  }
}
