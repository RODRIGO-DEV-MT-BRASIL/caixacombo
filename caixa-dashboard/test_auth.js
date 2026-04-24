const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, 'data.json');
const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
const user = data.usuarios.find(u => u.username === 'rodrigodevmt');

console.log('Usuário encontrado:', user ? 'Sim' : 'Não');
if (user) {
  const match = bcrypt.compareSync('1985', user.password);
  console.log('Senha "1985" coincide?', match ? 'SIM' : 'NÃO');
}
