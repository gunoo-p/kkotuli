# 🎮 끝말잇기 플랫폼

> 끄투 온라인을 오마쥬한 웹 기반 실시간 한국어 끝말잇기 멀티플레이어 게임 플랫폼
> 로그인 없이 방 코드 하나로 참여하는 비회원 실시간 게임 서비스

---


- **사용 언어**: Kotlin 1.9
- **프레임워크**: Spring Boot 3.x
- **이슈 관리**: Jira (KAN 프로젝트)

---

## 🏗️ 시스템 아키텍처

```
[ Client (React) ]
        |
        | ① HTTP REST API          ② WebSocket (STOMP)
        |   방 생성 · 참여              실시간 게임 이벤트 · 채팅
        ↓                                   ↓
┌─────────────────────────────────────────────────┐
│              Spring Boot (Kotlin)               │
│                                                 │
│   ┌─────────────┐       ┌───────────────────┐  │
│   │  REST API   │       │  WebSocket Handler │  │
│   │  Controller │       │  (STOMP Broker)   │  │
│   └──────┬──────┘       └────────┬──────────┘  │
│          │                       │             │
│   ┌──────▼───────────────────────▼──────────┐  │
│   │              Service Layer              │  │
│   │  GameRoomService / WordValidationService│  │
│   │  ChatService / GameSessionService       │  │
│   └─────────────────────┬───────────────────┘  │
│                         │                      │
│   ┌─────────────────────▼───────────────────┐  │
│   │           Redis (유일한 저장소)          │  │
│   │  방 상태 · 참가자 · 단어 Set · 채팅     │  │
│   └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
        |
        | ③ 외부 API 호출
        ↓
[ 국립국어원 표준국어대사전 API ]
```

---

## 🛠️ 사용 기술 및 선택 이유

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

---

## 📋 이슈별 설계 내용

---

### KAN-49 | DB 구축 (Redis)

**이슈 배경**

이 서비스는 회원 정보나 게임 이력을 영구 저장할 필요가 없다.
방은 게임이 끝나면 사라지고, 유저는 닉네임만 쓰는 비회원 구조다.
영구 저장소(MySQL) 없이 Redis 하나로 전체 게임 상태를 관리한다.

**기술 선택: Redis를 유일한 저장소로 사용**

- 게임 진행 중 단어 검증과 턴 상태는 매 초 단위로 읽고 써야 한다
- 인메모리 구조로 밀리초 단위 처리 가능
- TTL(자동 만료) 설정으로 게임 종료 후 별도 삭제 로직 없이 자동 정리

**Redis 자료구조 설계**

각 데이터 특성에 맞는 자료구조를 선택했다.

| 데이터 | 자료구조 | 이유 |
|--------|----------|------|
| 방 기본 정보 | Hash | 여러 필드를 하나의 키로 묶어 원자적 조회 |
| 참가자 목록 | Hash | sessionId → 닉네임 매핑, O(1) 조회 |
| 사용된 단어 | Set | 중복 자동 방지, `SISMEMBER`로 O(1) 중복 검사 |
| 턴 정보 | Hash | 현재 턴 · 마지막 단어 묶음 갱신 |
| 채팅 기록 | List | 순서 보장, `LPUSH + LTRIM`으로 최근 100개 유지 |
| 사전 API 캐시 | String | 단순 키-값, TTL 24시간 |

**Redis 키 구조**

```
room:{roomId}:info      → 방 상태 (Hash) - TTL 2시간
room:{roomId}:players   → 참가자 목록 (Hash) - TTL 2시간
room:{roomId}:words     → 사용된 단어 (Set) - TTL 2시간
room:{roomId}:turn      → 현재 턴 정보 (Hash) - TTL 2시간
room:{roomId}:chat      → 채팅 기록 (List, 최근 100개) - TTL 2시간
word:cache:{단어}       → 사전 API 캐시 (String) - TTL 24시간
```

---

### KAN-46 | 유저 매칭 시스템 구현

