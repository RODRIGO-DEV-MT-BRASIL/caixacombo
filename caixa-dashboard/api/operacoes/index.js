import redis from '../lib/db.js';

export default async function handler(req, res) {
  if (req.method === 'GET') {
    try {
      const operacoes = await redis.get('operacoes') || [];
      res.json(operacoes);
    } catch (err) {
      res.status(500).json({ error: 'Erro ao buscar operações' });
    }
  } else if (req.method === 'POST') {
    try {
      const operacoes = await redis.get('operacoes') || [];
      const operacao = {
        id: Date.now(),
        ...req.body,
        createdAt: new Date()
      };
      operacoes.push(operacao);
      await redis.set('operacoes', operacoes);
      res.json(operacao);
    } catch (err) {
      res.status(500).json({ error: 'Erro ao salvar operação' });
    }
  } else if (req.method === 'DELETE') {
    try {
      const { id } = req.query;
      const operacoes = await redis.get('operacoes') || [];
      const filtered = operacoes.filter(op => op.id != id);
      await redis.set('operacoes', filtered);
      res.json({ success: true });
    } catch (err) {
      res.status(500).json({ error: 'Erro ao deletar operação' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
