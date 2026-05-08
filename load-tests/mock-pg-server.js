#!/usr/bin/env node
'use strict';

const http = require('http');
const { URL } = require('url');

const port = Number(process.env.PORT || 18080);
const baseLatencyMs = Number(process.env.PG_LATENCY_MS || 300);
const jitterMs = Number(process.env.PG_JITTER_MS || 0);
const errorRate = Number(process.env.PG_ERROR_RATE || 0);
const timeoutRate = Number(process.env.PG_TIMEOUT_RATE || 0);
const timeoutMs = Number(process.env.PG_TIMEOUT_MS || 30000);

const buckets = [50, 100, 200, 300, 500, 1000, 2000, 3000, 5000, 10000, 30000];
const metrics = {
  confirmCount: 0,
  cancelCount: 0,
  errorCount: 0,
  timeoutCount: 0,
  latencyCount: 0,
  latencySumMs: 0,
  bucketCounts: Object.fromEntries(buckets.map((b) => [b, 0])),
};

function latency() {
  if (jitterMs <= 0) return baseLatencyMs;
  return Math.max(0, baseLatencyMs + Math.floor((Math.random() * 2 - 1) * jitterMs));
}

function observe(ms) {
  metrics.latencyCount += 1;
  metrics.latencySumMs += ms;
  for (const b of buckets) {
    if (ms <= b) metrics.bucketCounts[b] += 1;
  }
}

function readJson(req) {
  return new Promise((resolve) => {
    let raw = '';
    req.on('data', (chunk) => { raw += chunk; });
    req.on('end', () => {
      try {
        resolve(raw ? JSON.parse(raw) : {});
      } catch (_) {
        resolve({});
      }
    });
  });
}

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

function prometheus() {
  const lines = [];
  const total = metrics.confirmCount + metrics.cancelCount;
  lines.push('# TYPE mock_pg_confirm_requests_total counter');
  lines.push(`mock_pg_confirm_requests_total ${metrics.confirmCount}`);
  lines.push('# TYPE mock_pg_cancel_requests_total counter');
  lines.push(`mock_pg_cancel_requests_total ${metrics.cancelCount}`);
  lines.push('# TYPE mock_pg_errors_total counter');
  lines.push(`mock_pg_errors_total ${metrics.errorCount}`);
  lines.push('# TYPE mock_pg_timeouts_total counter');
  lines.push(`mock_pg_timeouts_total ${metrics.timeoutCount}`);
  lines.push('# TYPE mock_pg_latency_ms histogram');
  for (const b of buckets) {
    lines.push(`mock_pg_latency_ms_bucket{le="${b}"} ${metrics.bucketCounts[b]}`);
  }
  lines.push(`mock_pg_latency_ms_bucket{le="+Inf"} ${metrics.latencyCount}`);
  lines.push(`mock_pg_latency_ms_count ${metrics.latencyCount}`);
  lines.push(`mock_pg_latency_ms_sum ${metrics.latencySumMs}`);
  return lines.join('\n') + '\n';
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);

  if (req.method === 'GET' && url.pathname === '/metrics') {
    res.writeHead(200, { 'Content-Type': 'text/plain; version=0.0.4' });
    res.end(prometheus());
    return;
  }

  if (req.method === 'GET' && url.pathname === '/health') {
    sendJson(res, 200, { status: 'UP' });
    return;
  }

  if (req.method !== 'POST') {
    sendJson(res, 404, { message: 'not found' });
    return;
  }

  const body = await readJson(req);
  const started = Date.now();
  const delayMs = latency();
  const isConfirm = url.pathname.endsWith('/confirm');
  const isCancel = /\/v1\/payments\/[^/]+\/cancel$/.test(url.pathname) || /\/payments\/[^/]+\/cancel$/.test(url.pathname);

  if (isConfirm) metrics.confirmCount += 1;
  if (isCancel) metrics.cancelCount += 1;

  if (Math.random() < timeoutRate) {
    metrics.timeoutCount += 1;
    setTimeout(() => {
      observe(Date.now() - started);
      sendJson(res, 504, { code: 'MOCK_PG_TIMEOUT', message: 'mock timeout' });
    }, timeoutMs);
    return;
  }

  setTimeout(() => {
    const elapsed = Date.now() - started;
    observe(elapsed);

    if (Math.random() < errorRate) {
      metrics.errorCount += 1;
      sendJson(res, 500, { code: 'MOCK_PG_ERROR', message: 'mock error' });
      return;
    }

    if (isConfirm) {
      sendJson(res, 200, {
        paymentKey: body.paymentKey || 'mock-payment-key',
        orderId: body.orderId || 'mock-order-id',
        orderName: 'k6 mock order',
        totalAmount: Number(body.amount || 0),
        status: 'DONE',
        method: 'CARD',
        approvedAt: new Date().toISOString(),
        receipt: { url: `http://localhost:${port}/receipt/${encodeURIComponent(body.paymentKey || 'mock')}` },
      });
      return;
    }

    if (isCancel) {
      sendJson(res, 200, {
        paymentKey: decodeURIComponent(url.pathname.split('/').at(-2)),
        orderId: body.orderId || 'mock-cancel-order',
        status: 'CANCELED',
        method: 'CARD',
        totalAmount: Number(body.amount || 0),
        receipt: { url: `http://localhost:${port}/cancel-receipt` },
        cancels: [{
          cancelAmount: Number(body.amount || 0),
          canceledAt: new Date().toISOString(),
          cancelReason: body.cancelReason || 'mock cancel',
        }],
      });
      return;
    }

    sendJson(res, 404, { message: 'not found' });
  }, delayMs);
});

server.listen(port, () => {
  console.log(`mock PG server listening port=${port} latencyMs=${baseLatencyMs} jitterMs=${jitterMs}`);
});
