## 1) 전체 프로세스

1. **티켓 발급(대기열 등록)**
    - **중복검사**: “사용자당 1토큰” 보장.
    - **점수(score) 계산**: `ms = epoch_ms`, `seq = INCR seq:waiting:{ms}`(+ `EXPIRE 5s`),

      **score = ms * FACTOR + seq** (FACTOR 예: 1_000 또는 1_000_000).

    - **등록**: `z:waiting`에 `(member=token, score=위 계산값)` 삽입.
    - **메타 기록**: `h:ticket:{token}`에 유저/상태/타임스탬프 저장.
    - **응답**: `{ token, rank=ZRANK(z:waiting, token)+1 }`.
2. **승격(대기 → 예약)**
    - **빈 슬롯 계산**: `free = capacity - ZCARD(z:working)`.
    - **승격**: `ZPOPMIN z:waiting`으로 가장 앞 토큰 pop → `ZADD z:working (score=admittedAtMillis, member=token)` → `h:ticket:{token}.state=working`.


3. **완료 처리(결제 커밋 후 out)**

- `ZREM z:working token`, `h:ticket:{token}.state='done'`.
- `HDEL h:active_token userId`로 사용자→토큰 매핑 해제(재진입 가능).

---

## 2) 각 프로세스에서의 Redis 자료구조·개념

### A. 티켓 발급(중복검사 → 대기열 등록)

- **`h:{queue}:active_token` (HASH) – 중복 발급 방지**
    - *역할*: “사용자 → 현재 활성 토큰” 유일 매핑.
    - *연산*: `HGET`으로 기존 토큰 확인 → 없으면 `HSETNX userId -> token` (원자 점유).
- **`seq:{queue}:waiting:{ms}` (STRING, per-ms INCR) – 동점 해소용 시퀀스**
    - *역할*: 같은 밀리초(ms)에 몰린 요청의 **순서를 안정적으로 분리**.
    - *연산*: `INCR seq:{queue}:waiting:{ms}` → 즉시 `EXPIRE ...`(예: 5s)로 키 폭증 방지.
    - *이유*: 점수(score) 계산 시 `score = ms * FACTOR + seq`로 **완전한 순서**(FIFO + 타이브레이크) 보장.
    - *특징*: `INCR`은 **원자적**이고 Redis는 단일 스레드라 **동시성 안전**.
- **`z:{queue}:waiting` (ZSET) – 전역 대기열**
    - *역할*: 대기 인원 정렬/랭킹.
    - *연산*: `ZADD (score=ms*FACTOR+seq, member=token)`, `ZRANK token` (+1)로 순번.
    - *이점*: `ZPOPMIN`으로 **맨 앞**을 원자적으로 꺼낼 수 있어 승격에 유리.
- **`h:{queue}:ticket:{token}` (HASH) – 티켓 메타**
    - *필드*: `userId`, `state=waiting`, `issuedAt`
    - *용도*: 상태 점검/감사 로그

### B. 승격(대기 → 예약)

- **`z:working` (ZSET)**

### C. 완료 처리(결제 커밋 후 out)

- **핵심 키 조작**:
    - `ZREM z:working token` (임계영역 종료)
    - `HSET h:ticket:{token} state='done'`
    - `HDEL h:active_token userId` (재발급 허용)