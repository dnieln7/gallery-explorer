package xyz.dnieln7.galleryex.feature.home.presentation.component

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.R
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryEmptyStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN GalleryEmptyState WHEN it is rendered THEN all storage access texts are displayed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setGalleryExplorerContent {
            GalleryEmptyState(
                onButtonClick = {},
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.all_files_access_title))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.all_files_access_description))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.all_files_access_description_2))
            .assertIsDisplayed()
    }

    @Test
    fun `GIVEN GalleryEmptyState WHEN it is rendered THEN the grant access button is displayed`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setGalleryExplorerContent {
            GalleryEmptyState(
                onButtonClick = {},
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.grant_access))
            .assertIsDisplayed()
    }

    @Test
    fun `GIVEN GalleryEmptyState WHEN the grant access button is clicked THEN onButtonClick is invoked`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var clickCount = 0

        composeTestRule.setGalleryExplorerContent {
            GalleryEmptyState(
                onButtonClick = {
                    clickCount += 1
                },
            )
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.grant_access))
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }
}
