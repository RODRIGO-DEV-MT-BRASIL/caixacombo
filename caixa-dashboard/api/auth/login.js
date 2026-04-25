const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { username, password } = req.body;
  const JWT_SECRET = process.env.JWT_SECRET || 'caixacombo-secret-key';

  // Usuário hardcoded (depois migrar para banco de dados)
  const user = {
    id: 1,
    username: 'rodrigodevmt',
    password: bcrypt.hashSync('1985', 10),
    role: 'admin'
  };

  if (username !== user.username) {
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }

  if (!bcrypt.compareSync(password, user.password)) {
    return res.status(401).json({ error: 'Credenciais inválidas' });
  }

  const token = jwt.sign(
    { id: user.id, username: user.username, role: user.role },
    JWT_SECRET,
    { expiresIn: '24h' }
  );

  res.json({
    token,
    user: { id: user.id, username: user.username, role: user.role }
  });
}
