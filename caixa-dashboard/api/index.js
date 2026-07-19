const app = require('../server');

module.exports = (req, res) => {
  // Vercel serverless: passar req/res para o Express app
  return app(req, res);
};