**이슈 배경**

로그인/회원가입 없이 방 코드만으로 게임에 참여하는 구조가 필요하다.
유저 식별은 서버가 발급한 세션 ID(UUID)로 처리하며, DB 저장 없이 Redis에만 보관한다.

**방 코드 발급 설계**

```
[문자셋] 영문 대문자 + 숫자 32자
         O·0·I·1 제외 (입력 시 혼동 방지)
         → ABCDEFGHJKLMNPQRSTUVWXYZ23456789

[길이] 6자리 → 약 10억 가지 경우의 수 (32^6)

[발급 흐름]
32자 문자셋에서 6자리 랜덤 추출
    ↓
Redis EXISTS room:{roomCode}:info 확인
    ↓
중복 없음 → 코드 확정
중복 있음 → 재생성 (최대 5회 재시도)
```

| 결정 | 이유 |
|------|------|
| 6자리 | 사용자 직접 입력 → 짧을수록 편함. 10억 가지면 충돌 가능성 무시 가능 |
| O·0·I·1 제외 | 모바일 입력 시 헷갈리는 문자 제거 |
| UUID 미사용 | UUID는 36자 → 사람이 직접 입력하기 부적합 |

**API 설계**

```
방 생성: POST /api/rooms
         Body: { "nickname": "홍길동", "maxPlayers": 4 }
         Response: { "roomCode": "A3F9K2", "sessionId": "uuid-xxx" }

방 참여: POST /api/rooms/join
         Body: { "roomCode": "A3F9K2", "nickname": "김철수" }
         Response: { "roomCode": "A3F9K2", "sessionId": "uuid-yyy" }

방 조회: GET /api/rooms/{roomCode}
         Response: { "status", "players", "maxPlayers" }
```

**에러 처리**

| 상황 | HTTP 상태 |
|------|-----------|
| 존재하지 않는 방 코드 | 404 |
| 이미 게임 중인 방 | 409 |
| 정원 초과 | 409 |

---

### KAN-17 | 실시간 게임 통신

**이슈 배경**

끝말잇기는 상대방의 단어 제출 결과, 턴 전환, 타이머를 모든 참가자가 동시에 받아야 한다.
HTTP 요청-응답 구조로는 서버가 먼저 데이터를 보내는 것이 불가능하다.

**기술 선택: WebSocket + STOMP**

- **WebSocket**: 서버가 클라이언트에 직접 push 가능한 양방향 지속 연결
- **STOMP**: 순수 WebSocket 위에 pub/sub 프로토콜을 얹어 방마다 독립 채널 구성

**토픽 구조 설계**

게임 이벤트와 채팅을 별도 토픽으로 분리했다.
프론트엔드에서 게임 로직과 채팅 UI를 독립적으로 처리할 수 있다.

| 구분 | 경로 | 용도 |
|------|------|------|
| 구독 | `/topic/room/{roomCode}` | 게임 이벤트 수신 |
| 구독 | `/topic/room/{roomCode}/chat` | 채팅 수신 |
| 전송 | `/app/game/start` | 게임 시작 (방장 전용) |
| 전송 | `/app/game/word` | 단어 제출 |
| 전송 | `/app/game/chat` | 채팅 전송 |
| 전송 | `/app/player/update` | 닉네임 · 표정 · 색상 변경 (대기실 전용) |

**브로드캐스트 메시지 타입**

```
WORD_RESULT   → 단어 제출 결과 (성공/실패, 다음 턴 정보)
TURN_TIMEOUT  → 시간 초과로 인한 플레이어 탈락
GAME_OVER     → 게임 종료 및 결과
ROOM_CLOSED   → 방 강제 종료 (호스트 퇴장)
PLAYER_JOINED → 플레이어 입장 알림
PLAYER_LEFT        → 플레이어 퇴장 알림
PLAYER_UPDATED     → 닉네임 · 표정 · 색상 변경 (전체 브로드캐스트)
PLAYER_UPDATE_ERROR→ 커스터마이징 유효성 검사 실패 (요청자 개별 전송)
```

