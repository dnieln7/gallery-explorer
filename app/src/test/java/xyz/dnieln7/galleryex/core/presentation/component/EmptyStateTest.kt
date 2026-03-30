package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EmptyStateTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN an EmptyState WHEN it is rendered THEN the title is displayed`() {
        composeTestRule.setGalleryExplorerContent {
            EmptyState(
                icon = Icons.Rounded.FolderOff,
                title = TITLE,
            )
        }

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    }

    @Test
    fun `GIVEN an EmptyState message WHEN it is rendered THEN the message is displayed`() {
        composeTestRule.setGalleryExplorerContent {
            EmptyState(
                icon = Icons.Rounded.FolderOff,
                title = TITLE,
                message = MESSAGE,
            )
        }

        composeTestRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }

    @Test
    fun `GIVEN a null EmptyState message WHEN it is rendered THEN the message is absent`() {
        composeTestRule.setGalleryExplorerContent {
            EmptyState(
                icon = Icons.Rounded.FolderOff,
                title = TITLE,
                message = null,
            )
        }

        composeTestRule.onAllNodesWithText(MESSAGE).assertCountEquals(0)
    }
}

private const val TITLE = "This folder is empty."
private const val MESSAGE = "There are no images or videos here."
