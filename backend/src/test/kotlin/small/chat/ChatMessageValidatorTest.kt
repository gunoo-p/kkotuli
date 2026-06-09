package small.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatMessageValidatorTest {

    @Test
    fun `정상적인 채팅 메시지를 생성할 수 있다`() {

        // given
        val message = "안녕하세요"

        // when
        val chatMessage = ChatMessage(message)

        // then
        assertEquals(
            "안녕하세요",
            chatMessage.content
        )

    }

    @Test
    fun `빈 문자열이면 예외가 발생한다`() {

        assertFailsWith<InvalidChatMessageException> {

            ChatMessage(
                ""
            )

        }

    }

    @Test
    fun `공백만 입력하면 예외가 발생한다`() {

        assertFailsWith<InvalidChatMessageException> {

            ChatMessage(
                "      "
            )

        }

    }

    @Test
    fun `100자를 초과하면 예외가 발생한다`() {

        // given
        val longMessage = "a".repeat(101)

        assertFailsWith<InvalidChatMessageException> {

            ChatMessage(
                longMessage
            )

        }

    }

}