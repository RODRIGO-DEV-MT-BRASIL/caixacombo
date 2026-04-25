export default async function handler(req, res) {
  if (req.method === 'GET') {
    // Depois migrar para banco de dados
    res.json([]);
  } else if (req.method === 'POST') {
    const operacao = {
      id: Date.now(),
      ...req.body,
      createdAt: new Date()
    };
    // Depois salvar no banco de dados
    res.json(operacao);
  } else if (req.method === 'DELETE') {
    const { id } = req.query;
    // Depois deletar do banco de dados
    res.json({ success: true });
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
