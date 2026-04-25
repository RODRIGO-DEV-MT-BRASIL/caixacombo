const Pusher = require('pusher');

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const pusher = new Pusher({
    appId: process.env.PUSHER_APP_ID,
    key: process.env.PUSHER_APP_KEY,
    secret: process.env.PUSHER_APP_SECRET,
    cluster: process.env.PUSHER_CLUSTER,
    useTLS: true
  });

  const { channel, event, data } = req.body;

  try {
    await pusher.trigger(channel, event, data);
    res.json({ success: true });
  } catch (error) {
    console.error('Erro ao enviar evento Pusher:', error);
    res.status(500).json({ error: 'Erro ao enviar evento' });
  }
}
