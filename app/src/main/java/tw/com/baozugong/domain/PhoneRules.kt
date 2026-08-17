package tw.com.baozugong.domain

object PhoneRules {
    fun tailFromInput(input: String): String {
        val digits = input.filter(Char::isDigit)
        return (if (digits.startsWith("09") && digits.length > 8) digits.drop(2) else digits).take(8)
    }

    fun isCompleteTail(tail: String): Boolean = tail.filter(Char::isDigit).length == 8

    fun canonical(tail: String): String {
        val digits = tail.filter(Char::isDigit)
        require(digits.length == 8) { "手機號碼必須為 10 碼" }
        return "09$digits"
    }

    fun formatTail(tail: String): String {
        val digits = tail.filter(Char::isDigit).take(8)
        return digits.take(2) + if (digits.length > 2) "-${digits.drop(2)}" else ""
    }

    fun tailFromStored(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        return when {
            digits.startsWith("09") -> digits.drop(2).take(8)
            else -> digits.take(8)
        }
    }

    fun formatStored(phone: String): String {
        val digits = phone.filter(Char::isDigit)
        return if (digits.length == 10 && digits.startsWith("09")) "${digits.take(4)}-${digits.drop(4)}" else phone
    }
}
