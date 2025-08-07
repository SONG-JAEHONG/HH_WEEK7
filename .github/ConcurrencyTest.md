# 유저 포인트 충전 기능(낙관적 락)



## 포인트 충전에서 락이 필요한 이유

- 여러 트랜잭션이 동시에 접근해 **중복 충전, 값 덮어쓰기** 같은 문제가 발생할 수 있음

```
트랜잭션 1: 포인트 조회 (1000)
트랜잭션 2: 포인트 조회 (1000)
트랜잭션 1: +1000 → 저장 (2000)
트랜잭션 2: +1000 → 저장 (2000)
->  충전 시도 금액 2000 ,실제 충전금액 1000 (중복 충전 누락)
```

- 위와 같은 상황을 방지하려면 동시성 제어가 필요하고, 그 수단으로 **낙관적 락(Optimistic Lock)** 적용

---

## 포인트 충전은 "자원 선점"이 아니다

- 포인트 충전은 좌석 예약처럼 **희소한 자원을 여러 사용자가 경쟁하는 상황이 아님**
- 같은 유저에 대해 여러 요청이 동시 들어오더라도 **각 요청이 독립적으로 수행될 수 있음**
- **동시에 성공해도 상관없음**  
  → 단, 충돌은 발생할 수 있으므로 **정합성을 위한 최소한의 동시성 제어는 필요**

---

## 충돌은 가능하지만, 발생 확률과 비용이 낮음

- 같은 유저에 대해 동시에 포인트를 변경하는 경우는 상대적으로 **드문 상황**(따닥, 정도)
- 충돌이 발생하더라도 낙관적 락은 **version 체크로 간단함**
- 포인트 충전은 `+=` 단순 연산 → 이전 상태에 덧셈만 하는 구조이기 떄문에 재시도 로직 쉬움, 롤백시 DB 관점에서도 비교적 부담이 크지 않을 것으로 예상

--> **포인트 충전은 희소 자원의 선점이 아니기 때문에, 충돌 발생 가능성이 낮고 재시도 로직으로 충분히 대응 가능하여 낙관적 락(Optimistic Lock)이 적합."**




# 예약 선점 기능 (비관적 락)

## 예약 선점 기능에서 락이 필요한 이유

- 좌석 예약은 **자원 선점 경쟁** -> 하나의 좌석에 대해 동시에 여러 사용자가 접근 가능.
- 락을 적용하지 않은 환경에서는 **중복 예약 또는 데이터 정합성 오류**가 발생 가능.
- 따라서 **"좌석 1개당 1개의 예약만 허용"**해야 하며, 이를 보장하려면 **동시성 제어** 필요.
- 즉, 트랜잭션 단위에서 **Row-Level Lock**을 걸어 **선점 성공자만 저장**.

---

## 낙관적 락이 아닌 비관적 락을 선택한 이유

###경합이 매우 높은 상황
- 티켓팅은 다수의 사용자가 동시에 특정 좌석을 선점하려는 상황으로 **경합이 극심**.
- **낙관적 락은 충돌이 드물다는 전제에서 효과적**이지만, 이와 같은 시나리오에서는 **매 요청마다 충돌 가능성이 높음**.

###낙관적 락의 구조적 한계
- 낙관적 락은 트랜잭션 커밋 시점에 `@Version` 필드를 통해 충돌을 감지.
- 충돌 시점이 **트랜잭션의 끝부분**이기 때문에, 그 이전에 수행된 작업들이 롤백처리
- 이는  DB입장에서  **Undo Log, 락 해제, 트랜잭션 정리 등** 추가적인 비용 발생
-  **재시도 로직**을 구성시, **부하 증가**
   ###비관적 락의 실용적 장점
- 비관적 락은 `SELECT ... FOR UPDATE`를 통해 **트랜잭션 초반에 row-level 락을 선점**한다.
- **락을 선점하지 못한 요청은 대기** -> 대기시간(timeout)을 설정하여 즉시 실패 처리 가능**
- **경쟁자가 많을수록 실패 요청을 빠르게 차단할 수 있음**
  -트랜잭션이 **순차 처리**되기 때문에 **처리량이 감소**할 수 있으나, **데이터 정합성은 철저히 보장**
---

-> 결론 :  **"하나의 좌석에 단 하나의 예약만 허용해야 하며, 경합이 강한 기능이므로, 데이터 정합성과 시스템 안정성을 보장하기 위해 비관적 락(Pessimistic Lock)을 선택."**