**동시성 처리: Java 21 가상 쓰레드 적용**

WebSocket 연결 유지는 전형적인 I/O 대기 구조다.
전통 쓰레드는 연결 수만큼 쓰레드를 점유하지만,
가상 쓰레드는 대기 구간에서 OS 쓰레드를 자동 반납하여 적은 자원으로 더 많은 연결을 처리한다.

```yaml
# application.yml - 설정 한 줄로 적용
spring:
  threads:
    virtual:
      enabled: true
```

---

### KAN-16 | 끝말잇기 로직

**이슈 배경**

단어 제출 시 끝말잇기 규칙을 서버에서 검증해야 한다.
클라이언트 검증만으로는 조작이 가능하므로 모든 검증은 서버에서 수행한다.

**검증 단계 설계**

빠른 검증을 먼저 수행하고 느린 검증(외부 API)을 나중에 실행하여 불필요한 API 호출을 최소화했다.

```
[1단계] 현재 턴 세션 ID 확인        → Redis 조회 (빠름)
[2단계] 끝글자 일치 여부 확인        → 메모리 연산 (가장 빠름)
[3단계] 중복 단어 확인               → Redis SISMEMBER (빠름)
[4단계] 실존 단어 확인               → 외부 API 호출 (느림, 마지막 실행)
```

**한국어 처리: 두음법칙**

```
"나라" → 마지막 글자 "라"
         두음법칙 미적용: "라"로 시작하는 단어만 허용
         두음법칙 적용:  "나"로 시작하는 단어도 허용
```

두음법칙 허용 여부는 방 생성 시 옵션으로 설정 가능하며 `room:{roomId}:info`에 저장한다.

**턴 타이머**

- 제한 시간: 기본 30초
- Spring `@Scheduled`로 매 초 Redis 턴 정보 확인
- 타임아웃 시 해당 플레이어 탈락 → 다음 턴 자동 전환 → BROADCAST

> 가상 쓰레드가 아닌 `@Scheduled`를 사용하는 이유:
> 순수 시간 대기는 I/O 대기가 아니므로 가상 쓰레드의 이점이 없다.
> 스케줄러가 이 역할에 더 적합하다.

---

### KAN-36 | 단어사전 API 연결

**이슈 배경**

제출된 단어가 실제 존재하는 한국어 단어인지 검증이 필요하다.
한국어 단어 전체를 직접 DB에 적재하면 수십만 건 데이터 관리가 필요하다.

**기술 선택: 국립국어원 표준국어대사전 Open API**

- 공식 표준어 기준으로 검증 가능
- 직접 사전 DB 구축 없이 외부 위임으로 유지보수 비용 제거

**성능 문제 해결: Redis 캐싱 전략**

외부 API 호출은 네트워크 지연(50~200ms)이 발생한다.
실시간 게임에서 매 단어마다 이 지연이 발생하면 게임 흐름이 끊긴다.

```
단어 제출
    ↓
Redis word:cache:{단어} 조회
    ↓
캐시 HIT  → 즉시 결과 반환 (API 호출 없음)
캐시 MISS → 국립국어원 API 호출 → Redis 캐싱 (TTL 24시간) → 결과 반환
```

**장애 대응 (Fallback)**

외부 API 호출 실패 시 검증 통과 처리한다.
API 장애로 게임 전체가 멈추는 상황이 잘못된 단어 하나가 통과되는 것보다 더 큰 문제이기 때문이다.

---

### KAN-15 | 게임방 채팅

**이슈 배경**

게임 진행 중 참가자 간 실시간 소통 기능이 필요하다.
채팅 메시지는 게임 이벤트와 혼재되면 안 되므로 별도 채널로 분리한다.

**설계 결정: 게임 이벤트와 채팅 토픽 분리**

- 게임 이벤트: `/topic/room/{roomCode}`
- 채팅: `/topic/room/{roomCode}/chat`

