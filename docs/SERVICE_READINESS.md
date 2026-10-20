# BooE 서비스화 준비 점검 (2026-10-20 기준)

현재 코드(`main` + 미커밋 변경분)와 지금까지의 작업 기록을 대조해, **실제 사용자에게 오픈하기 위해 부족한 작업**을 우선순위별로 정리한 문서입니다.

- 기준: `./gradlew compileJava compileTestJava` 통과 확인. 테스트 실행(MySQL·Redis 필요)은 이번 점검에서 돌리지 않음
- 표기: `파일:라인`은 점검 시점 기준

---

## 0. 결론 — 최소 출시선(MVP)

> **P0 전부 + P1 중 "알람"까지**가 최소 출시선입니다.

이유는 세 가지입니다.

1. **핵심 가치가 아직 끝까지 이어지지 않습니다.** 사용자가 돈을 내고 받는 결과물은 "서명된 계약서"입니다. 그런데 모두사인 연동은 클라이언트와 DTO만 있고 호출하는 곳이 없습니다. PDF도 서버 로컬 디스크에 저장만 되고 내려받을 API가 없습니다.
2. **운영 환경에서 사고가 날 수 있는 코드가 있습니다.** 부하 테스트 시드 러너가 프로파일 가드 없이 `@Component`로 등록돼 있습니다. 그래서 운영 DB에 가짜 회원 1만 명과 비밀번호가 알려진 계정이 생깁니다. 또 계약 상태 전이에 가드가 없어 결제 후 상태를 되돌릴 수 있습니다.
3. **외부 계약과 법적 요건이 필요합니다.** 토스 라이브 키, SMS 또는 NICE 본인인증, 모두사인 운영 키는 모두 사업자등록과 심사가 선행돼야 합니다. 개인정보처리방침과 약관 동의 저장도 없습니다.

P0만 끝나면 "결제 → PDF → 서명 → 다운로드"가 실제로 동작합니다. 이상 상황이 생겨도 데이터가 보존되고, 운영자가 인지할 수 있는 상태가 됩니다. 성능이나 확장성 작업(P2)은 출시 후 실제 트래픽을 보고 해도 늦지 않습니다.

---

## 1. 현재 구현 현황 요약

| 영역 | 상태 | 비고 |
|---|---|---|
| 로컬 가입/로그인, JWT(Access/Refresh+Redis) | ✅ | 휴대폰 인증은 **SMS 발송 시뮬레이션(로그 출력)** |
| 카카오 OAuth 2단계 가입 | ✅ (확인 필요) | `/api/auth/oauth/signup`이 permitAll에 없음 (P0-6) |
| 본인인증(NICE) / CI 중복 차단 | ⏸ 보류 | 엔티티·암호화(`CiCipher`, `CiHasher`)만 있음 |
| 공공데이터 조회(건축물대장·토지·대지권) | ✅ | 병렬 페이지네이션, Redis 캐시(TTL 7일), single-flight |
| 동/호 리스트 → 단건 면적 조회 전환 | ✅ (미커밋) | `POST /contracts/unit-area`. Redis building 캐시 플러시 필요 |
| 계약서 임시저장/확인요청/임차인 입력/계약금 확인 | ✅ | 상태 전이 가드 없음 (P0-3) |
| 결제(토스) 승인·환불, 멱등성, PENDING 선기록, 보상 cancel | ✅ | 테스트 키 기준 |
| 결제 Reconciliation 스케줄러 | ✅ | 5분 주기·5분 임계. 단일 인스턴스 전제 |
| PDF 생성 비동기 워커(N=4, Q=100) | ✅ (미커밋) | 로컬 파일 저장, 재시도 없음, 다운로드 API 없음 |
| 모두사인 전자서명 | ❌ | `ModusignApiClient`가 어디서도 호출되지 않음 |
| FCM 알림 | ✅ | 일부는 트랜잭션 안에서 동기 발송 |
| 임대료 납부 스케줄(`ContractPaymentSchedule`) | 🟡 | 엔티티·시드만 있음, 생성/알림 로직 없음 |

---

## 2. 우선순위별 부족 작업

### 🔴 P0 — 출시 차단 (이게 없으면 오픈 불가)

