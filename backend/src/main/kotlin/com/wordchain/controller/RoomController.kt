package com.wordchain.controller

import com.wordchain.model.*
import com.wordchain.service.GameService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = ["*"])
class RoomController(private val gameService: GameService) {

    /** 방 생성 */
    @PostMapping
    fun createRoom(@RequestBody req: CreateRoomRequest): ResponseEntity<ApiResponse<RoomResponse>> {
        return try {
            val room = gameService.createRoom(req.nickname)
            ResponseEntity.ok(ApiResponse(success = true, data = room))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, error = e.message))
        }
    }

    /** 방 참가 */
    @PostMapping("/{roomCode}/join")
    fun joinRoom(
        @PathVariable roomCode: String,
        @RequestBody req: JoinRoomRequest
    ): ResponseEntity<ApiResponse<RoomResponse>> {
        return try {
            val room = gameService.joinRoom(roomCode, req.nickname)
            ResponseEntity.ok(ApiResponse(success = true, data = room))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, error = e.message))
        }
    }

    /** 방 조회 */
    @GetMapping("/{roomCode}")
    fun getRoom(@PathVariable roomCode: String): ResponseEntity<ApiResponse<RoomResponse>> {
        return try {
            val room = gameService.getRoomInfo(roomCode)
            ResponseEntity.ok(ApiResponse(success = true, data = room))
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /** 게임 시작 (방장만) */
    @PostMapping("/{roomCode}/start")
    fun startGame(
        @PathVariable roomCode: String,
        @RequestHeader("X-Session-Id") sessionId: String
    ): ResponseEntity<ApiResponse<Unit>> {
        return try {
            gameService.startGame(roomCode, sessionId)
            ResponseEntity.ok(ApiResponse(success = true))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(ApiResponse(success = false, error = e.message))
        }
    }

    /** 채팅 히스토리 조회 */
    @GetMapping("/{roomCode}/chat")
    fun getChatHistory(@PathVariable roomCode: String): ResponseEntity<ApiResponse<List<Any>>> {
        val history = gameService.getChatHistory(roomCode)
        return ResponseEntity.ok(ApiResponse(success = true, data = history))
    }
}
