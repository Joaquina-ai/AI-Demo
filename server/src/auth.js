'use strict';

const crypto = require('crypto');

const ADMIN_KEY = process.env.ADMIN_KEY || 'changeme-admin-key';

function hashToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

/** Fastify hook: verify X-DEVICE-TOKEN against stored hash */
function deviceAuth(db) {
  return async (request, reply) => {
    const token = request.headers['x-device-token'];
    const deviceId = request.body?.device_id || request.params?.deviceId;
    if (!token || !deviceId) {
      return reply.code(401).send({ error: 'missing credentials' });
    }
    const row = db.prepare('SELECT device_token_hash FROM devices WHERE device_id = ?').get(deviceId);
    if (!row) {
      return reply.code(404).send({ error: 'device not registered' });
    }
    if (row.device_token_hash !== hashToken(token)) {
      return reply.code(403).send({ error: 'invalid token' });
    }
  };
}

/** Fastify hook: verify X-ADMIN-KEY */
function adminAuth(request, reply, done) {
  const key = request.headers['x-admin-key'];
  if (key !== ADMIN_KEY) {
    reply.code(403).send({ error: 'invalid admin key' });
    return;
  }
  done();
}

module.exports = { hashToken, deviceAuth, adminAuth, ADMIN_KEY };
