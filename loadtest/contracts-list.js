import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 200 },
    { duration: '30s', target: 1000 },
    { duration: '1m',  target: 1000 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed:   ['rate<0.10'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN    = __ENV.TOKEN;

export default function () {
  const res = http.get(`${BASE_URL}/contracts`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'has body':      (r) => r.body && r.body.length > 0,
  });
}
