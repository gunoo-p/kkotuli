package com.wordchain.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.wordchain.model.GameRoom
import com.wordchain.model.ChatMessagePayload
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.concurrent.TimeUnit

@Repository
class GameRoomRepository(
    private val redisTemplate: RedisTemplate<String, Any>
) {
    private val mapper = ObjectMapper().registerKotlinModule()

    // ─── 키 상수 ───────────────────────────────────────────────────────────────
    private fun roomKey(roomCode: String) = "room:$roomCode"
    private fun usedWordsKey(roomCode: String) = "room:$roomCode:words"
    private fun chatKey(roomCode: String) = "room:$roomCode:chat"
    private fun sessionKey(sessionId: String) = "session:$sessionId"

    // ─── 방 CRUD ──────────────────────────────────────────────────────────────

    fun save(room: GameRoom, ttlHours: Long = 2) {
        val json = mapper.writeValueAsString(room)
        redisTemplate.opsForValue().set(roomKey(room.roomCode), json, ttlHours, TimeUnit.HOURS)
    }

    fun findByRoomCode(roomCode: String): GameRoom? {
        val json = redisTemplate.opsForValue().get(roomKey(roomCode)) as? String ?: return null
        return mapper.readValue(json, GameRoom::class.java)
    }

    fun delete(roomCode: String) {
        redisTemplate.delete(roomKey(roomCode))
        redisTemplate.delete(usedWordsKey(roomCode))
        redisTemplate.delete(chatKey(roomCode))
    }

    fun exists(roomCode: String): Boolean =
        redisTemplate.hasKey(roomKey(roomCode))

    // ─── 사용된 단어 집합 (SISMEMBER 활용) ────────────────────────────────────

    fun addUsedWord(roomCode: String, word: String) {
        redisTemplate.opsForSet().add(usedWordsKey(roomCode), word)
        // 방 TTL과 동일하게 유지
        redisTemplate.expire(usedWordsKey(roomCode), 2, TimeUnit.HOURS)
    }

    fun isWordUsed(roomCode: String, word: String): Boolean =
        redisTemplate.opsForSet().isMember(usedWordsKey(roomCode), word) == true

    // ─── 채팅 히스토리 (Redis List, 최근 100개) ────────────────────────────────

    fun addChatMessage(roomCode: String, message: ChatMessagePayload, maxSize: Long = 100) {
        val key = chatKey(roomCode)
        val json = mapper.writeValueAsString(message)
        redisTemplate.opsForList().rightPush(key, json)
        // 최근 100개만 유지
        redisTemplate.opsForList().trim(key, -maxSize, -1)
        redisTemplate.expire(key, 2, TimeUnit.HOURS)
    }

    fun getChatHistory(roomCode: String): List<ChatMessagePayload> {
        val key = chatKey(roomCode)
        val items = redisTemplate.opsForList().range(key, 0, -1) ?: return emptyList()
        return items.mapNotNull { item ->
            (item as? String)?.let { mapper.readValue(it, ChatMessagePayload::class.java) }
        }
    }

    // ─── 세션 → 방 코드 매핑 ──────────────────────────────────────────────────

    fun saveSessionRoom(sessionId: String, roomCode: String) {
        redisTemplate.opsForValue().set(sessionKey(sessionId), roomCode, 2, TimeUnit.HOURS)
    }

    fun getRoomCodeBySession(sessionId: String): String? =
        redisTemplate.opsForValue().get(sessionKey(sessionId)) as? String

    fun deleteSession(sessionId: String) {
        redisTemplate.delete(sessionKey(sessionId))
    }

    // ─── 단어 검증 캐시 (24시간 TTL) ──────────────────────────────────────────

    fun cacheWordValidity(word: String, isValid: Boolean) {
        redisTemplate.opsForValue().set("dict:$word", isValid.toString(), 24, TimeUnit.HOURS)
    }

    fun getCachedWordValidity(word: String): Boolean? {
        val cached = redisTemplate.opsForValue().get("dict:$word") as? String ?: return null
        return cached.toBoolean()
    }
}
