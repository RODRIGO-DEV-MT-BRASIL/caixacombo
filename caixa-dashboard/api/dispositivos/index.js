const jwt = require('jsonwebtoken');
import redis from '../lib/db.js';

export default async function handler(req, res) {
  if (req.method === 'GET') {
    const token = req.headers['authorization']?.split(' ')[1];
    const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

    if (!token) {
      return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
      jwt.verify(token, JWT_SECRET);
      const dispositivos = await redis.get('dispositivos') || [];
      res.json(dispositivos);
    } catch (err) {
      return res.status(403).json({ error: 'Token inválido' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
