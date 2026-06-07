
### CI/CD 파이프라인 (GitHub Actions + AWS EC2)

**이슈 배경**

코드를 수동으로 서버에 올리면 팀원마다 배포 방식이 달라지고,
테스트를 건너뛴 코드가 서버에 올라가는 사고가 생긴다.
GitHub Actions로 테스트 → 빌드 → 배포를 자동화하여 이를 방지한다.

**인프라 구성**

```
[ 개발자 PC ]
      |
      | git push / PR
      ↓
[ GitHub ]
      |
      | GitHub Actions 트리거
      ↓
┌─────────────────────────────┐
│      GitHub Actions         │
│                             │
│  ① 빌드 + JUnit 테스트      │
│  ② Docker 이미지 빌드       │
│  ③ Docker Hub push          │
│  ④ EC2 SSH 접속 → 배포      │
└─────────────────────────────┘
      |
      | Docker pull + 재시작
      ↓
[ AWS EC2 ]
  ├── Spring Boot 컨테이너
  └── Redis 컨테이너
      (docker-compose로 함께 실행)
```

**AWS EC2 구성**

| 항목 | 내용 |
|------|------|
| 인스턴스 타입 | t3.small (또는 t2.micro - 프리티어) |
| OS | Ubuntu 22.04 LTS |
| 탄력적 IP | 고정 IP로 도메인 연결 및 재시작 후에도 주소 유지 |
| 보안 그룹 | 8080 (Spring Boot), 6379 (Redis - 내부만), 22 (SSH - GitHub Actions IP) |
| 설치 환경 | Docker + Docker Compose만 설치 (JDK 불필요) |

> Redis 포트(6379)는 외부에서 접근 불가하도록 보안 그룹에서 차단.
> Spring Boot 컨테이너와 Redis 컨테이너가 같은 Docker 네트워크 내에서만 통신.

**파이프라인 상세 흐름**

```
[트리거 1] PR 생성 시 → CI만 실행 (테스트)
[트리거 2] main 브랜치 머지 시 → CI + CD 전체 실행
```

```
# CI 단계 (PR + main 머지 공통)
1. 코드 체크아웃
2. Kotlin + Spring Boot 빌드 (./gradlew build)
3. JUnit 테스트 실행 (./gradlew test)
   → 실패 시 파이프라인 중단, PR 머지 차단

# CD 단계 (main 머지 시에만)
4. Docker 이미지 빌드
   (docker build -t {dockerhub-id}/ggtu-backend:latest .)
5. Docker Hub 로그인 + 이미지 push
6. EC2 SSH 접속 (GitHub Secrets에 저장된 키 사용)
7. EC2에서 실행:
   docker pull {dockerhub-id}/ggtu-backend:latest
   docker-compose down
   docker-compose up -d
```


**EC2에서 실행되는 docker-compose.yml 구조**

```yaml
services:
  backend:
    image: {dockerhub-id}/kkotuli-backend:latest
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=redis
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - DICTIONARY_API_KEY=${DICTIONARY_API_KEY}
    depends_on:
      - redis
    networks:
      - kkotuli-net

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    networks:
      - kkotuli-net
    volumes:
      - redis-data:/data

networks:
  ggtu-net:

volumes:
  redis-data:
```