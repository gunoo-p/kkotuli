# 기술 스택 및 선택 이유

### 프론트엔드

| 기술 | 선택 이유 |
|------|-----------|
| **React 19.2.6** | 컴포넌트 기반 UI 구성 |
| **Vite 8.0.12** | 빠른 개발 환경 및 번들링 |

### 백엔드

| 기술 | 선택 이유 |
|------|-----------|
| **Kotlin 2.0.21** | `data class`로 DTO 보일러플레이트 제거, Null Safety로 런타임 NPE 방지. Java 대비 코드량 약 30~40% 감소 |
| **Spring Boot 3.3.5** | REST API · WebSocket · 의존성 주입을 하나의 프레임워크에서 일관되게 처리 |
| **Spring WebSocket + STOMP** | 방마다 독립 채널(`/topic/room/{roomCode}`)로 브로드캐스트 범위를 방 단위로 격리 |
| **Java 21 가상 쓰레드** | WebSocket 동시 연결 · Redis 대기 · 외부 API 대기 구간에서 OS 쓰레드 자동 반납. `application.yml` 설정 한 줄로 적용 |

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 데이터베이스

| 기술 | 선택 이유 |
|------|-----------|
| **Redis 7-alpine** | 인메모리 구조로 단어 검증 · 턴 상태를 밀리초 단위 처리. TTL로 게임 종료 후 자동 소멸. MySQL 불필요 |
| **Spring Data Redis** | `RedisTemplate`으로 Hash · Set · List · TTL 등 자료구조를 타입 안전하게 사용 |

### 외부 API

| 기술 | 선택 이유 |
|------|-----------|
| **국립국어원 표준국어대사전 Open API** | 공식 표준어 기준으로 단어 검증 외부 위임. 직접 사전 DB 구축 대비 유지보수 비용 없음 |
| **Redis 캐싱 (word:cache)** | 사전 API 응답 지연(50~200ms) 방지. 검증된 단어를 24시간 캐싱하여 반복 호출 차단 |

### 테스트

| 기술 | 선택 이유 |
|------|-----------|
| **JUnit 5 + MockK** | Kotlin 전용 모킹 라이브러리. `every { } returns` 문법으로 Redis 의존성 모킹 후 비즈니스 로직만 독립 테스트 |

### DevOps

| 기술 | 선택 이유 |
|------|-----------|
| **AWS EC2 (Ubuntu 22.04)** | Spring Boot + Redis를 Docker로 실행. 탄력적 IP로 고정 주소 확보 |
| **Docker / Docker Compose** | 팀원 간 개발 환경 통일. `docker-compose up` 한 번으로 전체 환경 실행 |
| **Docker Hub** | GitHub Actions에서 빌드한 이미지 저장. EC2에서 pull 받아 실행하는 배포 경로의 중간 저장소 |
| **GitHub Actions** | PR 생성 시 JUnit 자동 실행 · 실패 시 머지 차단. main 머지 시 EC2 자동 배포 연결 |

### 협업 도구

| 기술 | 용도 |
|------|------|
| **GitHub** | 소스 코드 버전 관리 및 PR 기반 코드 리뷰 |
| **Jira (KAN 프로젝트)** | 이슈 트래킹 및 스프린트 관리 |
