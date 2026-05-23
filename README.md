# BooE (부동산 이곳에서) - Backend

부동산 임대차 계약서 작성부터 결제, PDF 생성까지 모바일에서 처리할 수 있는 서비스의 백엔드 서버입니다.

## Tech Stack

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.x, Java 17 |
| Security | Spring Security, JWT (Access + Refresh Token) |
| Database | MySQL, Spring Data JPA |
| Cache | Redis (토큰 관리, 결제 주문 임시 저장, 공공데이터 응답 캐싱) |
| Payment | TossPayments API (WebClient) |
| PDF | Thymeleaf + OpenHTMLtoPDF |
| Push | Firebase Cloud Messaging (FCM) |
| External API | 공공데이터 포털 (건축물대장, 토지정보) |
| Client | Flutter (REST API 통신) |

## 핵심 기능

### 1. 인증/인가
- JWT 기반 Stateless 인증 (AccessToken 1h / RefreshToken 7d)
- RefreshToken Redis 저장 및 검증
- 토큰 갱신 API (`POST /api/auth/refresh`)
- 로그아웃 시 Redis 토큰 삭제
- Spring Security 필터 체인 기반 권한 제어
- 로컬 + 카카오 OAuth 2종 로그인 지원

### 2. 부동산 계약서
- 임대차 계약서 생성/조회/삭제
- 5단계 정보 입력 플로우 (물건정보 → 계약조건 → 특약사항 → 개인정보 → 서명)
- 임대인/임차인 양방향 계약 참여
- 소프트 삭제 (공유 계약서 당사자별 삭제 처리)
- 계약서 상태 관리 (DRAFT → REVIEW_REQUESTED → PAYMENT_PENDING → PAYMENT_DONE → SIGNED)

### 3. 결제 (TossPayments)
- 결제 주문 생성 → Redis 임시 저장 (15분 TTL)
- 결제 승인 — 토스 confirm 호출 시 **서버 측 Redis 기억값**(orderId·amount)을 사용해 클라이언트 변조 차단
- 결제 데이터 RDBMS 저장 (원본 JSON 응답 포함)
- 환불·취소 처리 (CANCELED 상태로 통합)
- 결제 상태 전이 이력은 별도 `PaymentEvent` 테이블에 시계열 기록
- PENDING 자동 정리 스케줄러로 orphan 결제 자동 복구

### 4. PDF 계약서 생성
- Thymeleaf 템플릿 기반 HTML 렌더링
- OpenHTMLtoPDF로 PDF 변환
- 로컬 저장 (NCP Object Storage 전환 예정)

### 5. 부동산 공공데이터 조회
- 건축물대장 표제부/전유부 정보 조회
- 토지 정보 및 대지권 비율 조회
- 외부 API 통신 (WebClient, 비동기 논블로킹)
- Redis 캐싱 (PNU 단위, TTL 7일)
- 동일 PNU 동시 요청에 대한 single-flight (in-flight Mono 공유로 외부 API 중복 호출 방지)

### 6. 알림 (FCM)
- 계약 단계별 푸시 알림 (정보입력 요청, 결제 완료, 환불 등)
- 디바이스 토큰 관리

### 7. 회원
- 로컬 회원가입/로그인 (id/pw)
- 카카오 OAuth 가입/로그인 (2단계: 카카오 토큰 검증 → 가입 정보 입력)
  - 신규 사용자는 임시 가입 토큰(Redis TTL 10분) 발급 후 추가 정보 입력으로 가입 완료
  - 기존 사용자는 즉시 JWT 발급
- 휴대폰 번호 인증 (NICE 본인인증 통합 예정)
- 프로필 조회/수정
- 회원 탈퇴 (소프트 삭제 + Redis 토큰 삭제)

## 성능 개선 / 문제해결

### 1. 계약서 목록 조회 N+1 해결
- 문제: `Contract`-`ContractPaymentSchedule` 1:N 관계로, 목록 조회 시 계약 N건마다 스케줄 조회 쿼리가 추가 발생
- 해결: `LEFT JOIN FETCH c.paymentSchedules`로 단일 쿼리 처리 (`ContractRepository.findAllBy*WithSchedules`)

### 2. 외부 공공데이터 API 응답 캐싱
- 문제: 같은 PNU에 대한 토지/건축물/대지권 정보 조회 시마다 외부 API 호출 (응답 지연 + 호출량 부담)
- 해결: PNU 단위 Redis 캐싱 (TTL 7일)

### 3. 동일 PNU 동시 요청 cache stampede 방지
- 문제: 캐시 미스 상태에서 같은 PNU로 N개 동시 요청 시 외부 API가 N번 중복 호출됨
- 해결: `InFlightRequestRegistry`에서 `ConcurrentHashMap` + `computeIfAbsent`로 진행 중인 `Mono`를 원자적으로 공유 (single-flight) → 외부 API 호출 1회로 축소, 나머지 요청은 같은 결과 구독