분리하지 않으면 프론트엔드에서 메시지 타입을 매번 파싱해야 하고,
채팅 UI와 게임 로직이 강하게 결합된다.

**채팅 이력 관리: Redis List**

```
LPUSH room:{roomId}:chat "{닉네임}:{메시지}:{timestamp}"
LTRIM room:{roomId}:chat 0 99   → 최근 100개만 유지, 초과분 자동 삭제
```

메모리 사용량을 100개로 고정하여 무제한 적재를 방지한다.
게임 종료 후 TTL(2시간)로 채팅 기록도 함께 소멸한다.

---

### KAN-66 | 게임 완료 후 결과창

**이슈 배경**

게임이 종료되면 참가자 전원에게 결과를 동시에 전달해야 한다.
비회원 구조이므로 결과를 DB에 영구 저장하지 않는다.

**게임 종료 조건**

| 조건 | 처리 |
|------|------|
| 최후 1인 생존 | 생존자 우승 |
| 전체 게임 시간 초과 | 생존 턴 수가 가장 많은 플레이어 우승 |
| 호스트 강제 종료 | KAN-67 연계 → ROOM_CLOSED 발송 |

**결과 브로드캐스트 후 즉시 정리**

```
게임 종료 트리거
    ↓
Redis players에서 생존 턴 수 집계
    ↓
GAME_OVER 메시지 브로드캐스트 → /topic/room/{roomCode}
    ↓
room:{roomId}:* 키 전체 삭제 (TTL 즉시 만료)
```

결과를 DB에 저장하지 않고 Redis 집계 후 즉시 브로드캐스트하는 이유는
비회원 서비스에서 영구 저장이 불필요한 데이터를 무겁게 다루지 않기 위해서다.

---

### KAN-67 | 게임 호스트 퇴장 시 남은 인원도 퇴장

**이슈 배경**

방장이 나가면 방을 유지할 주체가 없다.
방장 없이 게임이 계속되면 턴 관리 · 시작 권한 등 예외 상황이 연쇄 발생한다.

**설계 결정: 방장 퇴장 = 방 즉시 소멸**

예외 케이스를 줄이기 위해 단순한 규칙을 적용했다.
방장 위임 같은 복잡한 처리 없이 방장 퇴장 시 방 전체를 즉시 종료한다.

**퇴장 감지 방식**

Spring WebSocket의 `SessionDisconnectEvent`를 리스닝한다.
클라이언트가 명시적으로 퇴장 API를 호출하지 않아도
브라우저 강제 종료 · 네트워크 끊김 등 모든 상황에서 서버가 정확하게 감지한다.

**처리 흐름**

```
SessionDisconnectEvent 발생
    ↓
퇴장 세션 ID == room:{roomId}:info의 hostSessionId?
    ↓ YES                               ↓ NO
    |                           players에서 제거
    |                           BROADCAST: PLAYER_LEFT
    ↓
BROADCAST: ROOM_CLOSED { "reason": "HOST_LEFT" }
    ↓
room:{roomId}:* 전체 키 삭제
```

게임 진행 중(PLAYING) 호스트 퇴장도 동일하게 방 즉시 종료한다.
결과 집계 없이 ROOM_CLOSED만 발송하며, 클라이언트는 로비 화면으로 이동한다.

---


---

### KAN-NEW | 캐릭터 커스터마이징 및 랜덤 닉네임

**이슈 배경**

로그인 없는 비회원 구조에서 플레이어를 시각적으로 구별할 수단이 필요하다.
닉네임만으로는 누가 누구인지 한눈에 파악하기 어려우므로,
픽셀 아트 스타일 캐릭터(표정 + 색상)로 각 플레이어를 시각적으로 식별한다.

**커스터마이징 구성 요소**

| 요소 | 선택지 | 저장 위치 |
|------|--------|-----------|
| 표정 | 픽셀 아트 얼굴 8종 (기쁨·당황·화남·졸림 등) | Redis (플레이어 정보) |
| 색상 | 8가지 캐릭터 배경색 | Redis (플레이어 정보) |
| 닉네임 | 랜덤 배정 + 직접 수정 가능 | Redis (플레이어 정보) |

