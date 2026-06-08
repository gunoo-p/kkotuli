# TDD 테스트 항목

| 테스트 클래스 | 검증 항목 |
|--------------|-----------|
| `GameRoomServiceTest` | 방 코드 중복 재생성, 정원 초과 거부, 존재하지 않는 방 참여 |
| `WordValidationServiceTest` | 끝글자 일치, 중복 단어 거부, 두음법칙 처리 |
| `GameSessionServiceTest` | 턴 전환, 타임아웃 처리, 호스트 퇴장 시 방 소멸 |
| `DictionaryServiceTest` | 캐시 HIT 시 API 미호출, API 실패 시 fallback 통과 |
| `ChatServiceTest` | 100개 초과 시 오래된 메시지 자동 삭제 |
| `GameResultServiceTest` | 생존 턴 수 집계, 동점자 처리, 종료 후 Redis 정리 |
| `NicknameGeneratorTest` | 같은 방 내 중복 재생성, 5회 재시도 후 예외 처리 |
| `PlayerCustomizationServiceTest` | 닉네임 8자 초과·특수문자·공백·빈값·게임 중 수정 거부, 중복 시 숫자 접미사 자동 부여, 최종 닉네임 응답 반환 확인 |
