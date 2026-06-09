package small.avatar

class InvalidAvatarException(message: String) : Exception(message)

data class Avatar(val expression: Int, val color: Int) {
    init {
        if (expression < 0 || expression > 15 || color < 0 || color > 15)
            throw InvalidAvatarException("표정과 색상은 0에서 15 사이여야 합니다.")
    }
}