**닉네임 입력 방식**

닉네임은 두 가지 방식으로 설정할 수 있다.
서버에서 랜덤 닉네임을 먼저 배정한 뒤, 사용자가 원하면 직접 입력으로 덮어쓸 수 있다.

```
[방식 1] 랜덤 배정 (기본값)
형용사 + 동물명 조합 자동 생성
예: "용감한 고양이", "졸린 판다", "빠른 수달"

형용사 30개 × 동물명 30개 → 최대 900가지 조합

발급 흐름:
방 참여 요청
    ↓
서버에서 형용사 + 동물명 랜덤 조합 생성
    ↓
같은 방 내 중복 닉네임 여부 확인
    ↓
중복 없음 → 해당 닉네임 배정
중복 있음 → 재생성 (최대 5회 재시도)
    ↓
클라이언트에 랜덤 닉네임 반환

[방식 2] 직접 입력 (선택)
클라이언트에서 닉네임 입력란을 직접 수정
    ↓
PATCH /api/rooms/{roomCode}/players/me 호출
    ↓
서버에서 유효성 검사 (길이·허용 문자·공백·빈값)
    ↓
같은 방 내 동일 닉네임 존재 여부 확인
    ↓
중복 없음 → 입력값 그대로 저장
중복 있음 → 숫자 접미사 자동 부여 후 저장
           예: "고양이" 중복 → "고양이2"
               "고양이2" 중복 → "고양이3" (최대 9까지)
    ↓
Redis 갱신 + PLAYER_UPDATED 브로드캐스트
(최종 확정된 닉네임을 응답으로 반환하여 클라이언트에 표시)
```

**닉네임 제약 조건**

직접 입력과 랜덤 배정 모두 동일한 제약을 적용한다.

| 규칙 | 내용 |
|------|------|
| 최대 길이 | 8자 |
| 허용 문자 | 한글 · 영문 · 숫자만 허용 (특수문자 불가) |
| 공백 | 앞뒤 공백 자동 제거, 중간 공백 불가 |
| 중복 | 같은 방 내 중복 시 자동으로 숫자 접미사 부여 (예: 고양이 → 고양이2 → 고양이3), 다른 방과는 무관 |
| 수정 가능 시점 | 게임 시작 전 대기실에서만 수정 가능 |
| 빈 값 제출 | 서버에서 거부 → 기존 닉네임 유지 |

**Redis 플레이어 정보 구조 확장**

기존 `room:{roomId}:players` Hash에 커스터마이징 정보를 함께 저장한다.

```
room:{roomId}:players (Hash)
    {sessionId} → JSON 직렬화
    {
      "nickname": "용감한 고양이",
      "expression": 3,        ← 표정 인덱스 (0~7)
      "color": "#7F77DD"      ← 캐릭터 배경색 HEX
    }
```

별도 키를 추가하지 않고 기존 Hash 값을 JSON으로 확장하여
기존 TTL(2시간) 정책을 그대로 유지한다.

**커스터마이징 API 설계**

초기 배정은 REST API, 이후 수정은 WebSocket으로 처리한다.
대기실 진입 시 이미 WebSocket이 연결된 상태이므로,
수정 요청을 REST API로 한 번 더 보내는 불필요한 왕복을 없앤다.

