const jwt = require('jsonwebtoken');
import { put } from '@vercel/blob';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const token = req.headers['authorization']?.split(' ')[1];
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  if (!token) {
    return res.status(401).json({ error: 'Não autorizado' });
  }

  try {
    jwt.verify(token, JWT_SECRET);
    
    const { base64 } = req.body;
    
    // Converter base64 para buffer
    const buffer = Buffer.from(base64.split(',')[1], 'base64');
    
    const blob = await put(`produtos/${Date.now()}.jpg`, buffer, {
      access: 'public',
    });
    
    res.json({ url: blob.url });
  } catch (err) {
    console.error('Erro ao fazer upload:', err);
    return res.status(500).json({ error: 'Erro ao fazer upload' });
  }
}
