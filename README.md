# 🏠 부동산 이곳에서 (BooE) - Backend

부동산 정보를 쉽고 빠르게 조회할 수 있는 **부동산 이곳에서 (BooE)** 프로젝트의 백엔드 서버입니다.

## 🛠 Tech Stack & Core Libraries
- **Backend**: Spring Boot 3.x, Java 17
- **Security**: Spring Security & JWT (Json Web Token)
- **Database**: MySQL, Spring Data JPA
- **Communication**: REST API (for Flutter Frontend)

## 🚀 Key Implementation Details

### 1. Authentication & Security
- **JWT 기반 인증 시스템**: Stateless한 서버 환경을 위해 JWT를 도입하여 로그인 및 권한 관리를 수행합니다.
- **Spring Security**: 보안 필터 체인을 통해 리소스 접근 권한을 제어하며, 인증 로직을 커스텀하여 구현했습니다.

### 2. Domain Modeling & Persistence
- **JPA (Java Persistence API)**: 객체 지향적인 데이터 모델링을 위해 JPA를 사용하며, 복잡한 부동산 데이터를 효율적으로 매핑했습니다.
- **부동산 데이터 구조화**:
  - **토지**: 지목, 대지권 비율, 면적
  - **건물**: 구조, 용도, 전용 면적 (표제부 API 연동)

### 3. Advanced Exception Handling
- **Global Exception Strategy**: 모든 예외를 중앙 집중식으로 관리합니다.
- **Custom Error Codes**: 별도의 `Enum` 타입을 활용하여 에러 코드를 정의하고, 비즈니스 예외(예: `already_registered_member`)를 처리하기 위한 전용 예외 클래스를 운영합니다.
