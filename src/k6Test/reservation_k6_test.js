import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 100000,
  iterations: 100000,
};

export default function () {
  const userId = __ITER + 1;

  const reservationPayload = JSON.stringify({
    userId: userId,
    concertDateId: 1,
    seatId: 1
  });

  const res = http.post('http://localhost:8080/reservation', reservationPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'status is 200': (r) => r.status === 200,
  });
}
