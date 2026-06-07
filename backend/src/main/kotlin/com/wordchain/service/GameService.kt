package com.wordchain.service

import com.wordchain.model.*
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GameService(
    private val repository: GameRoomRepository,
    private val dictionaryService: DictionaryService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(GameService::class.java)

    // ─── 방 관리 ───────────────────────────────────────────────────────────────

    fun createRoom(nickname: String): RoomResponse {
        val sessionId = UUID.randomUUID().toString()
        val roomCode = generateRoomCode()

        val host = Player(sessionId = sessionId, nickname = nickname, isHost = true)
        val room = GameRoom(
            roomCode = roomCode,
            hostSessionId = sessionId,
            players = mutableListOf(host)
        )

        repository.save(room)
        repository.saveSessionRoom(sessionId, roomCode)

        log.info("Room created: $roomCode by $nickname ($sessionId)")
        return RoomResponse(roomCode, sessionId, room.players, room.status)
    }

    fun joinRoom(roomCode: String, nickname: String): RoomResponse {
        val room = repository.findByRoomCode(roomCode)
            ?: throw IllegalArgumentException("존재하지 않는 방입니다: $roomCode")

        if (room.status != GameStatus.WAITING)
            throw IllegalStateException("이미 게임이 시작된 방입니다.")

        if (room.players.size >= 8)
            throw IllegalStateException("방이 꽉 찼습니다.")

        if (room.players.any { it.nickname == nickname })
            throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")

        val sessionId = UUID.randomUUID().toString()
        val player = Player(sessionId = sessionId, nickname = nickname)
        room.players.add(player)

        repository.save(room)
        repository.saveSessionRoom(sessionId, roomCode)

        // 다른 플레이어에게 입장 알림
        broadcast(roomCode, WsMessage(
            type = MessageType.PLAYER_JOINED,
            payload = mapOf("nickname" to nickname, "players" to room.players)
        ))

        return RoomResponse(roomCode, sessionId, room.players, room.status)
    }

    fun getRoomInfo(roomCode: String): RoomResponse {
        val room = repository.findByRoomCode(roomCode)
            ?: throw IllegalArgumentException("존재하지 않는 방입니다.")
        return RoomResponse(roomCode, room.hostSessionId, room.players, room.status)
    }

    // ─── 게임 시작 ─────────────────────────────────────────────────────────────

    fun startGame(roomCode: String, requestSessionId: String) {
        val room = repository.findByRoomCode(roomCode)
            ?: throw IllegalArgumentException("존재하지 않는 방입니다.")

        if (room.hostSessionId != requestSessionId)
            throw IllegalStateException("방장만 게임을 시작할 수 있습니다.")

        if (room.players.size < 2)
            throw IllegalStateException("최소 2명이 필요합니다.")

        room.status = GameStatus.PLAYING
        room.currentTurnSessionId = room.players.first().sessionId
        room.turnStartTime = System.currentTimeMillis()

        repository.save(room)

        val firstPlayer = room.players.first()
        broadcast(roomCode, WsMessage(
            type = MessageType.GAME_START,
            payload = mapOf(
                "firstTurn" to firstPlayer.nickname,
                "firstSessionId" to firstPlayer.sessionId,
                "players" to room.players
            )
        ))

        log.info("Game started in room $roomCode. First turn: ${firstPlayer.nickname}")
    }

    // ─── 단어 제출 검증 (4단계) ────────────────────────────────────────────────

    fun submitWord(roomCode: String, sessionId: String, word: String): WordSubmitResult {
        val room = repository.findByRoomCode(roomCode)
            ?: return WordSubmitResult.Error("존재하지 않는 방입니다.")

        if (room.status != GameStatus.PLAYING)
            return WordSubmitResult.Error("게임 진행 중이 아닙니다.")

        // [1단계] 현재 턴 세션 ID 확인 → Redis 조회
        if (room.currentTurnSessionId != sessionId)
            return WordSubmitResult.Error("지금은 당신의 차례가 아닙니다.")

        val trimmedWord = word.trim()

        // [2단계] 끝글자 일치 여부 확인 → 메모리 연산 (가장 빠름)
        val lastWord = room.lastWord
        if (lastWord != null) {
            val requiredStartChar = lastWord.last()
            if (trimmedWord.first() != requiredStartChar)
                return WordSubmitResult.Rejected("'${requiredStartChar}'(으)로 시작하는 단어를 입력하세요.")
        }

        // [3단계] 중복 단어 확인 → Redis SISMEMBER
        if (repository.isWordUsed(roomCode, trimmedWord))
            return WordSubmitResult.Rejected("이미 사용된 단어입니다: $trimmedWord")

        // [4단계] 실존 단어 확인 → 외부 API 호출 (느림, 마지막 실행)
        if (!dictionaryService.isValidWord(trimmedWord))
            return WordSubmitResult.Rejected("사전에 없는 단어입니다: $trimmedWord")

        // ─── 검증 통과: 상태 업데이트 ─────────────────────────────────────────
        repository.addUsedWord(roomCode, trimmedWord)
        room.lastWord = trimmedWord
        room.usedWords.add(trimmedWord)
        room.players.find { it.sessionId == sessionId }?.let { it.score++ }

        // 다음 턴으로 전환
        val nextTurn = advanceTurn(room)
        repository.save(room)

        val submitterNickname = room.players.find { it.sessionId == sessionId }?.nickname ?: "Unknown"

        val payload = WordAcceptedPayload(
            word = trimmedWord,
            submittedBy = submitterNickname,
            nextTurn = nextTurn
        )
        broadcast(roomCode, WsMessage(MessageType.WORD_SUBMITTED, payload))

        return WordSubmitResult.Accepted(trimmedWord, nextTurn)
    }

    // ─── 턴 타이머 만료 처리 (@Scheduled에서 호출) ────────────────────────────

    fun checkTurnTimeout(roomCode: String) {
        val room = repository.findByRoomCode(roomCode) ?: return
        if (room.status != GameStatus.PLAYING) return

        val turnStartTime = room.turnStartTime ?: return
        val elapsed = (System.currentTimeMillis() - turnStartTime) / 1000

        if (elapsed >= 30) {
            val eliminatedSessionId = room.currentTurnSessionId ?: return
            val eliminated = room.players.find { it.sessionId == eliminatedSessionId } ?: return

            eliminated.isAlive = false

            broadcast(roomCode, WsMessage(
                type = MessageType.PLAYER_ELIMINATED,
                payload = PlayerEliminatedPayload(eliminatedSessionId, eliminated.nickname, "timeout")
            ))

            if (room.alivePlayerCount <= 1) {
                endGame(room)
            } else {
                val nextTurn = advanceTurn(room)
                repository.save(room)
                broadcast(roomCode, WsMessage(MessageType.TURN_CHANGED, nextTurn))
            }
        }
    }

    // ─── 플레이어 퇴장 처리 ────────────────────────────────────────────────────

    fun handlePlayerDisconnect(sessionId: String) {
        val roomCode = repository.getRoomCodeBySession(sessionId) ?: return
        val room = repository.findByRoomCode(roomCode) ?: return

        val player = room.players.find { it.sessionId == sessionId } ?: return
        log.info("Player disconnected: ${player.nickname} from room $roomCode")

        // 방장 퇴장 → 방 즉시 종료
        if (sessionId == room.hostSessionId) {
            broadcast(roomCode, WsMessage(
                type = MessageType.HOST_LEFT,
                payload = mapOf("message" to "방장이 퇴장하여 게임방이 종료됩니다.")
            ))
            broadcast(roomCode, WsMessage(MessageType.ROOM_CLOSED))
            repository.delete(roomCode)
            log.info("Room $roomCode closed because host left.")
            return
        }

        // 일반 플레이어 퇴장
        room.players.remove(player)
        repository.deleteSession(sessionId)

        broadcast(roomCode, WsMessage(
            type = MessageType.PLAYER_LEFT,
            payload = mapOf("nickname" to player.nickname, "players" to room.players)
        ))

        // 게임 중이었고 해당 플레이어의 턴이면 다음 턴으로
        if (room.status == GameStatus.PLAYING && room.currentTurnSessionId == sessionId) {
            if (room.alivePlayerCount <= 1) {
                endGame(room)
            } else {
                val nextTurn = advanceTurn(room)
                repository.save(room)
                broadcast(roomCode, WsMessage(MessageType.TURN_CHANGED, nextTurn))
            }
        } else {
            repository.save(room)
        }
    }

    // ─── 채팅 ──────────────────────────────────────────────────────────────────

    fun sendChat(roomCode: String, sessionId: String, content: String) {
        val room = repository.findByRoomCode(roomCode) ?: return
        val player = room.players.find { it.sessionId == sessionId } ?: return

        val message = ChatMessagePayload(nickname = player.nickname, content = content)
        repository.addChatMessage(roomCode, message)

        broadcastChat(roomCode, WsMessage(MessageType.CHAT_MESSAGE, message))
    }

    fun getChatHistory(roomCode: String): List<ChatMessagePayload> =
        repository.getChatHistory(roomCode)

    // ─── 내부 헬퍼 ────────────────────────────────────────────────────────────

    private fun advanceTurn(room: GameRoom): TurnChangedPayload {
        val alivePlayers = room.players.filter { it.isAlive }
        val currentIndex = alivePlayers.indexOfFirst { it.sessionId == room.currentTurnSessionId }
        val nextIndex = (currentIndex + 1) % alivePlayers.size
        val nextPlayer = alivePlayers[nextIndex]

        room.currentTurnSessionId = nextPlayer.sessionId
        room.turnStartTime = System.currentTimeMillis()

        val requiredChar = room.lastWord?.last() ?: '가'
        return TurnChangedPayload(
            nextSessionId = nextPlayer.sessionId,
            nextNickname = nextPlayer.nickname,
            requiredStartChar = requiredChar
        )
    }

    private fun endGame(room: GameRoom) {
        room.status = GameStatus.FINISHED
        repository.save(room)

        val winner = room.players.find { it.isAlive }
        // 살아있는 플레이어(우승자) 먼저, 그 다음 단어 많이 낸 순
        val ranking = room.players.sortedWith(
            compareByDescending<Player> { it.isAlive }.thenByDescending { it.score }
        )

        broadcast(room.roomCode, WsMessage(
            type = MessageType.GAME_OVER,
            payload = GameOverPayload(
                winner = winner,
                finalRanking = ranking,
                totalRounds = room.usedWords.size
            )
        ))

        log.info("Game over in room ${room.roomCode}. Winner: ${winner?.nickname}")
    }

    private fun broadcast(roomCode: String, message: WsMessage) {
        messagingTemplate.convertAndSend("/topic/room/$roomCode/game", message)
    }

    private fun broadcastChat(roomCode: String, message: WsMessage) {
        messagingTemplate.convertAndSend("/topic/room/$roomCode/chat", message)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

// ─── 단어 제출 결과 sealed class ────────────────────────────────────────────────

sealed class WordSubmitResult {
    data class Accepted(val word: String, val nextTurn: TurnChangedPayload) : WordSubmitResult()
    data class Rejected(val reason: String) : WordSubmitResult()
    data class Error(val message: String) : WordSubmitResult()
}
