package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpacersTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN HorizontalSpacer WHEN it is placed in a Row THEN it offsets the next child by the requested width`() {
        composeTestRule.setGalleryExplorerContent {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(START_TAG),
                )
                HorizontalSpacer(of = 16.dp)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(END_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(END_TAG).assertLeftPositionInRootIsEqualTo(36.dp)
    }

    @Test
    fun `GIVEN VerticalSpacer WHEN it is placed in a Column THEN it offsets the next child by the requested height`() {
        composeTestRule.setGalleryExplorerContent {
            Column(modifier = Modifier.fillMaxHeight()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(START_TAG),
                )
                VerticalSpacer(of = 16.dp)
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(END_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(END_TAG).assertTopPositionInRootIsEqualTo(36.dp)
    }

    @Test
    fun `GIVEN HorizontalExpandedSpacer WHEN it is placed in a Row THEN it pushes trailing content to the end`() {
        composeTestRule.setGalleryExplorerContent {
            Row(modifier = Modifier.size(width = 100.dp, height = 20.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(START_TAG),
                )
                HorizontalExpandedSpacer()
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(END_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(END_TAG).assertLeftPositionInRootIsEqualTo(80.dp)
    }

    @Test
    fun `GIVEN VerticalExpandedSpacer WHEN it is placed in a Column THEN it pushes trailing content to the bottom`() {
        composeTestRule.setGalleryExplorerContent {
            Column(modifier = Modifier.size(width = 20.dp, height = 100.dp)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(START_TAG),
                )
                VerticalExpandedSpacer()
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .testTag(END_TAG),
                )
            }
        }

        composeTestRule.onNodeWithTag(END_TAG).assertTopPositionInRootIsEqualTo(80.dp)
    }
}

private const val START_TAG = "start_box"
private const val END_TAG = "end_box"