```
[REST API] 방 참여 시 초기 배정 (1회성)
POST /api/rooms/join
Response: { "roomCode": "A3F9K2", "sessionId": "uuid",
            "nickname": "용감한 고양이", "expression": 3, "color": "#7F77DD" }

[WebSocket] 닉네임 수정 (게임 시작 전, 대기실에서)
SEND /app/player/update
Body: { "sessionId": "uuid", "nickname": "수정된이름" }

처리 순서:
1. 유효성 검사 (길이·허용 문자·공백·빈값)
2. 같은 방 내 중복 확인
   중복 없음 → 입력값 그대로
   중복 있음 → 숫자 접미사 자동 부여 (예: 고양이 → 고양이2)
3. Redis 갱신
4. BROADCAST → /topic/room/{roomCode}
   { "type": "PLAYER_UPDATED", "sessionId": "uuid",
     "nickname": "고양이2", "expression": 3, "color": "#7F77DD" }
   (확정된 닉네임을 브로드캐스트하여 본인 포함 전원 즉시 반영)

[WebSocket] 표정·색상 변경 (게임 시작 전, 대기실에서)
SEND /app/player/update
Body: { "sessionId": "uuid", "expression": 5, "color": "#1D9E75" }

처리 순서:
1. Redis 갱신
2. BROADCAST → /topic/room/{roomCode}
   { "type": "PLAYER_UPDATED", "sessionId": "uuid",
     "nickname": "용감한 고양이", "expression": 5, "color": "#1D9E75" }
```

**유효성 검사 실패 처리**

REST API와 달리 WebSocket은 HTTP 상태 코드를 쓸 수 없다.
검증 실패 시 요청자 세션에만 에러 메시지를 개별 전송한다.

```
SEND /app/player/update (닉네임 8자 초과 입력)
    ↓
서버 유효성 검사 실패
    ↓
요청자에게만 개별 전송 (브로드캐스트 아님)
{ "type": "PLAYER_UPDATE_ERROR",
  "reason": "NICKNAME_TOO_LONG" }   ← 클라이언트에서 에러 메시지 표시
```

**커스터마이징 정보 전파**

플레이어가 표정·색상·닉네임을 변경하면 같은 방의 모든 참가자에게 즉시 반영된다.

```
PATCH 요청
    ↓
Redis 갱신
    ↓
BROADCAST → /topic/room/{roomCode}
{
  "type": "PLAYER_UPDATED",
  "sessionId": "uuid",
  "nickname": "용감한 고양이",
  "expression": 3,
  "color": "#7F77DD"
}
```

**프론트엔드 렌더링 방식 (참고)**

픽셀 아트 얼굴은 서버에서 이미지를 관리하지 않는다.
클라이언트에서 표정 인덱스(0~7)를 받아 미리 정의된 픽셀 아트 스프라이트로 렌더링한다.
서버는 인덱스 숫자만 저장·전달하면 되므로 이미지 저장소가 불필요하다.

```
서버 저장: expression: 3  (숫자만)
클라이언트: 인덱스 3 → 스프라이트 시트의 3번 픽셀 아트 얼굴 렌더링
```

**설계 목적**

회원 가입 없이도 플레이어 간 시각적 식별이 가능하고,
표정·색상 조합(8×8 = 64가지)으로 같은 방 내에서 충분한 다양성을 확보한다.
커스터마이징 정보를 기존 Redis 구조에 통합하여 별도 저장소 추가 없이 구현하고,
게임 종료 후 TTL과 함께 자동 소멸되므로 서버 관리 부담이 없다.

**TDD 테스트 항목 추가**

| 테스트 | 검증 내용 |
|--------|-----------|
| `NicknameGeneratorTest` | 같은 방 내 중복 닉네임 재생성, 5회 재시도 후 예외 처리 |
| `PlayerCustomizationServiceTest` | 닉네임 8자 초과 거부, 게임 중 수정 시도 거부, 특수문자 거부 |

---


---

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

**GitHub Secrets 설정 항목**

민감 정보는 코드에 직접 쓰지 않고 GitHub Secrets에 저장한다.

| Secret 이름 | 내용 |
|-------------|------|
| `EC2_HOST` | EC2 탄력적 IP 주소 |
| `EC2_USER` | EC2 접속 유저명 (ubuntu) |
| `EC2_SSH_KEY` | EC2 접속용 PEM 키 (private key) |
| `DOCKER_USERNAME` | Docker Hub 계정명 |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 |
| `DICTIONARY_API_KEY` | 국립국어원 API 키 |
| `REDIS_PASSWORD` | Redis 접속 비밀번호 |