#### P0-1. 부하테스트 시드 러너의 운영 실행 차단
- **문제:** `loadtest/LoadTestSeedRunner.java:18`이 `@Component` + `CommandLineRunner`이고 `@Profile`이 없습니다. 운영 부팅 시 회원 1만 명, 계약 1.5만 건, 스케줄 19.5만 건이 들어갑니다. 비밀번호가 코드에 박힌 `loadtest_user` 계정도 생깁니다.
- **조치:** `@Profile("loadtest")`를 붙이거나 테스트 소스셋으로 이동합니다. `property/ApiTestRunner.java`도 주석 상태지만 함께 정리합니다.

#### P0-2. 운영 설정 분리 + 스키마 마이그레이션 도구
- **문제:**
  - `application.yml`에 `ddl-auto: update`, `show_sql: true`, `generate_statistics: true`, DB `root` 계정이 설정돼 있습니다.
  - 시크릿(JWT·CI 암호화 키·토스·모두사인·공공데이터 키)이 로컬 yml 하나에 들어 있습니다. gitignore 처리는 돼 있습니다.
  - `ddl-auto=update`가 enum 컬럼 변경을 반영하지 못해 결제 INSERT가 깨진 전례가 있습니다(`payment.status` 드리프트).
- **조치:**
  - `application-prod.yml`을 분리하고 시크릿은 환경변수나 Secret Manager로 옮깁니다.
  - `ddl-auto: validate`로 바꾸고 SQL 로그를 끕니다.
  - **Flyway를 도입**하고 현재 스키마를 V1 baseline으로 잡습니다.
  - 앱 전용 DB 계정(최소 권한)을 만듭니다.

#### P0-3. 계약 상태 전이 가드
- **문제:** `Contract.java:115-145`의 `requestReview`, `submitByLessee`, `confirmByLessor`, `completePayment`, `cancelPayment`, `requestSign`이 **현재 상태를 검사하지 않고** 상태를 덮어씁니다. `PaymentService.createOrder`와 `confirmPayment`도 계약이 `PAYMENT_PENDING`인지 확인하지 않습니다.
  - 예 1: 결제 완료 계약에 `/review-request`를 다시 호출하면 `REVIEW_REQUESTED`로 회귀합니다.
  - 예 2: 임차인이 계약금 확인 전 단계에서 결제할 수 있습니다.
- **조치:** 각 전이 메서드에 `from` 상태 검증을 넣고, 위반 시 `InvalidContractStatusException`(409)을 던집니다. `save()`(임시저장 수정)도 `DRAFT`일 때만 허용합니다.

#### P0-4. 전자서명(모두사인) 연동 — 핵심 플로우 완결
- **문제:** `SIGN_REQUESTED`와 `SIGNED` 상태, `modusignDocumentId` 컬럼은 있지만 서명 요청 호출과 완료 수신 경로가 없습니다. 서명되지 않은 PDF만으로는 서비스 가치가 성립하지 않습니다.
- **조치:**
  - PDF 생성 완료 후 모두사인 문서 생성과 서명 요청을 보내고 `SIGN_REQUESTED`로 전이합니다.
  - 모두사인 **webhook** 수신 엔드포인트를 만듭니다(서명 검증 + 멱등 처리). 완료되면 서명본 PDF를 받아 저장하고 `SIGNED`로 전이합니다.
  - 서명 거절·만료 시 처리 정책(환불 연계 여부)을 정해야 합니다.
- **결정 필요:** MVP에서 서명을 빼고 "PDF 발급까지"만 할지. 이 경우 상품 정의 자체가 달라집니다.

#### P0-5. PDF 저장·제공·재생성
- **문제:**
  - `ContractPdfStorageService`가 로컬 디스크(`./pdf-storage`)에 저장합니다. 컨테이너 재배포나 다중 인스턴스 환경에서는 파일이 사라집니다.
  - `confirmPayment`가 **서버 내부 파일 경로**를 클라이언트에 반환합니다. 다운로드 API도 없습니다.
  - 워커 실패(`PdfGenerationWorker.process`의 catch)나 큐 거절(`RejectedExecutionException`)은 로그만 남깁니다. 그러면 결제는 됐는데 계약서가 없는 상태가 영구히 남습니다.
- **조치:**
  - NCP Object Storage나 S3로 전환합니다.
  - `GET /contracts/{id}/pdf`를 만듭니다. 당사자 권한을 확인한 뒤 presigned URL이나 스트림으로 내려줍니다.
  - 계약에 PDF 상태(`PDF_PENDING`, `PDF_READY`, `PDF_FAILED`)를 기록합니다. Reconciliation 스케줄러가 "결제 완료 + PDF 없음"을 감지해 재생성합니다(재시도 N회 후 알람).
  - 응답은 경로 대신 `contractId`와 상태를 주고, 완료는 FCM으로 알립니다.
