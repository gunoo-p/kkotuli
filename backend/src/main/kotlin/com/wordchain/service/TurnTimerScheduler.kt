package com.wordchain.service

import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class TurnTimerScheduler(
    private val gameService: GameService,
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val log = LoggerFactory.getLogger(TurnTimerScheduler::class.java)

    /**
     * 매 1초마다 실행: 모든 진행 중인 방의 턴 타임아웃 체크
     *
     * 가상 쓰레드는 I/O 대기 최적화에 초점.
     * 이 기능은 정해진 주기에 따라 실행되는 시간 기반 작업이므로
     * @Scheduled 방식이 더 적합.
     */
    @Scheduled(fixedDelay = 1000)
    fun checkAllRoomTimers() {
        try {
            val keys = redisTemplate.keys("room:*")
                ?.filter { it.isNotBlank() && !it.contains(":words") && !it.contains(":chat") }
                ?: return

            keys.forEach { key ->
                val roomCode = key.removePrefix("room:")
                gameService.checkTurnTimeout(roomCode)
            }
        } catch (e: Exception) {
            log.error("Timer scheduler error: ${e.message}")
        }
    }
}
