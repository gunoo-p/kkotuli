package com.wordchain.handler

import com.wordchain.model.*
import com.wordchain.service.GameService
import com.wordchain.service.WordSubmitResult
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Controller
class GameWebSocketHandler(
    private val gameService: GameService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val log = LoggerFactory.getLogger(GameWebSocketHandler::class.java)

    /**
     * 단어 제출
     * STOMP 구독: /app/game/{roomCode}/submit-word
     */
    @MessageMapping("/game/{roomCode}/submit-word")
    fun submitWord(
        @DestinationVariable roomCode: String,
        @Payload request: WordSubmitRequest
    ) {
        when (val result = gameService.submitWord(roomCode, request.sessionId, request.word)) {
            is WordSubmitResult.Rejected -> {
                // 개인에게만 거절 메시지 전송
                messagingTemplate.convertAndSendToUser(
                    request.sessionId,
                    "/queue/errors",
                    WsMessage(MessageType.WORD_REJECTED, mapOf("reason" to result.reason))
                )
            }
            is WordSubmitResult.Error -> {
                messagingTemplate.convertAndSendToUser(
                    request.sessionId,
                    "/queue/errors",
                    WsMessage(MessageType.ERROR, mapOf("message" to result.message))
                )
            }
            is WordSubmitResult.Accepted -> {
                // 성공 시 broadcast는 GameService에서 처리
                log.debug("Word accepted: ${result.word} in room $roomCode")
            }
        }
    }

    /**
     * 채팅 메시지 전송
     * STOMP 구독: /app/chat/{roomCode}/send
     */
    @MessageMapping("/chat/{roomCode}/send")
    fun sendChat(
        @DestinationVariable roomCode: String,
        @Payload request: ChatMessageRequest
    ) {
        gameService.sendChat(roomCode, request.sessionId, request.content)
    }

    /**
     * 브라우저 종료, 네트워크 단절 등 비정상 연결 종료 감지
     * Spring WebSocket SessionDisconnectEvent 활용
     */
    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        log.info("WebSocket disconnected: $sessionId")
        gameService.handlePlayerDisconnect(sessionId)
    }
}
