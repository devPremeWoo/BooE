# BooE (부동산 이곳에서) - Backend

부동산 임대차 계약서 작성부터 결제, PDF 생성까지 모바일에서 처리할 수 있는 서비스의 백엔드 서버입니다.

## Tech Stack

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.x, Java 17 |
| Security | Spring Security, JWT (Access + Refresh Token) |
| Database | MySQL, Spring Data JPA |
| Cache | Redis (토큰 관리, 결제 주문 임시 저장) |
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
- 외부 API 통신 (WebClient)

### 6. 알림 (FCM)
- 계약 단계별 푸시 알림 (정보입력 요청, 결제 완료, 환불 등)
- 디바이스 토큰 관리

### 7. 회원
- 로컬 회원가입/로그인
- 휴대폰 번호 인증
- 프로필 조회/수정
- 회원 탈퇴 (소프트 삭제 + Redis 토큰 삭제)

## ERD (엔티티 연관관계)

```
Member (회원)
├── 1:1  MemberCredential        (로그인 정보)
├── 1:1  MemberProfile           (프로필)
├── 1:N  MemberDevice            (FCM 디바이스 토큰)
├── 1:N  userOauthConnection     (OAuth 연동)
│         └── N:1  OauthProvider
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
```

## 패키지 구조

```
src/main/java/org/hyeong/booe/
├── auth/              # OAuth 관련 (예정)
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
| POST | /api/auth/signup | 회원가입 |
| POST | /api/auth/login | 로그인 |
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
