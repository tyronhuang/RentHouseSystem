package tw.com.baozugong.domain

object BillingRules {
    fun monthsToGenerate(startDate: String, endDate: String, throughDate: String): List<String> {
        val start = Month.parse(startDate)
        val contractualEnd = Month.parse(endDate)
        val end = minOf(contractualEnd, Month.parse(throughDate))
        if (start > end) return emptyList()

        return buildList {
            var current = start
            while (current <= end) {
                add(current.toString())
                current = current.next()
            }
        }
    }

    fun dueDate(billingMonth: String, dueDay: Int): String {
        val month = Month.parse(billingMonth)
        val day = dueDay.coerceIn(1, month.lengthInDays())
        return "${month}-$day".replace(Regex("-(\\d)$"), "-0$1")
    }

    fun renewalStart(previousEndDate: String): String {
        val parts = previousEndDate.split('-').map(String::toInt)
        require(parts.size == 3) { "日期格式必須為 yyyy-MM-dd" }
        var year = parts[0]
        var month = parts[1]
        var day = parts[2] + 1
        val daysInMonth = Month(year, month).lengthInDays()
        if (day > daysInMonth) {
            day = 1
            month += 1
            if (month > 12) {
                month = 1
                year += 1
            }
        }
        return "%04d-%02d-%02d".formatPortable(year, month, day)
    }

    private data class Month(val year: Int, val month: Int) : Comparable<Month> {
        init {
            require(month in 1..12) { "月份必須介於 1 到 12" }
        }

        override fun compareTo(other: Month): Int =
            compareValuesBy(this, other, Month::year, Month::month)

        fun next(): Month = if (month == 12) Month(year + 1, 1) else Month(year, month + 1)

        fun lengthInDays(): Int = when (month) {
            2 -> if (year.isLeapYear()) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        override fun toString(): String = "%04d-%02d".formatPortable(year, month)

        companion object {
            fun parse(dateOrMonth: String): Month {
                val parts = dateOrMonth.split('-')
                require(parts.size >= 2) { "日期格式必須為 yyyy-MM 或 yyyy-MM-dd" }
                return Month(parts[0].toInt(), parts[1].toInt())
            }
        }
    }

    private fun Int.isLeapYear(): Boolean = this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)

    private fun String.formatPortable(vararg values: Int): String {
        var index = 0
        return replace(Regex("%0(\\d)d")) { match ->
            values[index++].toString().padStart(match.groupValues[1].toInt(), '0')
        }
    }
}
