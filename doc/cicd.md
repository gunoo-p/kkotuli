# CI/CD 파이프라인 (GitHub Actions + AWS EC2)

## 인프라 구성

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

## AWS EC2 구성

| 항목 | 내용 |
|------|------|
| 인스턴스 타입 | t3.small (또는 t2.micro - 프리티어) |
| OS | Ubuntu 22.04 LTS |
| 탄력적 IP | 고정 IP로 재시작 후에도 주소 유지 |
| 보안 그룹 | 8080 (Spring Boot), 6379 (Redis - 내부만), 22 (SSH) |
| 설치 환경 | Docker + Docker Compose만 설치 (JDK 불필요) |

## 파이프라인 흐름

```
[트리거 1] PR 생성 시 → CI만 실행 (테스트)
[트리거 2] main 브랜치 머지 시 → CI + CD 전체 실행

# CI 단계 (공통)
1. 코드 체크아웃
2. Kotlin + Spring Boot 빌드 (./gradlew build)
3. JUnit 테스트 실행 → 실패 시 파이프라인 중단, PR 머지 차단

# CD 단계 (main 머지 시에만)
4. Docker 이미지 빌드
5. Docker Hub 로그인 + 이미지 push
6. EC2 SSH 접속
7. docker pull → docker-compose down → docker-compose up -d
```

## GitHub Secrets

| Secret 이름 | 내용 |
|-------------|------|
| `EC2_HOST` | EC2 탄력적 IP 주소 |
| `EC2_USER` | EC2 접속 유저명 (ubuntu) |
| `EC2_SSH_KEY` | EC2 접속용 PEM 키 |
| `DOCKER_USERNAME` | Docker Hub 계정명 |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 |
| `DICTIONARY_API_KEY` | 국립국어원 API 키 |
| `REDIS_PASSWORD` | Redis 접속 비밀번호 |

## docker-compose.yml (EC2)

```yaml
services:
  backend:
    image: {dockerhub-id}/ggtu-backend:latest
    ports:
      - "8080:8080"
    environment:
      - REDIS_HOST=redis
      - REDIS_PASSWORD=${REDIS_PASSWORD}
      - DICTIONARY_API_KEY=${DICTIONARY_API_KEY}
    depends_on:
      - redis
    networks:
      - ggtu-net

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    networks:
      - ggtu-net
    volumes:
      - redis-data:/data

networks:
  ggtu-net:

volumes:
  redis-data:
```

> Redis는 포트를 외부에 노출하지 않는다. backend 컨테이너가 같은 네트워크 내에서 `redis` 호스트명으로 접근한다.