**EC2에서 실행되는 docker-compose.yml 구조**

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

> Redis는 포트를 외부에 노출하지 않는다 (`ports` 설정 없음).
> backend 컨테이너가 같은 네트워크 내에서 `redis` 호스트명으로 접근한다.

**배포 흐름 설계 목적**

- 테스트 실패 시 배포 자체가 불가능하므로 TDD가 실질적 의미를 가진다
- 개발자가 서버에 직접 SSH 접속해서 명령어를 칠 필요가 없다
- Docker Hub를 중간 저장소로 두어 EC2가 항상 검증된 이미지만 받는다
- 환경변수를 GitHub Secrets로 관리하여 API 키·비밀번호가 코드에 노출되지 않는다


## 🔄 전체 게임 흐름

```
[방 생성]   POST /api/rooms
            → 방 코드(A3F9K2) + 세션 ID 발급
            → Redis 초기화 (TTL 2시간)

[방 참여]   POST /api/rooms/join
            → 세션 ID 발급 → Redis 플레이어 등록

[WS 연결]   ws://{host}/ws?roomCode=A3F9K2&sessionId=uuid
            → /topic/room/A3F9K2 구독

[게임 시작] 방장 → SEND /app/game/start
            → 턴 순서 결정 → BROADCAST: GAME_START

[게임 진행] 턴 플레이어 → SEND /app/game/word { "word": "사과" }
            → KAN-16 끝말 검증 → KAN-36 사전 검증
            → 성공: BROADCAST WORD_RESULT → 다음 턴
            → 실패/타임아웃: BROADCAST TURN_TIMEOUT → 탈락

[게임 종료] 최후 1인 → KAN-66 BROADCAST GAME_OVER → Redis 삭제

[호스트 퇴장] SessionDisconnectEvent → KAN-67 BROADCAST ROOM_CLOSED → Redis 삭제
```

---


---

## 🔥 트러블슈팅

---

### TS-01 | REST API → WebSocket + STOMP 전환

**문제 상황**

초기 설계에서 단어 제출·턴 전환·타이머를 전부 REST API로 구현하려 했다.
설계를 진행하다 세 가지 구조적 한계를 발견했다.

- 상대방이 단어를 제출했는지 클라이언트가 알 방법이 없다.
  REST는 클라이언트가 먼저 요청해야 서버가 응답하는 구조라 서버가 먼저 push할 수 없다.
  결국 클라이언트가 1초마다 "바뀐 거 있어요?"를 반복 요청해야 한다. (폴링)
- 8명이 1초마다 폴링하면 대부분 "없음"을 돌려주는 불필요한 요청이 폭발적으로 증가한다.
- 30초 타이머 동기화가 구조적으로 불가능하다.
  타임아웃 시 서버가 먼저 "탈락"을 push해야 하는데 REST로는 클라이언트 요청 없이 아무것도 보낼 수 없다.

**해결 1단계 — WebSocket 도입**

WebSocket은 한 번 연결하면 양쪽이 언제든 먼저 데이터를 보낼 수 있는 양방향 지속 연결이다.
서버가 먼저 push할 수 있어 폴링 없이 실시간 동기화가 가능해졌다.

**해결 2단계 — 순수 WebSocket이 아닌 STOMP 선택**

순수 WebSocket만 쓰면 방마다 채널 분리, 메시지 형식 정의, 구독 관리를 전부 직접 구현해야 한다.
STOMP의 pub/sub 구조를 얹으니 이 복잡도를 프로토콜 수준에서 해결하고 구조를 단순화할 수 있었다.

```
[순수 WebSocket]
모든 연결이 하나로 묶임 → 방별 채널 분리 직접 구현 필요

[STOMP]
/topic/room/{roomCode}       ← 방별 독립 채널 자동 분리
/topic/room/{roomCode}/chat  ← 채팅 채널 분리
서버: convertAndSend() 한 줄로 방 전체에 브로드캐스트
```

