const jwt = require('jsonwebtoken');
import redis from '../lib/db.js';

export default async function handler(req, res) {
  if (req.method === 'GET') {
    try {
      const categorias = await redis.get('categorias') || [];
      res.json(categorias);
    } catch (err) {
      res.status(500).json({ error: 'Erro ao buscar categorias' });
    }
  } else if (req.method === 'POST') {
    const token = req.headers['authorization']?.split(' ')[1];
    const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

    if (!token) {
      return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
      jwt.verify(token, JWT_SECRET);
      
      const categorias = await redis.get('categorias') || [];
      const categoria = {
        id: Date.now(),
        nome: req.body.nome,
        descricao: req.body.descricao,
        createdAt: new Date()
      };
      
      categorias.push(categoria);
      await redis.set('categorias', categorias);
      res.json(categoria);
    } catch (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
