package tw.com.baozugong.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class BillingRulesTest {
    @Test
    fun generatesEveryMonthThroughToday() {
        assertEquals(
            listOf("2026-06", "2026-07", "2026-08"),
            BillingRules.monthsToGenerate("2026-06-15", "2027-06-14", "2026-08-17"),
        )
    }

    @Test
    fun respectsContractEnd() {
        assertEquals(
            listOf("2026-06", "2026-07"),
            BillingRules.monthsToGenerate("2026-06-01", "2026-07-31", "2026-08-17"),
        )
    }

    @Test
    fun clampsDueDayForShortMonth() {
        assertEquals("2026-02-28", BillingRules.dueDate("2026-02", 31))
        assertEquals("2028-02-29", BillingRules.dueDate("2028-02", 31))
    }

    @Test
    fun renewalStartsTheDayAfterPreviousLease() {
        assertEquals("2027-06-15", BillingRules.renewalStart("2027-06-14"))
        assertEquals("2028-01-01", BillingRules.renewalStart("2027-12-31"))
    }
}
