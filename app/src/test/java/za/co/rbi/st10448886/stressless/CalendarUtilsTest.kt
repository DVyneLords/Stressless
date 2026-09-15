package za.co.rbi.st10448886.stressless

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarUtilsTest {

    @Test
    fun isSameDay_trueForSameDate() {
        val cal = Calendar.getInstance()
        val millis = cal.timeInMillis
        assertTrue(isSameDay(millis, cal))
    }

    @Test
    fun isSameDay_falseForDifferentDate() {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertFalse(isSameDay(tomorrow.timeInMillis, today))
    }
}