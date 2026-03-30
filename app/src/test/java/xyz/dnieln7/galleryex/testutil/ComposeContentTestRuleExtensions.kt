package xyz.dnieln7.galleryex.testutil

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import xyz.dnieln7.galleryex.core.presentation.theme.GalleryExplorerTheme

internal fun createJvmComposeRule(): ComposeContentTestRule {
    val factoryClass = Class.forName(COMPOSE_RULE_FACTORY_CLASS_NAME)
    val factoryMethod = factoryClass.getMethod(CREATE_COMPOSE_RULE_METHOD_NAME)

    return factoryMethod.invoke(null) as ComposeContentTestRule
}

internal fun ComposeContentTestRule.setGalleryExplorerContent(content: @Composable () -> Unit) {
    setContent {
        GalleryExplorerTheme(
            darkTheme = false,
            dynamicColor = false,
        ) {
            content()
        }
    }
}

private const val COMPOSE_RULE_FACTORY_CLASS_NAME =
    "androidx.compose.ui.test.junit4.AndroidComposeTestRule_androidKt"
private const val CREATE_COMPOSE_RULE_METHOD_NAME = "createComposeRule"