### 4. `.block()`으로 인한 워커 스레드 점유 해소
- 문제: 부동산 정보 조회 컨트롤러가 `Mono.block()`으로 외부 API 응답까지 톰캣 워커 스레드 점유 → 동시성 한계
- 해결: 컨트롤러 반환 타입을 `Mono`로 변경해 Servlet async 위임. ASYNC dispatch에서 `SecurityContext`가 비어 권한 체크가 실패할 가능성을 차단하기 위해 `JwtAuthenticationFilter.shouldNotFilterAsyncDispatch()`를 `false`로 오버라이드하여 dispatch 재진입 시 토큰을 다시 검증

## 결제 무결성 / 운영 보강

### 1. 클라이언트 변조 차단 — 토스 confirm에 서버 기억값 전달
- 문제: 토스 confirm API에 클라이언트가 보낸 `amount`·`orderId`를 그대로 전달하면, 변조된 가격으로 결제창을 띄운 케이스를 우리 서버에서 검증할 수단이 없음 (토스는 "클라가 띄운 가격대로 청구"할 뿐, 그게 우리 서버 의도와 같은지 모름)
- 해결: `createOrder` 시 Redis에 `{orderId, amount}` 저장 → confirm 시 클라 값과 Redis 값을 비교 + **토스에는 Redis 기억값으로 호출**. 클라 변조가 토스 측 검증과 우리 측 검증 양쪽에서 차단됨
- 부수효과: `validateAmount` 등 1차 검증이 dead code화되어 제거 → 코드 단순화 + 가드 견고도 동일/향상

### 2. 중복 결제 방지 — Idempotency Key + DB UNIQUE 이중 안전망
- 문제: 동시 요청·재시도·중복 클릭으로 같은 `paymentKey`에 대한 confirm이 두 번 호출될 수 있음
- 해결:
  - Redis `SETNX` + TTL(60s)로 `paymentKey`별 락 (일반 동시 요청 차단)
  - `Payment.payment_key`에 UNIQUE 제약 (`uk_payment_key`) (락 만료·크래시·Redis 장애 대비)
  - 신규 예외 `DuplicatePaymentConfirmException` + `ErrorCode.PAYMENT_DUPLICATE_CONFIRM (P104, CONFLICT)`

### 3. 선기록(PENDING) 패턴 + 트랜잭션 분리
- 문제: confirmPayment 전체가 한 트랜잭션으로 묶여 있으면 토스 호출(외부 IO) 시간 동안 DB 커넥션 점유 + 토스 호출 후 우리 측 실패 시 인서트도 같이 롤백되어 자동 보상의 단서 자체가 사라짐
- 해결:
  - `confirmPayment`의 클래스 레벨 `@Transactional` 제거 (오케스트레이션)
  - `PaymentRecordService`를 별도 빈으로 분리하여 짧은 트랜잭션 단위로 호출 (`REQUIRED` 기본값 사용, REQUIRES_NEW는 데드락 우려로 미사용)
  - 트랜잭션 지도: `[TX1: PENDING insert] → 토스 호출(밖) → [TX2: APPROVE + contract 상태 + redis 정리]`
- 효과: 외부 IO 동안 DB 커넥션 미점유, 후속 실패 시 PENDING 레코드가 살아남아 보상/복구 가능

### 4. Orphan 결제 자동 보상 cancel
- 문제: 토스 confirm 호출은 성공(실제 청구 발생)했는데 우리 측 후처리(`validateConfirmedOrder`·`approve` DB 저장)가 실패하면, 토스에 청구만 살아있고 우리 DB는 PENDING으로 남는 비일관 상태
- 해결: `confirmWithCompensation` try/catch + `compensateOrphan`에서 토스 cancel 자동 호출 + `Payment.CANCELED` 기록
- 한계와 보완: 보상 cancel 자체 실패 또는 서버 크래시 사이 케이스는 try/catch로 못 잡음 → `PaymentRecordService.markCancelFailed`로 `Payment.FAILED + CANCEL_FAILED 이벤트` 기록 + ERROR 로그, 그래도 못 잡힌 케이스는 ⑥ 스케줄러로 회수

### 5. 상태 전이 이력 분리 — `PaymentEvent` 테이블
- 문제: Payment 본체에 상태별 시각·사유·actor·payload를 다 컬럼화하면 NULL 필드와 변동성 큰 enum이 늘어나며 본체가 무거워짐. 운영 검토 시 "이 결제가 어떻게 흘러왔는지" 추적도 어려움
- 해결: 본체는 현재 상태만, 상태 전이·실패·보상·정정 액션은 모두 `payment_event`에 시계열로 INSERT
  - 컬럼: `payment_id`, `event_type`, `actor`, `reason`, `payload(JSON)`, `created_at`
  - 이벤트 유형: `CREATED`, `APPROVED`, `CANCEL_SUCCEEDED`, `CANCEL_FAILED`, `RECONCILED`, `ABANDONED`, `AMOUNT_MISMATCH`
  - actor: `SYSTEM`, `USER`, `ADMIN`, `SCHEDULER`
