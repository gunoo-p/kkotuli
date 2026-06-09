# TDD 테스트 전략

## 개요

3단계 피라미드 구조로 테스트를 작성한다.
단순한 도메인 로직부터 시작해 복잡한 시나리오까지 점진적으로 검증한다.

```
         ▲
        / \
       / L \   Large  — 전체 사용자 시나리오 (6개 파일, 25개 테스트)
      /-----\
     /   M   \  Medium — 서비스 단위 동작 (6개 파일, 24개 테스트)
    /---------\
   /     S     \ Small  — 핵심 도메인 로직 (8개 파일, 44개 테스트)
  /─────────────\
```

총 93개 테스트 · 전 구간 통과

---

## 실행 방법

```bash
# 전체 테스트 실행
./gradlew test

# 테스트 결과 리포트
build/reports/tests/test/index.html
```

---

## Small — 핵심 도메인 로직

의존성 없는 순수 로직을 단위 테스트한다.
외부 연결 없이 빠르게 실행된다.

### `small.game`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `WordValidatorTest` | 끝 글자와 첫 글자 일치 시 성공, 불일치·한 글자·빈 문자열 시 `InvalidWordException` |
| `TurnManagerTest` | 첫 플레이어 선택, 다음 플레이어 전환, 마지막 → 첫 번째 순환, 탈락자 건너뜀 |
| `UsedWordManagerTest` | 단어 저장·포함 여부 확인, 중복 입력 시 `DuplicateWordException` |
| `TimerManagerTest` | 30초 시작, 1초 감소, 0초 도달, 시간 초과 상태 전환 |
| `HistoryManagerTest` | 단어 저장, 입력 순서 유지, 전체 히스토리 조회 |
| `ResultManagerTest` | 1인 생존 시 게임 종료, 생존자가 승자, 2인 이상 시 게임 진행 중 |

### `small.avatar`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `AvatarTest` | 유효 범위(0~15) 아바타 생성, 범위 초과·음수 시 `InvalidAvatarException` |

### `small.chat`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `ChatMessageValidatorTest` | 정상 메시지 생성, 빈 문자열·공백·100자 초과 시 `InvalidChatMessageException` |

### `small.nickname`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `NicknameGeneratorTest` | 닉네임 생성, 빈 문자열 아님, 동사+동물 조합 최소 2글자, 100회 반복 생성 |

### `small.room`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `RoomCodeGeneratorTest` | 코드 생성, 6자리, 대문자+숫자 형식(`[A-Z0-9]{6}`), 1000회 반복 검증 |

---

## Medium — 서비스 단위 동작

여러 도메인 객체가 협력하는 서비스 계층을 테스트한다.

### `medium.room`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `RoomServiceTest` | 방 생성·조회·삭제, 존재하지 않는 방 조회 시 `RoomNotFoundException` |
| `JoinRoomTest` | 방 입장 및 플레이어 목록 반영, 랜덤 닉네임 배정, 최대 인원(4명) 초과 시 `RoomFullException`, 진행 중 방 입장 시 `GameAlreadyStartedException` |

### `medium.game`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `GameServiceTest` | 정상 단어 입력, 사전 없는 단어 `WordNotFoundException`, 중복 단어 `DuplicateWordException`, 입력 후 다음 플레이어 전환, 1인 생존 시 게임 종료 |
| `HistoryServiceTest` | 단어 히스토리 저장 순서, 모든 플레이어가 동일한 히스토리 조회 |

### `medium.dictionary`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `DictionaryServiceTest` | 존재 단어 `true`, 미존재 단어 `false`, 빈 문자열 `false` |

### `medium.redis`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `RedisRoomTest` | 게임방 저장·조회·삭제, 삭제 후 조회 시 `RoomNotFoundException` |

### `medium.websocket`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `ChatWebSocketTest` | 채팅 브로드캐스트 확인, 모든 플레이어 동일 메시지 수신, 게임과 독립 동작 |
| `GameWebSocketTest` | 게임 상태 브로드캐스트 확인, 모든 플레이어 동일 상태 수신, 턴 변경 전파 |

---

## Large — 전체 사용자 시나리오

실제 플레이어 관점의 흐름을 End-to-End로 검증한다.

### `large.room`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `CreateRoomScenarioTest` | 아바타 선택 후 방 생성, 6자리 코드 부여, 생성자가 방장, 초기 상태 대기 |
| `JoinRoomScenarioTest` | 방 코드로 입장, 랜덤 닉네임 부여, 인원 갱신, 최대 4인 |

### `large.game`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `StartGameScenarioTest` | 방장 게임 시작, 상태 `RUNNING` 전환, 첫 플레이어 결정, 타이머 시작 |
| `WordChainScenarioTest` | 연속 끝말잇기, 히스토리 저장, 단어 후 턴 이동, 중복 단어 `DuplicateWordException` |
| `TimeOutScenarioTest` | 시간 초과 시 현재 플레이어 탈락, 참가자 목록에서 제외, 다음 턴 전환, 1인 탈락 후 게임 지속 |
| `GameOverScenarioTest` | 1인 생존 시 게임 종료, 최후 생존자 우승, 상태 `FINISHED`, 우승자가 생존 목록에 존재 |

### `large.chat`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `ChatScenarioTest` | 게임 중 채팅 전송, 모든 플레이어 수신, 게임 진행에 영향 없음, 채팅 후 정상 게임 속행 |

### `large.integration`

| 테스트 클래스 | 검증 항목 |
|---------------|-----------|
| `EndToEndScenarioTest` | 방 생성 → 입장 → 게임 시작 → 끝말잇기 → 채팅 → 탈락 → 우승 → 방 삭제 전체 흐름 |

---

## 패키지 구조

```
src/
├── main/kotlin/
│   ├── small/
│   │   ├── game/          WordValidator, TurnManager, UsedWordManager,
│   │   │                  TimerManager, HistoryManager, ResultManager
│   │   ├── avatar/        Avatar
│   │   ├── chat/          ChatMessage
│   │   ├── nickname/      NicknameGenerator
│   │   └── room/          RoomCodeGenerator
│   ├── medium/
│   │   ├── room/          RoomService, Room, Player
│   │   ├── game/          GameService, HistoryService
│   │   ├── dictionary/    DictionaryService
│   │   ├── redis/         RedisRoomRepository
│   │   └── websocket/     ChatWebSocketHandler, GameWebSocketHandler
│   └── large/
│       ├── room/          RoomService, Room, Player, Timer, Avatar
│       ├── game/          GameService, RoomService, GameStatus
│       ├── chat/          ChatService, GameService
│       └── integration/   RoomService, GameService, ChatService
└── test/kotlin/
    └── (패키지 구조 동일)
```
