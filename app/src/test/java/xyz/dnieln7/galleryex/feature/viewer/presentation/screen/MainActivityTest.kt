package xyz.dnieln7.galleryex.main.presentation.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun `GIVEN the redirect handler WHEN invoked THEN navigation happens before the toast`() {
        val events = mutableListOf<String>()

        handleExternalMediaRedirect(
            navigateHome = {
                events += "navigate"
            },
            showToast = { message ->
                events += "toast:$message"
            },
            message = "External device removed",
        )

        assertEquals(
            listOf(
                "navigate",
                "toast:External device removed",
            ),
            events,
        )
    }
}
