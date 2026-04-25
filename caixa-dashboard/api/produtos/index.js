const jwt = require('jsonwebtoken');
import redis from '../lib/db.js';

export default async function handler(req, res) {
  if (req.method === 'GET') {
    try {
      const produtos = await redis.get('produtos') || [];
      res.json(produtos);
    } catch (err) {
      res.status(500).json({ error: 'Erro ao buscar produtos' });
    }
  } else if (req.method === 'POST') {
    const token = req.headers['authorization']?.split(' ')[1];
    const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

    if (!token) {
      return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
      jwt.verify(token, JWT_SECRET);
      
      const produtos = await redis.get('produtos') || [];
      const produto = {
        id: Date.now(),
        ...req.body,
        createdAt: new Date()
      };
      
      produtos.push(produto);
      await redis.set('produtos', produtos);
      res.json(produto);
    } catch (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
