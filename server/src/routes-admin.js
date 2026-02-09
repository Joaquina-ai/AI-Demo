'use strict';

const { adminAuth } = require('./auth');

/**
 * Admin / Web-facing API routes (called by the Vue3 frontend).
 */
function registerAdminRoutes(fastify, db) {

  // All admin routes require X-ADMIN-KEY
  fastify.addHook('preHandler', adminAuth);

  // ── List all devices ───────────────────────────────────────────────
  fastify.get('/api/admin/devices', (request, reply) => {
    const rows = db.prepare(`
      SELECT device_id, nickname, last_seen_at, last_perm_json
      FROM devices ORDER BY last_seen_at DESC
    `).all();
    return { devices: rows };
  });

  // ── Today usage for a device ───────────────────────────────────────
  fastify.get('/api/admin/devices/:deviceId/today', (request, reply) => {
    const { deviceId } = request.params;
    const today = new Date().toISOString().slice(0, 10);

    const rows = db.prepare(`
      SELECT package, app_label, used_ms, updated_at
      FROM usage_daily_app
      WHERE device_id = ? AND date = ?
      ORDER BY used_ms DESC
    `).all(deviceId, today);

    return { date: today, apps: rows };
  });

  // ── Weekly usage for a device ──────────────────────────────────────
  fastify.get('/api/admin/devices/:deviceId/weekly', (request, reply) => {
    const { deviceId } = request.params;

    // Last 7 days including today
    const dates = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      dates.push(d.toISOString().slice(0, 10));
    }

    const rows = db.prepare(`
      SELECT date, package, app_label, used_ms
      FROM usage_daily_app
      WHERE device_id = ? AND date >= ? AND date <= ?
      ORDER BY date ASC, used_ms DESC
    `).all(deviceId, dates[0], dates[dates.length - 1]);

    // Group by date
    const byDate = {};
    for (const d of dates) byDate[d] = { date: d, total_ms: 0, apps: [] };
    for (const row of rows) {
      if (byDate[row.date]) {
        byDate[row.date].total_ms += row.used_ms;
        byDate[row.date].apps.push({
          package: row.package,
          app_label: row.app_label,
          used_ms: row.used_ms,
        });
      }
    }

    return { days: Object.values(byDate) };
  });

  // ── Single day detail ──────────────────────────────────────────────
  fastify.get('/api/admin/devices/:deviceId/date/:date', (request, reply) => {
    const { deviceId, date } = request.params;
    const rows = db.prepare(`
      SELECT package, app_label, used_ms, updated_at
      FROM usage_daily_app
      WHERE device_id = ? AND date = ?
      ORDER BY used_ms DESC
    `).all(deviceId, date);

    return { date, apps: rows };
  });
}

module.exports = { registerAdminRoutes };
