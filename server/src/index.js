'use strict';

const path = require('path');
const Fastify = require('fastify');
const fastifyStatic = require('@fastify/static');
const fastifyCors = require('@fastify/cors');
const { getDb, close } = require('./db');
const { registerDeviceRoutes } = require('./routes-device');
const { registerAdminRoutes } = require('./routes-admin');

const PORT = parseInt(process.env.PORT, 10) || 3000;
const HOST = process.env.HOST || '0.0.0.0';

async function main() {
  const fastify = Fastify({ logger: true });
  const db = getDb();

  await fastify.register(fastifyCors, { origin: true });

  // ── Device routes (no prefix) ──────────────────────────────────────
  registerDeviceRoutes(fastify, db);

  // ── Admin routes (no prefix) ───────────────────────────────────────
  fastify.register(async (instance) => {
    registerAdminRoutes(instance, db);
  });

  // ── Serve Vue3 static build ────────────────────────────────────────
  const webDistPath = path.join(__dirname, '..', '..', 'web', 'dist');
  try {
    await fastify.register(fastifyStatic, {
      root: webDistPath,
      prefix: '/',
      wildcard: false,
    });
    // SPA fallback: serve index.html for all non-API routes
    fastify.setNotFoundHandler((request, reply) => {
      if (request.url.startsWith('/api/')) {
        reply.code(404).send({ error: 'not found' });
      } else {
        reply.sendFile('index.html');
      }
    });
  } catch {
    // web/dist may not exist yet during development
    fastify.setNotFoundHandler((request, reply) => {
      reply.code(404).send({ error: 'not found' });
    });
  }

  // ── Graceful shutdown ──────────────────────────────────────────────
  const shutdown = async () => {
    await fastify.close();
    close();
    process.exit(0);
  };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);

  await fastify.listen({ port: PORT, host: HOST });
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
