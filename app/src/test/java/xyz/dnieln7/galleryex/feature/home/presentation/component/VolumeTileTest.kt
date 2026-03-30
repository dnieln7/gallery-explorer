package xyz.dnieln7.galleryex.feature.home.presentation.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import xyz.dnieln7.galleryex.core.domain.model.Volume
import xyz.dnieln7.galleryex.testutil.setGalleryExplorerContent
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VolumeTileTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN a Volume WHEN VolumeTile is rendered THEN the volume name is displayed`() {
        val volume = createVolume()

        composeTestRule.setGalleryExplorerContent {
            VolumeTile(
                volume = volume,
                onClick = {},
            )
        }

        composeTestRule.onNodeWithText(volume.name).assertIsDisplayed()
    }

    @Test
    fun `GIVEN a VolumeTile WHEN it is clicked THEN onClick is invoked`() {
        var clickCount = 0

        composeTestRule.setGalleryExplorerContent {
            VolumeTile(
                modifier = Modifier.testTag(VOLUME_TILE_TAG),
                volume = createVolume(),
                onClick = {
                    clickCount += 1
                },
            )
        }

        composeTestRule.onNodeWithTag(VOLUME_TILE_TAG).performClick()

        composeTestRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }
}

private fun createVolume(): Volume {
    return Volume(
        name = "Internal storage",
        file = File("/storage/emulated/0"),
    )
}

private const val VOLUME_TILE_TAG = "volume_tile"
