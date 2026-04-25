const jwt = require('jsonwebtoken');
const { v2: cloudinary } = require('cloudinary').v2;

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

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
    
    const result = await cloudinary.uploader.upload(base64, {
      folder: 'caixa-combo/produtos',
      transformation: [
        { width: 500, height: 500, crop: 'limit' }
      ]
    });
    
    res.json({ url: result.secure_url });
  } catch (err) {
    console.error('Erro ao fazer upload:', err);
    return res.status(500).json({ error: 'Erro ao fazer upload' });
  }
}
