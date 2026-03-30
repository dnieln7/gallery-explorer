package xyz.dnieln7.galleryex.core.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PullToRefreshTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN PullToRefresh content WHEN it is rendered THEN the child content is displayed`() {
        composeTestRule.setGalleryExplorerContent {
            PullToRefresh(
                onRefresh = {},
                content = {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .testTag(CONTENT_TAG),
                    )
                },
            )
        }

        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun `GIVEN PullToRefresh WHEN the user pulls down THEN onRefresh is invoked on the JVM`() {
        var refreshCount = 0

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setGalleryExplorerContent {
            Box(modifier = Modifier.size(width = 240.dp, height = 480.dp)) {
                PullToRefresh(
                    onRefresh = {
                        refreshCount += 1
                    },
                    content = {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(CONTENT_TAG),
                        ) {
                            items(items = TEST_ITEMS) { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(72.dp)
                                        .testTag(item),
                                )
                            }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithTag(CONTENT_TAG).performTouchInput {
            swipeDown()
        }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.runOnIdle {
            assertEquals(1, refreshCount)
        }

        composeTestRule.mainClock.advanceTimeBy(REFRESH_INDICATOR_DELAY_MS)
        composeTestRule.waitForIdle()
    }
}

private const val CONTENT_TAG = "pull_to_refresh_content"
private const val REFRESH_INDICATOR_DELAY_MS = 500L
private val TEST_ITEMS = List(size = 20) { index ->
    "item_$index"
}
