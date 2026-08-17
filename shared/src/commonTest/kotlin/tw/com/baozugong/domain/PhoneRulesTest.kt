package tw.com.baozugong.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneRulesTest {
    @Test
    fun fixedPrefixAndTailProduceCanonicalMobile() {
        assertEquals("0912345678", PhoneRules.canonical("12-345678"))
    }

    @Test
    fun pastedFullNumberDropsFixedPrefix() {
        assertEquals("12345678", PhoneRules.tailFromInput("0912-345678"))
    }

    @Test
    fun displayUsesRequestedFormat() {
        assertEquals("0912-345678", PhoneRules.formatStored("0912345678"))
        assertEquals("12-345678", PhoneRules.formatTail("12345678"))
    }

    @Test
    fun requiresExactlyEightEditableDigits() {
        assertFalse(PhoneRules.isCompleteTail("1234567"))
        assertTrue(PhoneRules.isCompleteTail("12345678"))
    }
}
