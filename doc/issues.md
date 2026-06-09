# 이슈별 설계 내용

## 목차

- [DB 구축 (Redis)](#db-구축-redis)
- [유저 매칭 시스템 구현](#유저-매칭-시스템-구현)
- [실시간 게임 통신](#실시간-게임-통신)
- [끝말잇기 로직](#끝말잇기-로직)
- [단어사전 API 연결](#단어사전-api-연결)
- [게임방 채팅](#게임방-채팅)
- [게임 완료 후 결과창](#게임-완료-후-결과창)
- [게임 호스트 퇴장 시 방 즉시 소멸](#게임-호스트-퇴장-시-방-즉시-소멸)
- [캐릭터 커스터마이징 및 랜덤 닉네임](#캐릭터-커스터마이징-및-랜덤-닉네임)

---

## DB 구축 (Redis)

**이슈 배경**

이 서비스는 회원 정보나 게임 이력을 영구 저장할 필요가 없다.
방은 게임이 끝나면 사라지고, 유저는 닉네임만 쓰는 비회원 구조다.
영구 저장소(MySQL) 없이 Redis 하나로 전체 게임 상태를 관리한다.

**기술 선택: Redis를 유일한 저장소로 사용**

- 게임 진행 중 단어 검증과 턴 상태는 매 초 단위로 읽고 써야 한다
- 인메모리 구조로 밀리초 단위 처리 가능
- TTL(자동 만료) 설정으로 게임 종료 후 별도 삭제 로직 없이 자동 정리

**Redis 자료구조 설계**

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

## 유저 매칭 시스템 구현

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

## 실시간 게임 통신

**이슈 배경**

끝말잇기는 상대방의 단어 제출 결과, 턴 전환, 타이머를 모든 참가자가 동시에 받아야 한다.
HTTP 요청-응답 구조로는 서버가 먼저 데이터를 보내는 것이 불가능하다.

**기술 선택: WebSocket + STOMP**

- **WebSocket**: 서버가 클라이언트에 직접 push 가능한 양방향 지속 연결
- **STOMP**: 순수 WebSocket 위에 pub/sub 프로토콜을 얹어 방마다 독립 채널 구성

**토픽 구조 설계**

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
WORD_RESULT        → 단어 제출 결과 (성공/실패, 다음 턴 정보)
TURN_TIMEOUT       → 시간 초과로 인한 플레이어 탈락
GAME_OVER          → 게임 종료 및 결과
ROOM_CLOSED        → 방 강제 종료 (호스트 퇴장)
PLAYER_JOINED      → 플레이어 입장 알림
PLAYER_LEFT        → 플레이어 퇴장 알림
PLAYER_UPDATED     → 닉네임 · 표정 · 색상 변경 (전체 브로드캐스트)
PLAYER_UPDATE_ERROR→ 커스터마이징 유효성 검사 실패 (요청자 개별 전송)
```

**동시성 처리: Java 21 가상 쓰레드**

```yaml
# application.yml - 설정 한 줄로 적용
spring:
  threads:
    virtual:
      enabled: true
```

---

## 끝말잇기 로직

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

---

## 단어사전 API 연결

**이슈 배경**

제출된 단어가 실제 존재하는 한국어 단어인지 검증이 필요하다.
한국어 단어 전체를 직접 DB에 적재하면 수십만 건 데이터 관리가 필요하다.

**Redis 캐싱 전략**

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

**참고**

- [국립국어원 표준국어대사전 Open API](https://kli.korean.go.kr/term/bbs/indexOpenApiInfo.do)

---

## 게임방 채팅

**설계 결정: 게임 이벤트와 채팅 토픽 분리**

- 게임 이벤트: `/topic/room/{roomCode}`
- 채팅: `/topic/room/{roomCode}/chat`

**채팅 이력 관리: Redis List**

```
LPUSH room:{roomId}:chat "{닉네임}:{메시지}:{timestamp}"
LTRIM room:{roomId}:chat 0 99   → 최근 100개만 유지, 초과분 자동 삭제
```

---

## 게임 완료 후 결과창

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

---

## 게임 호스트 퇴장 시 방 즉시 소멸

**설계 결정: 방장 퇴장 = 방 즉시 소멸**

예외 케이스를 줄이기 위해 단순한 규칙을 적용했다.
방장 위임 같은 복잡한 처리 없이 방장 퇴장 시 방 전체를 즉시 종료한다.

**퇴장 감지: `SessionDisconnectEvent`**

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

---

## 캐릭터 커스터마이징 및 랜덤 닉네임


**이슈 배경**

로그인 없는 비회원 구조에서 플레이어를 시각적으로 구별할 수단이 필요하다.

**커스터마이징 구성 요소**

| 요소 | 선택지 | 저장 위치 |
|------|--------|-----------|
| 표정 | 픽셀 아트 얼굴 8종 | Redis (플레이어 정보) |
| 색상 | 8가지 캐릭터 배경색 | Redis (플레이어 정보) |
| 닉네임 | 랜덤 배정 + 직접 수정 가능 | Redis (플레이어 정보) |

**닉네임 입력 방식**

```
랜덤 배정 (기본값)
형용사 30개 × 동물명 30개 → 최대 900가지 조합
예: "용감한 고양이", "졸린 판다", "빠른 수달"
```

**Redis 플레이어 정보 구조**

```
room:{roomId}:players (Hash)
    {sessionId} → JSON 직렬화
    {
      "nickname": "용감한 고양이",
      "expression": 3,        ← 표정 인덱스 (0~7)
      "color": "#7F77DD"      ← 캐릭터 배경색 HEX
    }
```

**커스터마이징 API**

```
[REST] 방 참여 시 초기 배정
POST /api/rooms/join
Response: { "roomCode", "sessionId", "nickname", "expression", "color" }

[WebSocket] 닉네임·표정·색상 수정 (대기실)
SEND /app/player/update
→ BROADCAST: PLAYER_UPDATED
```
