// RideX k6 load test.
//
// Exercises the hot read paths behind a JWT:
//   - POST /api/auth/login        (auth + BCrypt verify)
//   - GET  /api/pricing/quote     (haversine + geo nearby count + surge)
//   - GET  /api/drivers/nearby    (2dsphere geoNear query)
//
// Run (server must be up on :8080, Mongo running):
//   k6 run load-test/ridex-load.js
//
// Tunables via env:
//   BASE_URL   (default http://localhost:8080)
//   VUS        (default 20)   virtual users at peak
//   DURATION   (default 30s)  steady-state duration

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const PEAK_VUS = parseInt(__ENV.VUS || '20');
const DURATION = __ENV.DURATION || '30s';

// Custom per-endpoint latency trends.
const loginTrend = new Trend('ridex_login_ms', true);
const quoteTrend = new Trend('ridex_quote_ms', true);
const nearbyTrend = new Trend('ridex_nearby_ms', true);

export const options = {
  stages: [
    { duration: '10s', target: PEAK_VUS },   // ramp up
    { duration: DURATION, target: PEAK_VUS }, // steady state
    { duration: '5s', target: 0 },            // ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],           // <1% errors
    http_req_duration: ['p(95)<800'],         // 95% under 800ms
  },
};

const CENTER = { lng: 77.5946, lat: 12.9716 };
const DROP = { lng: 77.64, lat: 12.95 };

// Each VU logs in once and reuses the token.
export function setup() {
  const res = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ email: 'rider1@test.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(res, { 'setup login ok': (r) => r.status === 200 });
  return { token: res.json('token') };
}

export default function (data) {
  const authHeaders = { headers: { Authorization: `Bearer ${data.token}` } };

  // 1) login (measures auth path under load)
  const login = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ email: 'rider1@test.com', password: 'password123' }),
    { headers: { 'Content-Type': 'application/json' } });
  loginTrend.add(login.timings.duration);
  check(login, { 'login 200': (r) => r.status === 200 });

  // 2) fare quote (geo + pricing)
  const quoteUrl = `${BASE}/api/pricing/quote?pickupLng=${CENTER.lng}&pickupLat=${CENTER.lat}` +
    `&dropoffLng=${DROP.lng}&dropoffLat=${DROP.lat}`;
  const quote = http.get(quoteUrl, authHeaders);
  quoteTrend.add(quote.timings.duration);
  check(quote, { 'quote 200': (r) => r.status === 200 });

  // 3) nearby drivers (2dsphere geoNear)
  const nearbyUrl = `${BASE}/api/drivers/nearby?longitude=${CENTER.lng}&latitude=${CENTER.lat}&radiusKm=5`;
  const nearby = http.get(nearbyUrl, authHeaders);
  nearbyTrend.add(nearby.timings.duration);
  check(nearby, { 'nearby 200': (r) => r.status === 200 });

  sleep(0.5);
}