**최종 구조**

REST API는 방 생성·참여처럼 게임 시작 전 1회성 요청에만 남기고,
대기실 진입 후 모든 실시간 통신은 WebSocket으로 처리했다.

---

### TS-02 | MySQL + Redis 이중 구조 → Redis 단일 저장소로 전환

**문제 상황**

초기 설계에서 게임 이력과 랭킹을 MySQL에, 게임 진행 상태를 Redis에 저장하는 이중 구조로 잡았다.
설계를 진행하다 보니 이 서비스는 비회원 구조라 유저 자체가 없고,
게임이 끝나면 방도 사라지는 일회성 구조라는 것을 발견했다.

MySQL에 저장해봤자 조회할 주체(회원)가 없으니 데이터가 쌓이기만 하는 구조였다.
또한 MySQL을 함께 운영하면 EC2에 MySQL 컨테이너까지 추가로 띄워야 해서 서버 자원 부담도 컸다.

**원인 분석**

회원 기반 서비스를 기준으로 설계를 시작하다 보니 영구 저장소가 당연하다고 가정했다.
비회원 방 코드 방식으로 방향이 바뀌었는데 저장소 설계는 변경되지 않은 것이다.

**해결**

MySQL을 제거하고 Redis 단일 저장소로 전환했다.

게임 진행 중 필요한 모든 데이터는 Redis에 저장하고,
게임 종료·호스트 퇴장 시 코드로 즉시 삭제하며
TTL(2시간)을 설정하여 비정상 종료 시에도 데이터가 자동으로 소멸되는 구조로 설계했다.
이를 통해 서버 자원을 최적화하고 별도 삭제 로직의 부담을 줄였다.

```
[기존]
MySQL (게임 이력·랭킹) + Redis (게임 상태) → 이중 구조

[변경 후]
Redis 단일 저장소
  ├── 게임 진행 중: 방 상태·참가자·단어·턴·채팅 저장
  ├── 게임 정상 종료: 코드로 즉시 삭제
  ├── 호스트 퇴장: 코드로 즉시 삭제
  └── 비정상 종료: TTL 2시간 후 자동 소멸 (보험)
```

**결과 및 배운 점**

기술 선택은 서비스 성격을 먼저 정의한 뒤에 해야 한다는 것을 배웠다.
"비회원 일회성 서비스"라는 특성을 먼저 명확히 했다면 처음부터 Redis만 쓰는 구조로 갔을 것이다.
기술 결정보다 서비스 정의가 먼저임을 배웠다.

## 🧪 TDD 테스트 항목

| 테스트 클래스 | 검증 항목 | 연관 이슈 |
|--------------|-----------|-----------|
| `GameRoomServiceTest` | 방 코드 중복 재생성, 정원 초과 거부, 존재하지 않는 방 참여 | KAN-46 |
| `WordValidationServiceTest` | 끝글자 일치, 중복 단어 거부, 두음법칙 처리 | KAN-16 |
| `GameSessionServiceTest` | 턴 전환, 타임아웃 처리, 호스트 퇴장 시 방 소멸 | KAN-17, KAN-67 |
| `DictionaryServiceTest` | 캐시 HIT 시 API 미호출, API 실패 시 fallback 통과 | KAN-36 |
| `ChatServiceTest` | 100개 초과 시 오래된 메시지 자동 삭제 | KAN-15 |
| `GameResultServiceTest` | 생존 턴 수 집계, 동점자 처리, 종료 후 Redis 정리 | KAN-66 |
| `NicknameGeneratorTest` | 같은 방 내 중복 재생성, 5회 재시도 후 예외 처리 | KAN-NEW |
| `PlayerCustomizationServiceTest` | 닉네임 8자 초과·특수문자·공백·빈값·게임 중 수정 거부, 중복 시 숫자 접미사 자동 부여 (고양이→고양이2), 최종 닉네임 응답 반환 확인 | KAN-NEW |