- **참고:** PDF 안에 개인정보(이름·주소·연락처·생년월일)가 들어가므로 버킷은 반드시 private이어야 합니다.

#### P0-6. 인증 엔드포인트 보안 설정 점검
- **문제:** `SecurityConfig.java`의 permitAll에 `/api/auth/oauth/signup`이 없습니다. 신규 소셜 가입자는 JWT가 없는 상태라 막힐 가능성이 높습니다(실기기 확인 필요).
- **조치:**
  - permitAll 목록을 정리합니다.
  - 미인증 요청이 401로 응답되도록 `AuthenticationEntryPoint`를 설정합니다.
  - 디버그용 `GET /contracts/contract`와 `/units`의 `...WithTiming`을 제거합니다.

#### P0-7. 휴대폰/본인인증 실제 연동
- **문제:** `PhoneVerificationService.sendCode`가 SMS를 보내지 않고 **인증코드를 로그에 출력**합니다. `Math.random()`을 쓰고 시도 횟수 제한도 없습니다. 지금 상태로는 실제 가입이 불가능하고, 로그에 인증코드가 남습니다.
- **조치(둘 중 하나):**
  - (A) NICE 본인인증 통합. 기존에 합의한 "본인인증 = 가입 게이트" 정책에 맞는 정공법입니다(작업 분해 ⑤-a~f가 이미 정리돼 있음).
  - (B) 사업자 심사 전 임시로 SMS 발송사(NCP SENS, 알리고 등)만 연동합니다.
- **공통 조치:** `SecureRandom` 사용, 코드 로그 제거, 번호별·IP별 발송 횟수 제한(Redis), 검증 실패 5회 시 잠금.

#### P0-8. 결제 운영 전환
- **문제와 조치:**
  - 토스 라이브 키는 사업자등록과 PG 심사가 필요합니다.
  - **환불 정책이 코드에 없습니다.** `refundPayment`는 계약 상태와 무관하게 환불하고 `PAYMENT_PENDING`으로 되돌립니다. 서명 이후 환불도 허용됩니다. 언제까지 환불 가능한지, 환불 시 PDF·서명을 어떻게 무효화할지 정책을 정하고 코드에 반영해야 합니다.
  - `refundPayment`는 `@Transactional` 안에서 토스 cancel을 호출하고 동시 요청 가드도 없습니다. 최소한 상태 체크를 강화하고 row lock(`SELECT ... FOR UPDATE`)을 걸어야 합니다(기존 후속 과제 [E]).

#### P0-9. 법적·개인정보 요건
- 이용약관과 개인정보처리방침을 작성하고, 동의 이력을 저장합니다. `MemberTermsAgreement`는 설계만 돼 있고 미구현입니다.
- 계약서에 들어가는 개인정보(주민번호 앞자리=생년월일, 주소, 연락처)의 수집 근거와 보관 기간을 정하고, 파기 배치를 만듭니다.
- 통신판매업 신고와 전자상거래 고지(사업자 정보, 환불 규정)를 앱에 노출합니다.

#### P0-10. 배포·운영 기본기
- Dockerfile(또는 jar 배포 스크립트), HTTPS(리버스 프록시), MySQL·Redis 운영 인스턴스와 **DB 자동 백업**을 준비합니다.
- 헬스체크(`spring-boot-starter-actuator`의 `/actuator/health`만 노출)를 둡니다.
- `firebase-service-account.json`은 classpath 대신 외부 경로나 Secret으로 주입합니다.

---

### 🟠 P1 — 출시 직후 1~2개월 내 (안정 운영)

