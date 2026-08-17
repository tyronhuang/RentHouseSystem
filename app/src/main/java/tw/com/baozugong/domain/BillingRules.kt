package tw.com.baozugong.domain

import java.time.LocalDate
import java.time.YearMonth

object BillingRules {
    fun monthsToGenerate(startDate: String, endDate: String, through: LocalDate): List<YearMonth> {
        val start = YearMonth.from(LocalDate.parse(startDate))
        val contractualEnd = YearMonth.from(LocalDate.parse(endDate))
        val end = minOf(contractualEnd, YearMonth.from(through))
        if (start > end) return emptyList()
        return generateSequence(start) { it.plusMonths(1) }.takeWhile { it <= end }.toList()
    }

    fun dueDate(month: YearMonth, dueDay: Int): LocalDate = month.atDay(dueDay.coerceIn(1, month.lengthOfMonth()))
}
