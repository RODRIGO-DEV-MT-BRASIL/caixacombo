const jwt = require('jsonwebtoken');

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const token = req.headers['authorization']?.split(' ')[1];
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  if (!token) {
    return res.status(401).json({ error: 'Não autorizado' });
  }

  try {
    const user = jwt.verify(token, JWT_SECRET);
    res.json({ valid: true, user });
  } catch (err) {
    return res.status(403).json({ error: 'Token inválido' });
  }
}
