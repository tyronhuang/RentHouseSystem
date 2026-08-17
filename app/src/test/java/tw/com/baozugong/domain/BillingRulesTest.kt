package tw.com.baozugong.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BillingRulesTest {
    @Test fun generatesEveryMonthThroughToday() {
        assertEquals(listOf(YearMonth.of(2026,6),YearMonth.of(2026,7),YearMonth.of(2026,8)),BillingRules.monthsToGenerate("2026-06-15","2027-06-14",LocalDate.of(2026,8,17)))
    }
    @Test fun respectsContractEnd() {
        assertEquals(listOf(YearMonth.of(2026,6),YearMonth.of(2026,7)),BillingRules.monthsToGenerate("2026-06-01","2026-07-31",LocalDate.of(2026,8,17)))
    }
    @Test fun clampsDueDayForShortMonth() {
        assertEquals(LocalDate.of(2026,2,28),BillingRules.dueDate(YearMonth.of(2026,2),31))
    }
    @Test fun renewalStartsTheDayAfterPreviousLease() {
        assertEquals(LocalDate.of(2027,6,15), BillingRules.renewalStart("2027-06-14"))
    }
}
