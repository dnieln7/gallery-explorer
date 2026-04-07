package xyz.dnieln7.galleryex.feature.explorer.presentation.screen

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import xyz.dnieln7.galleryex.core.domain.preferences.AppPreferences
import xyz.dnieln7.galleryex.core.domain.enums.SortOrder
import xyz.dnieln7.galleryex.core.domain.enums.SortType
import xyz.dnieln7.galleryex.feature.explorer.domain.model.ExplorerAction
import xyz.dnieln7.galleryex.testutil.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val appPreferences = FakeAppPreferences()
    private val viewModel by lazy { ExplorerViewModel(appPreferences) }

    @Test
    fun `GIVEN directory path WHEN LoadFiles action is dispatched THEN state indicates loading and loads files correctly sorted by default criteria`() = runTest {
        val root = temporaryFolder.newFolder("test_dir")
        createDummyFile(root, "b.jpg", 2000L)
        createDummyFile(root, "c.jpg", 1000L)
        createDummyFile(root, "a.jpg", 3000L)
        
        viewModel.uiState.test {
            val initialState = awaitItem()
            initialState.isLoading.shouldBeFalse()
            initialState.files.size.shouldBeEqualTo(0)
            initialState.sortType.shouldBeEqualTo(SortType.NAME)
            initialState.sortOrder.shouldBeEqualTo(SortOrder.ASCENDING)
            
            viewModel.onAction(ExplorerAction.LoadFiles(root.absolutePath))
            
            val loadingState = awaitItem()
            loadingState.isLoading.shouldBeTrue()
            
            val loadedState = awaitItem()
            loadedState.isLoading.shouldBeFalse()
            loadedState.files.size.shouldBeEqualTo(3)
            
            // Default sort is NAME ASCENDING
            loadedState.files[0].name.shouldBeEqualTo("a")
            loadedState.files[1].name.shouldBeEqualTo("b")
            loadedState.files[2].name.shouldBeEqualTo("c")
            
            // Ignore any other emissions (e.g. state flow buffer)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN loaded files WHEN sorting by name descending THEN files are reordered correctly`() = runTest {
        val root = temporaryFolder.newFolder("test_dir")
        createDummyFile(root, "a.jpg", 3000L)
        createDummyFile(root, "b.jpg", 2000L)
        createDummyFile(root, "c.jpg", 1000L)

        viewModel.onAction(ExplorerAction.LoadFiles(root.absolutePath))

        viewModel.uiState.test {
            // Skip up to loaded state
            var state = awaitItem()
            while (state.files.isEmpty()) {
                state = awaitItem()
            }

            // Apply sorting
            viewModel.onAction(ExplorerAction.ChangeSortOrder(SortOrder.DESCENDING))

            val sortedState = awaitItem()
            sortedState.sortOrder.shouldBeEqualTo(SortOrder.DESCENDING)
            sortedState.files[0].name.shouldBeEqualTo("c")
            sortedState.files[1].name.shouldBeEqualTo("b")
            sortedState.files[2].name.shouldBeEqualTo("a")
        }
    }

    @Test
    fun `GIVEN loaded files WHEN sorting by date ascending THEN files are reordered correctly`() = runTest {
        val root = temporaryFolder.newFolder("test_dir")
        createDummyFile(root, "b.jpg", 2000L)
        createDummyFile(root, "c.jpg", 1000L) // Oldest
        createDummyFile(root, "a.jpg", 3000L) // Newest

        viewModel.onAction(ExplorerAction.LoadFiles(root.absolutePath))

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.files.isEmpty()) {
                state = awaitItem()
            }

            // Apply sorting
            viewModel.onAction(ExplorerAction.ChangeSortType(SortType.DATE))

            val sortedState = awaitItem()
            sortedState.sortType.shouldBeEqualTo(SortType.DATE)
            sortedState.sortOrder.shouldBeEqualTo(SortOrder.ASCENDING)
            sortedState.files[0].name.shouldBeEqualTo("c")
            sortedState.files[1].name.shouldBeEqualTo("b")
            sortedState.files[2].name.shouldBeEqualTo("a")
        }
    }

    @Test
    fun `GIVEN loaded files WHEN sorting by date descending THEN files are reordered correctly`() = runTest {
        val root = temporaryFolder.newFolder("test_dir")
        createDummyFile(root, "b.jpg", 2000L)
        createDummyFile(root, "c.jpg", 1000L) // Oldest
        createDummyFile(root, "a.jpg", 3000L) // Newest

        viewModel.onAction(ExplorerAction.LoadFiles(root.absolutePath))

        // Wait for files to load first before sending both actions, otherwise StateFlow conflation might drop intermediate states
        viewModel.uiState.test {
            var state = awaitItem()
            while (state.files.isEmpty()) {
                state = awaitItem()
            }
            
            viewModel.onAction(ExplorerAction.ChangeSortType(SortType.DATE))
            awaitItem() // Skip to Date Ascending state
            
            viewModel.onAction(ExplorerAction.ChangeSortOrder(SortOrder.DESCENDING))
            val finalState = awaitItem()
            
            finalState.sortType.shouldBeEqualTo(SortType.DATE)
            finalState.sortOrder.shouldBeEqualTo(SortOrder.DESCENDING)
            finalState.files[0].name.shouldBeEqualTo("a")
            finalState.files[1].name.shouldBeEqualTo("b")
            finalState.files[2].name.shouldBeEqualTo("c")
        }
    }

    private fun createDummyFile(dir: File, name: String, lastModified: Long): File {
        return File(dir, name).apply {
            createNewFile()
            setLastModified(lastModified)
        }
    }
}

private class FakeAppPreferences : AppPreferences {
    override val sortTypeFlow = MutableStateFlow(SortType.NAME)
    override val sortOrderFlow = MutableStateFlow(SortOrder.ASCENDING)

    override suspend fun saveSortType(type: SortType) {
        sortTypeFlow.value = type
    }

    override suspend fun saveSortOrder(order: SortOrder) {
        sortOrderFlow.value = order
    }
}
