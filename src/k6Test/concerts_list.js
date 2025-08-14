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
const PATH = __ENV.PATH || '/concerts?size=20&page=0';

export default function () {
  const res = http.get(`${BASE}${PATH}`);
  check(res, { '200 OK': r => r.status === 200 });
}
