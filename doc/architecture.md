# 시스템 아키텍처

## 구조도

![시스템 아키텍처](images/msa_archi.png)

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

## 전체 게임 흐름

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

### 아키텍처 핵심 결정

**REST API + WebSocket 혼용**
방 생성·참여처럼 1회성 요청은 REST, 게임 중 실시간 이벤트는 WebSocket으로 목적에 따라 분리했다.

**Redis 단일 저장소**
비회원 일회성 서비스 특성상 영구 저장이 불필요하다. 인메모리 구조로 빠른 처리 속도를 확보하고, TTL로 게임 종료 후 자동 정리한다.

**비회원 방 코드 방식**
로그인 없이 세션 ID(UUID)로 유저를 식별하여 진입 장벽을 제거했다.

---