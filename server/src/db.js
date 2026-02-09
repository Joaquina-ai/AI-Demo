'use strict';

const Database = require('better-sqlite3');
const path = require('path');

const DB_PATH = process.env.DB_PATH || path.join(__dirname, '..', 'data', 'usage.db');

let db;

function getDb() {
  if (db) return db;

  const fs = require('fs');
  fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });

  db = new Database(DB_PATH);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  db.exec(`
    CREATE TABLE IF NOT EXISTS devices (
      device_id   TEXT PRIMARY KEY,
      nickname    TEXT NOT NULL DEFAULT '',
      device_token_hash TEXT NOT NULL,
      last_seen_at TEXT,
      last_perm_json TEXT
    );

    CREATE TABLE IF NOT EXISTS usage_daily_app (
      device_id  TEXT NOT NULL,
      date       TEXT NOT NULL,
      package    TEXT NOT NULL,
      app_label  TEXT NOT NULL DEFAULT '',
      used_ms    INTEGER NOT NULL DEFAULT 0,
      updated_at TEXT NOT NULL,
      PRIMARY KEY (device_id, date, package),
      FOREIGN KEY (device_id) REFERENCES devices(device_id)
    );

    CREATE INDEX IF NOT EXISTS idx_usage_device_date
      ON usage_daily_app(device_id, date);
  `);

  return db;
}

function close() {
  if (db) {
    db.close();
    db = null;
  }
}

module.exports = { getDb, close };
