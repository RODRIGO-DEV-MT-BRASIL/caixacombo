const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');

const DATA_FILE = path.join(__dirname, 'data.json');

function updatePassword() {
  try {
    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    const userIdx = data.usuarios.findIndex(u => u.username === 'rodrigodevmt');
    
    const hashedPassword = bcrypt.hashSync('1985', 10);
    
    if (userIdx !== -1) {
      data.usuarios[userIdx].password = hashedPassword;
      console.log('Usuário rodrigodevmt atualizado com a nova senha.');
    } else {
      data.usuarios.push({
        id: Date.now(),
        username: 'rodrigodevmt',
        password: hashedPassword,
        role: 'admin',
        createdAt: new Date()
      });
      console.log('Usuário rodrigodevmt criado com a nova senha.');
    }
    
    fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
  } catch (e) {
    console.error('Erro ao atualizar senha:', e);
  }
}

updatePassword();
