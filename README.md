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
- 결제 승인 (금액 3중 검증: 클라이언트 요청 vs Redis vs Toss 응답)
- 환불 처리
- 결제 데이터 RDBMS 저장 (원본 JSON 응답 포함)

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
└── N:1  Member (결제자)

PhoneVerification (휴대폰 인증) - 독립
```

Redis:
```
refresh:{memberCode}     → refreshToken (TTL 7일)
payment:order:{orderId}  → orderId + amount (TTL 15분)
oauth:signup:{token}     → providerType + providerUserId (TTL 10분)
property:land:{pnu}        → 토지 정보 (TTL 7일)
property:land-ratio:{pnu}  → 대지권 비율 (TTL 7일)
property:building:{key}    → 건축물 동/호 정보 (TTL 7일)
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
