'use strict';

const { hashToken, deviceAuth } = require('./auth');

/**
 * Device-facing API routes (called by the Android app).
 */
function registerDeviceRoutes(fastify, db) {

  // ── Register a new device ──────────────────────────────────────────
  fastify.post('/api/device/register', {
    schema: {
      body: {
        type: 'object',
        required: ['device_id', 'device_token', 'nickname'],
        properties: {
          device_id: { type: 'string' },
          device_token: { type: 'string' },
          nickname: { type: 'string' },
        },
      },
    },
  }, (request, reply) => {
    const { device_id, device_token, nickname } = request.body;
    const hash = hashToken(device_token);

    const existing = db.prepare('SELECT device_id FROM devices WHERE device_id = ?').get(device_id);
    if (existing) {
      db.prepare('UPDATE devices SET nickname = ?, device_token_hash = ? WHERE device_id = ?')
        .run(nickname, hash, device_id);
    } else {
      db.prepare('INSERT INTO devices (device_id, nickname, device_token_hash) VALUES (?, ?, ?)')
        .run(device_id, nickname, hash);
    }

    return { ok: true };
  });

  // ── Snapshot upload (core) ─────────────────────────────────────────
  fastify.post('/api/device/snapshot', {
    preHandler: deviceAuth(db),
    schema: {
      body: {
        type: 'object',
        required: ['device_id', 'date', 'apps'],
        properties: {
          device_id: { type: 'string' },
          date: { type: 'string' },
          perm_status: { type: 'object' },
          apps: {
            type: 'array',
            items: {
              type: 'object',
              required: ['package', 'used_ms'],
              properties: {
                package: { type: 'string' },
                app_label: { type: 'string' },
                used_ms: { type: 'integer', minimum: 0 },
              },
            },
          },
        },
      },
    },
  }, (request, reply) => {
    const { device_id, date, apps, perm_status } = request.body;
    const now = new Date().toISOString();

    const upsert = db.prepare(`
      INSERT INTO usage_daily_app (device_id, date, package, app_label, used_ms, updated_at)
      VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT(device_id, date, package)
      DO UPDATE SET
        used_ms    = MAX(usage_daily_app.used_ms, excluded.used_ms),
        app_label  = excluded.app_label,
        updated_at = excluded.updated_at
    `);

    const tx = db.transaction(() => {
      for (const app of apps) {
        upsert.run(device_id, date, app.package, app.app_label || '', app.used_ms, now);
      }
      db.prepare('UPDATE devices SET last_seen_at = ?, last_perm_json = ? WHERE device_id = ?')
        .run(now, perm_status ? JSON.stringify(perm_status) : null, device_id);
    });

    tx();
    return { ok: true, received: apps.length };
  });
}

module.exports = { registerDeviceRoutes };
