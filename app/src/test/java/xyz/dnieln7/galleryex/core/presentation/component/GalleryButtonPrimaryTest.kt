package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryButtonPrimaryTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN a label WHEN GalleryButtonPrimary is rendered THEN the label is displayed`() {
        composeTestRule.setGalleryExplorerContent {
            GalleryButtonPrimary(
                modifier = Modifier.testTag(BUTTON_TAG),
                text = BUTTON_TEXT,
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText(BUTTON_TEXT).assertIsDisplayed()
    }

    @Test
    fun `GIVEN an enabled GalleryButtonPrimary WHEN the user clicks it THEN onClick is invoked`() {
        var clickCount = 0

        composeTestRule.setGalleryExplorerContent {
            GalleryButtonPrimary(
                modifier = Modifier.testTag(BUTTON_TAG),
                text = BUTTON_TEXT,
                onClick = {
                    clickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithTag(BUTTON_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }

    @Test
    fun `GIVEN a disabled GalleryButtonPrimary WHEN the user taps it THEN onClick is not invoked`() {
        var clickCount = 0

        composeTestRule.setGalleryExplorerContent {
            GalleryButtonPrimary(
                modifier = Modifier.testTag(BUTTON_TAG),
                enabled = false,
                text = BUTTON_TEXT,
                onClick = {
                    clickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithTag(BUTTON_TAG).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(BUTTON_TAG).performTouchInput {
            click()
        }

        composeTestRule.runOnIdle {
            assertEquals(0, clickCount)
        }
    }
}

private const val BUTTON_TAG = "gallery_button_primary"
private const val BUTTON_TEXT = "Open gallery"
