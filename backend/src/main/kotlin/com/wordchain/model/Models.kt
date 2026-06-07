package com.wordchain.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonValue

// ─── 게임 상태 ────────────────────────────────────────────────────────────────

enum class GameStatus {
    WAITING,    // 게임 대기 중 (방 생성 후)
    PLAYING,    // 게임 진행 중
    FINISHED    // 게임 종료
}

// ─── 플레이어 ─────────────────────────────────────────────────────────────────

data class Player(
    val sessionId: String,          // 서버 발급 UUID
    val nickname: String,
    val isHost: Boolean = false,
    var isAlive: Boolean = true,    // 탈락 여부
    var score: Int = 0
)

// ─── 게임 방 ──────────────────────────────────────────────────────────────────

@JsonIgnoreProperties(ignoreUnknown = true)
data class GameRoom(
    val roomCode: String,
    val hostSessionId: String,
    val players: MutableList<Player> = mutableListOf(),
    var status: GameStatus = GameStatus.WAITING,
    var currentTurnSessionId: String? = null,
    var lastWord: String? = null,           // 마지막으로 제출된 단어
    var usedWords: MutableSet<String> = mutableSetOf(),  // 사용된 단어 집합
    var turnStartTime: Long? = null,        // 현재 턴 시작 시각 (epoch ms)
    val createdAt: Long = System.currentTimeMillis()
) {
    val alivePlayerCount: Int get() = players.count { it.isAlive }
    val currentPlayer: Player? get() = players.find { it.sessionId == currentTurnSessionId && it.isAlive }
}

// ─── WebSocket 메시지 타입 ─────────────────────────────────────────────────────

enum class MessageType {
    // 게임 이벤트
    GAME_START,
    WORD_SUBMITTED,
    WORD_REJECTED,
    TURN_CHANGED,
    TIMER_TICK,
    PLAYER_ELIMINATED,
    GAME_OVER,

    // 채팅
    CHAT_MESSAGE,
    CHAT_HISTORY,

    // 방 이벤트
    PLAYER_JOINED,
    PLAYER_LEFT,
    HOST_LEFT,
    ROOM_CLOSED,

    // 에러
    ERROR
}

// ─── WebSocket 페이로드 ───────────────────────────────────────────────────────

data class WsMessage(
    val type: MessageType,
    val payload: Any? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class WordSubmitRequest(
    val word: String,
    val sessionId: String
)

data class ChatMessageRequest(
    val content: String,
    val sessionId: String
)

data class ChatMessagePayload(
    val nickname: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class TurnChangedPayload(
    val nextSessionId: String,
    val nextNickname: String,
    val requiredStartChar: Char,   // 이어야 할 첫 글자
    val remainingSeconds: Int = 30
)

data class WordAcceptedPayload(
    val word: String,
    val submittedBy: String,       // 닉네임
    val nextTurn: TurnChangedPayload
)

data class PlayerEliminatedPayload(
    val sessionId: String,
    val nickname: String,
    val reason: String             // "timeout" | "invalid_word"
)

data class GameOverPayload(
    val winner: Player?,
    val finalRanking: List<Player>,
    val totalRounds: Int
)

// ─── REST 요청/응답 ────────────────────────────────────────────────────────────

data class CreateRoomRequest(
    val nickname: String
)

data class JoinRoomRequest(
    val nickname: String,
    val roomCode: String
)

data class RoomResponse(
    val roomCode: String,
    val sessionId: String,
    val players: List<Player>,
    val status: GameStatus
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)