---
## 트랜잭션 분리 전/ 후 K6 테스트

| 항목                       | 분리 전 (트랜잭션 전체) | 분리 후 (Seat만 분리) | 비고 |
|----------------------------|--------------------------|------------------------|------|
| 요청 수                    | 100,000                  | 100,000                | 동일한 부하 조건 |
| 성공 응답 (HTTP 200)      | 1건 (0.00%)              | 1건 (0.00%)            | - |
| 실패 응답                 | 99,999건                 | 99,999건               | - |
| 실패율                    | 99.99%                   | 99.99%                 | - |
| 평균 응답 시간            | 0.268s                   | 0.860s                 | 락 대기 증가로 느려짐 |
| 성공 요청 응답 시간       | 4.54s                    | 5.86s                  | 락 획득 대기 시간 증가 |
| 평균 시나리오 실행 시간   | 8.85s                    | 11.78s                 | 평균 실행 시간 증가 |
| 최대 시나리오 실행 시간   | 17.46s                   | 23.94s                 | 최악의 대기 시간 증가 |
| 처리 속도 (req/s)         | 5,588 req/s              | 4,147 req/s            | 락 경합 증가로 감소 |

---

## 트랜잭션 구조 변화에 따른 영향 분석

### 분리 전
- Seat 조회부터 Reservation 저장까지 **하나의 트랜잭션 내에서 실행**됨
- 락이 오래 유지되어 **경합은 줄지만 병렬성은 낮음**
- **다른 요청은 아예 락 대기 상태에서 실패가 처리**됨

### 분리 후
- `seatHoldService.holdSeat()`만 분리하여 트랜잭션
- Seat에 대한 락은 빨리 풀리지만, 다른 쓰레드로 바로 진입하여 **더 많은 요청이 락 경합에 진입함**
- 이미 상태가 HOLDING이기 때문에 **실패는 그대로**, 단지 **경합과 부하만 증가**
- 처리 시간, 평균 응답 시간, 최대 시간 모두 악화

---

## 5. 결론 및 인사이트

- 단순히 트랜잭션을 분리한다고 성능이 항상 개선되진 않음
- **락이 빨리 풀리는 구조에서는 경쟁자 수가 늘어나면서 실패 요청도 많아지고, 시스템 전체 응답 시간이 길어짐**
- **앞 단에서의 대기열/예약열 구조로 경합 정도의 자체를 줄여야함**

---


<details>
<summary>트랜잭션 전체 적용 구조</summary>

<br>

```java
@Transactional
@Override
public ReservationResponse reserve(ReservationRequest reservationRequest, Long userId) {
    User user = userRepository.findUserById(userId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    Seat seat = seatRepository.findSeatById(reservationRequest.seatId())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

    if (!seat.isAvailable()) {
        throw new IllegalStateException("이미 예약 중인 좌석입니다.");
    }

    seat.hold();
    seatRepository.save(seat);

    ConcertDate concertDate = concertRepository.findConcertDateById(reservationRequest.concertDateId())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 날짜입니다."));

    Reservation reservation = Reservation.holding(user, concertDate, seat);
    reservationRepository.save(reservation);

    return new ReservationResponse(
        reservation.getId(),
        reservationRequest.seatId(),
        reservation.getStatus().name()
    );
}
```

</details>

<details>
<summary>트랜잭션 분리 구조</summary>

<br>

```java
@Override
public ReservationResponse reserve(ReservationRequest reservationRequest, Long userId) {
    User user = userRepository.findUserById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

    Seat seat = seatHoldService.holdSeat(reservationRequest.seatId());

    ConcertDate concertDate = concertRepository.findConcertDateById(reservationRequest.concertDateId())
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 날짜입니다."));

    Reservation reservation = Reservation.holding(user, concertDate, seat);
    reservationRepository.save(reservation);

    return new ReservationResponse(
            reservation.getId(),
            reservationRequest.seatId(),
            reservation.getStatus().name()
    );
}

@Transactional
public Seat holdSeat(Long seatId) {
    Seat seat = seatRepository.findSeatById(seatId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

    if (!seat.isAvailable()) {
        throw new IllegalStateException("이미 예약 중인 좌석입니다.");
    }

    seat.hold();
    seatRepository.save(seat);

    return seat;
}
```

</details>
