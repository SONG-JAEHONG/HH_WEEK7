import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    ramp: {
      executor: 'ramping-arrival-rate',
      startRate: 100, timeUnit: '1s',
      preAllocatedVUs: 200, maxVUs: 2000,
      stages: [
        { target: 200, duration: '30s' },
        { target: 500, duration: '30s' },
        { target: 1000, duration: '30s' },
        { target: 0, duration: '10s' },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    checks: ['rate>0.99'],
  },
};

const BASE = __ENV.BASE || 'http://localhost:8080';
const ID_START = Number(__ENV.ID_START || '2');
const ID_COUNT = Number(__ENV.ID_COUNT || '1000');

function pickId() {
  return ID_START + Math.floor(Math.random() * ID_COUNT);
}

export default function () {
  const id = pickId();
  const res = http.get(`${BASE}/concerts/${id}/dates`);
  check(res, { '200 OK': r => r.status === 200 });
}