| # | 작업 | 이유 |
|---|---|---|
| P1-1 | **에러 알람(Sentry/Slack webhook)** ← *최소 출시선에 포함* | 보상 cancel 실패, PDF 실패, 스케줄러 ABANDONED가 지금은 `log.error`로만 남음. 아무도 모르는 사이 돈이 걸린 비일관 상태가 쌓임 |
| P1-2 | Reconciliation `attempts` 관리 | PENDING 무한 재시도 방지 (N회 후 ABANDONED + 알람) |
| P1-3 | ContractService의 FCM을 커밋 후 비동기로 | `requestReview`, `submitLesseeInfo`, `confirmDownPayment`가 트랜잭션 안에서 FCM을 동기 호출. FCM이 지연되면 커넥션이 점유됨. `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` |
| P1-4 | 외부 API timeout 정리 | 공공데이터 `timeout: 50000`(50초)은 과도함. 토스는 5초로 적정. 공공데이터는 페이지별 timeout + 전체 deadline |
| P1-5 | Refresh Token Rotation + 다중 기기 키 | 지금은 `refresh:{memberCode}` 하나라서 다른 기기에서 로그인하면 이전 기기가 강제 로그아웃됨. 모바일+태블릿 사용자 불편 |
| P1-6 | 로그아웃 accessToken 블랙리스트 | 로그아웃 후에도 만료(1h) 전까지 API 호출 가능 |
| P1-7 | 인덱스 추가 | `contract(member_id)`, `contract(lessee_member_id)`, `payment(contract_id, status)`, `member_device(member_id)`, `member_profile(phone_number)` — Flyway 마이그레이션으로 |
| P1-8 | 테스트 정비 + CI | 픽스처 정리, `./gradlew test`를 GitHub Actions에서 실행(Testcontainers로 MySQL·Redis) |
| P1-9 | API 문서(springdoc-openapi) | Flutter와의 계약. 이번에 `/units` 응답에서 면적이 빠지는 등 계약 변경이 있었음 |
| P1-10 | `requestReview` 회원 미존재 처리 | `ContractService.java:71` TODO. 임차인이 미가입이면 초대 문자/링크 흐름이 필요(제품 결정) |
| P1-11 | 공공데이터 캐시 플러시/버저닝 | 동/호 응답 구조 변경이 배포될 때 옛 캐시(TTL 7일)와 충돌. 캐시 키에 버전 prefix |

---

### 🟡 P2 — 트래픽·규모가 생긴 뒤

| # | 작업 | 트리거 조건 |
|---|---|---|
| P2-1 | ShedLock(스케줄러 분산 락) | 인스턴스 2대 이상 |
| P2-2 | 토스 Webhook 통합 | 결제량 증가. 스케줄러보다 즉시성이 좋음 |
| P2-3 | PDF 폰트 파싱 결과 캐시(PDFBox 직접 사용) | JFR 기준 CPU의 약 80%가 폰트 파싱. PDF 처리량이 병목일 때 |
| P2-4 | PDF 워커를 Redis Stream이나 큐로 | 다중 인스턴스 + 잡 유실 0이 요구될 때 (`@Async`는 프로세스가 죽으면 큐가 유실됨) |
| P2-5 | 토스 API rate limit | 결제 폭증 시 |
| P2-6 | k6/Gatling 처리량(RPS/p99) 측정 | 운영 사양이 확정된 뒤 PDF 풀 N 재산출 |
| P2-7 | 도메인 확장: 임대료 납부 알림, 임대인 수익 통계 | 리텐션 기능. 납부 스케줄 엔티티는 이미 있음 |

---

## 3. 체크리스트 (최소 출시선)

```
[ ] P0-1  LoadTestSeedRunner 프로파일 가드
[ ] P0-2  prod 프로파일 + 시크릿 외부화 + Flyway baseline + ddl-auto=validate
[ ] P0-3  계약 상태 전이 가드 (+ 결제 진입 상태 검증)
[ ] P0-4  모두사인 서명 요청 + webhook → SIGNED   (또는 "서명 제외 MVP" 결정)
[ ] P0-5  PDF 오브젝트 스토리지 + 다운로드 API + 실패 재생성
[ ] P0-6  SecurityConfig permitAll 정리 / 디버그 엔드포인트 제거
[ ] P0-7  SMS 또는 NICE 실연동 + 인증 남용 방지
[ ] P0-8  토스 라이브 전환 + 환불 정책·락
[ ] P0-9  약관/개인정보처리방침/동의 이력/파기 정책
[ ] P0-10 배포·HTTPS·백업·헬스체크
[ ] P1-1  에러 알람
```

**외부 의존 리드타임 주의:** P0-4, P0-7, P0-8은 모두 **사업자등록 → 각 사 심사**가 선행돼야 하고 영업일 기준 수일에서 수주가 걸립니다. 코드 작업과 병행해 지금 신청을 시작하는 게 전체 일정에서 가장 중요합니다.
