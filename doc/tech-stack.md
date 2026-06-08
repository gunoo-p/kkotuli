# 기술 스택 및 선택 이유

| 기술 | 선택 이유 |
|------|-----------|
| **Kotlin 1.9** | `data class`로 DTO 보일러플레이트 제거, Null Safety로 런타임 NPE 방지, Spring Boot 3.x 공식 지원. Java 대비 코드량 약 30~40% 감소 |
| **Spring Boot 3.x** | REST API · WebSocket · 의존성 주입을 하나의 프레임워크에서 일관되게 처리 |
| **Spring WebSocket + STOMP** | 방마다 독립 채널(`/topic/room/{roomCode}`)을 만들어 브로드캐스트 범위를 방 단위로 격리 |
| **Spring Data Redis** | `RedisTemplate`으로 Hash · Set · List · TTL 등 자료구조를 타입 안전하게 사용 |
| **Redis 7.x** | 인메모리 구조로 게임 진행 중 단어 검증 · 턴 상태를 밀리초 단위 처리. TTL로 게임 종료 후 자동 소멸. MySQL 불필요 (영구 저장 데이터 없음) |
| **Java 21 가상 쓰레드** | WebSocket 동시 연결 · Redis 대기 · 외부 API 대기 구간에서 OS 쓰레드 자동 반납. `application.yml` 설정 한 줄로 적용, 코드 변경 없음 |
| **국립국어원 Open API** | 한국어 표준어 검증을 외부에 위임. 직접 사전 DB 구축 대비 유지보수 비용 없음 |
| **JUnit 5 + MockK** | Kotlin 전용 모킹 라이브러리. `every { } returns` 문법으로 Redis 의존성 모킹 후 비즈니스 로직만 독립 테스트 |
| **Docker / Docker Compose** | Redis 환경 컨테이너화. `docker-compose up` 한 번으로 팀 전체 개발 환경 통일 |
| **AWS EC2** | 애플리케이션 서버 호스팅. Spring Boot + Redis를 EC2 위에서 Docker로 실행. 탄력적 IP로 고정 주소 확보 |
| **GitHub Actions** | PR 생성 시 JUnit 자동 실행 · 실패 시 머지 차단. main 브랜치 머지 시 EC2 자동 배포까지 연결 |
| **Docker Hub** | GitHub Actions에서 빌드한 이미지를 푸시. EC2에서 pull 받아 실행하는 배포 경로의 중간 저장소 |