- 효과: 본체 단순 유지 + 운영 추적성·감사성 확보. 한 트랜잭션 안의 추가 INSERT 1건은 외부 IO 대비 무시 가능한 비용
- 도메인 단순화 결정: 단일 환불 흐름이라 `REFUNDED` 상태는 폐기 → 환불·시스템 취소·스케줄러 정리 모두 `CANCELED`로 통합, `canceled_at` 컬럼만 별도 기록

### 6. PENDING 자동 정리 스케줄러 (Reconciliation)
- 문제: ④의 보상 cancel 자체가 실패하거나 토스 호출 후 ~ DB 커밋 전 사이에 서버가 죽으면 `Payment.PENDING`이 영구히 남고 try/catch로는 회복 불가
- 해결: `@Scheduled`로 일정 주기마다 `WHERE status = PENDING AND created_at < NOW() - 5min` 조회 → 각 건마다 토스 조회 API(`GET /v1/payments/{paymentKey}`)로 진실 상태 확인 후 분기
  - `DONE` + orderId·amount 일치 → `reconcileToApprove` (APPROVE 복구, `RECONCILED` 이벤트)
  - `CANCELED`/`ABORTED`/`EXPIRED` → `reconcileToCancel` (CANCELED 마킹, `RECONCILED` 이벤트)
  - `DONE` + 검증 불일치 → `markAbandoned` (FAILED + `ABANDONED` 이벤트)
  - `IN_PROGRESS`/`WAITING_FOR_DEPOSIT`/`READY` 또는 토스 조회 실패(null) → skip, 다음 회차 재시도
- 격리: 한 건 처리 실패가 batch 전체 중단으로 이어지지 않도록 건별 try/catch + `TossPaymentApiClient.getPaymentStatus`는 throw 대신 null 반환
- 운영 의식: 모놀리식 환경에서 스케줄 잡이 사용자 트래픽에 영향을 주지 않도록 트랜잭션 짧게 유지(③) + `fixedDelay`로 동시 실행 회피. 다중 인스턴스 환경 진입 시 ShedLock 도입 예정

## ERD (엔티티 연관관계)

```
Member (회원)
├── 1:1  MemberCredential        (로컬 로그인 정보, 소셜 가입 시 미생성)
├── 1:1  MemberProfile           (프로필)
├── 1:N  MemberDevice            (FCM 디바이스 토큰)
├── 1:N  MemberOauthConnection   (OAuth 연동, providerType + providerUserId)
├── 1:N  Contract (as 임대인)
├── 1:N  Contract (as 임차인)
└── 1:N  Payment

Contract (계약서)
├── N:1  Member (임대인)
├── N:1  Member (임차인)
├── 1:1  ContractFormData        (계약서 입력 데이터 JSON)
├── 1:N  ContractParty           (계약 당사자 정보)
├── 1:N  ContractPaymentSchedule (납부 스케줄)
└── 1:N  Payment

Payment (결제)
├── N:1  Contract
├── N:1  Member (결제자)
└── 1:N  PaymentEvent           (상태 전이·보상·정정 이력)

PaymentEvent (결제 이력)
└── N:1  Payment

PhoneVerification (휴대폰 인증) - 독립
```

Redis:
```
refresh:{memberCode}        → refreshToken (TTL 7일)
payment:order:{contractId}  → orderId + amount (TTL 15분, 서버 진실값)
payment:confirm:{paymentKey} → idempotency 락 (SETNX, TTL 60초)
oauth:signup:{token}        → providerType + providerUserId (TTL 10분)
property:land:{pnu}         → 토지 정보 (TTL 7일)
property:land-ratio:{pnu}   → 대지권 비율 (TTL 7일)
property:building:{key}     → 건축물 동/호 정보 (TTL 7일)
```

## 패키지 구조

```
src/main/java/org/hyeong/booe/
├── auth/              # 인증 (로컬, OAuth)
│   ├── local/         # 로컬 회원가입/로그인
│   └── oauth/         # 카카오 OAuth (client, service, dto)
├── common/            # 공통 응답, 코드
├── contract/          # 계약서 도메인
├── exception/         # 글로벌 예외 처리
├── global/            # Security, JWT, Config
├── member/            # 회원 도메인
├── payment/           # 결제 도메인
├── property/          # 부동산 정보 조회
└── verification/      # 휴대폰 인증
```

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | /api/auth/signup | 로컬 회원가입 |
| POST | /api/auth/login | 로컬 로그인 |
| POST | /api/auth/kakao | 카카오 토큰 검증 (기존 회원→JWT, 신규→signupToken) |
| POST | /api/auth/oauth/signup | 소셜 가입 완료 (signupToken + 사용자 입력) |
| POST | /api/auth/refresh | 토큰 갱신 |
| POST | /api/auth/logout | 로그아웃 |
| GET | /api/members/me | 내 정보 조회 |
| PATCH | /api/members/me | 내 정보 수정 |
| DELETE | /api/members/me | 회원 탈퇴 |
| GET | /api/contracts | 계약서 목록 |
| DELETE | /api/contracts/{id} | 계약서 삭제 |
| POST | /payments/order | 결제 주문 생성 |
| POST | /payments/confirm | 결제 승인 |
| POST | /payments/refund | 환불 |
