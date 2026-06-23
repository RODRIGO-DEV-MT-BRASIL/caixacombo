const readline = require('readline');
const bcrypt = require('bcryptjs');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

rl.question('Digite a nova senha: ', (senha) => {
  const hash = bcrypt.hashSync(senha, 10);
  console.log('\n--- Copie esta linha e cole no SQL Editor do Supabase ---\n');
  console.log("UPDATE usuarios SET password = '" + hash + "' WHERE username = 'admin';");
  console.log('\n----------------------------------------------------------\n');
  rl.close();
});
