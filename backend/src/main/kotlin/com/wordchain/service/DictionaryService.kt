package com.wordchain.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class DictionaryService(
    private val repository: GameRoomRepository,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val log = LoggerFactory.getLogger(DictionaryService::class.java)

    @Value("\${word-chain.dictionary-api.key}")
    private lateinit var apiKey: String

    @Value("\${word-chain.dictionary-api.url}")
    private lateinit var apiUrl: String

    /**
     * 단어 유효성 검증
     * 1) Redis 캐시 조회
     * 2) 캐시 미스 → 국립국어원 API 호출
     * 3) API 장애 → Fallback(통과 처리)
     */
    fun isValidWord(word: String): Boolean {
        // 캐시 확인
        repository.getCachedWordValidity(word)?.let {
            log.debug("Cache hit for word: $word → $it")
            return it
        }

        // 외부 API 호출
        return try {
            val result = callDictionaryApi(word)
            repository.cacheWordValidity(word, result)
            result
        } catch (e: Exception) {
            log.warn("Dictionary API error for word '$word': ${e.message}. Applying fallback (pass).")
            // Fallback: 외부 API 장애 시 통과 처리 (서비스 가용성 확보)
            true
        }
    }

    private fun callDictionaryApi(word: String): Boolean {
        val uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
            .queryParam("key", apiKey)
            .queryParam("q", word)
            .queryParam("req_type", "json")
            .queryParam("part", "word")
            .queryParam("sort", "popular")
            .queryParam("num", "1")
            .build()
            .toUri()

        val response = restTemplate.getForObject(uri, Map::class.java) ?: return false

        // 국립국어원 API 응답: channel.total > 0 이면 실존 단어
        @Suppress("UNCHECKED_CAST")
        val channel = (response["channel"] as? Map<String, Any>) ?: return false
        val total = channel["total"]?.toString()?.toIntOrNull() ?: 0
        return total > 0
    }
}
