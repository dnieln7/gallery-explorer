@file:OptIn(ExperimentalCoroutinesApi::class)

package xyz.dnieln7.galleryex.feature.example.presentation.screen

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import io.mockk.coEvery
import xyz.dnieln7.galleryex.core.domain.repository.ExampleRepository
import xyz.dnieln7.galleryex.core.presentation.text.UIText
import xyz.dnieln7.galleryex.feature.example.domain.error.ExampleError
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleAction
import xyz.dnieln7.galleryex.feature.example.domain.model.ExampleEvent
import xyz.dnieln7.galleryex.testutil.coVerifyOnce
import xyz.dnieln7.galleryex.testutil.relaxedMockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.junit.Test

class ExampleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val exampleRepository = relaxedMockk<ExampleRepository>()

    private lateinit var viewModel: ExampleViewModel

    @Test
    fun `GIVEN the happy path WHEN OnRefresh is called in a new viewmodel THEN emit the expected data list`() {
        val data = listOf("DATA_1", "DATA_2", "DATA_3")

        coEvery { exampleRepository.getData() } returns data.right()

        runTest(dispatcher) {
            // OnRefresh is automatically called in the init block
            viewModel = ExampleViewModel(dispatcher, exampleRepository)

            viewModel.uiState.test {
                // First emission before getData
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldBeEmpty()
                }

                // Second emission while getData is loading
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeTrue()
                    stateSnapshot.data.shouldBeEmpty()
                }

                // Third emission with the expected data
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldContainSame(data)
                }
            }
        }
    }

    @Test
    fun `GIVEN an error from exampleRepository_getData WHEN OnRefresh is called in a new viewmodel THEN emit an error`() {
        coEvery {
            exampleRepository.getData()
        } returns ExampleError.Other("database is closed").left()

        runTest(dispatcher) {
            // OnRefresh is automatically called in the init block
            viewModel = ExampleViewModel(dispatcher, exampleRepository)

            viewModel.uiState.test {
                // First emission before getData
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldBeEmpty()
                }

                // Second emission while getData is loading
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeTrue()
                    stateSnapshot.data.shouldBeEmpty()
                }

                // Third emission without data
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldBeEmpty()
                }
            }

            viewModel.events.test {
                awaitItem().let { eventSnapshot ->
                    eventSnapshot.shouldBeInstanceOf<ExampleEvent.OnError>()
                    (eventSnapshot as ExampleEvent.OnError).message.shouldBeInstanceOf<UIText.FromString>()
                }
            }
        }
    }

    @Test
    fun `GIVEN the happy path WHEN OnSubmitClick THEN emit the expected state`() {
        val data = listOf("DATA_1", "DATA_2", "DATA_3")
        val input = "DATA_4"

        coEvery { exampleRepository.getData() } returns data.right()
        coEvery { exampleRepository.submitData(any()) } returns Unit.right()

        runTest(dispatcher) {
            viewModel = ExampleViewModel(dispatcher, exampleRepository)

            viewModel.onAction(ExampleAction.OnInputChanged(input))
            viewModel.onAction(ExampleAction.OnSubmitClick)

            viewModel.uiState.test {
                // Skip the init block emissions
                skipItems(3)

                // First emission while submitData is loading
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeTrue()
                    stateSnapshot.data.shouldContainSame(data)
                }

                // Second emission after submitData
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldContainSame(data)
                }
            }

            viewModel.events.test {
                awaitItem().shouldBe(ExampleEvent.OnDataSubmitted)
            }

            // Verify the expected calls
            coVerifyOnce {
                exampleRepository.submitData(input)
            }
        }
    }

    @Test
    fun `GIVEN an error from exampleRepository_submitData WHEN OnSubmitClick THEN emit an error`() {
        val data = listOf("DATA_1", "DATA_2", "DATA_3")
        val input = "DATA_4"

        coEvery { exampleRepository.getData() } returns data.right()
        coEvery {
            exampleRepository.submitData(any())
        } returns ExampleError.EmptyInput.left()

        runTest(dispatcher) {
            viewModel = ExampleViewModel(dispatcher, exampleRepository)

            viewModel.onAction(ExampleAction.OnInputChanged(input))
            viewModel.onAction(ExampleAction.OnSubmitClick)

            viewModel.uiState.test {
                // Skip the init block emissions
                skipItems(3)

                // First emission while submitData is loading
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeTrue()
                    stateSnapshot.data.shouldContainSame(data)
                }

                // Second emission after submitData
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeFalse()
                    stateSnapshot.data.shouldContainSame(data)
                }
            }

            viewModel.events.test {
                awaitItem().let { eventSnapshot ->
                    eventSnapshot.shouldBeInstanceOf<ExampleEvent.OnError>()
                    (eventSnapshot as ExampleEvent.OnError).message.shouldBeInstanceOf<UIText.FromResource>()
                }
            }
        }
    }

    @Test
    fun `GIVEN the happy path WHEN OnRefresh is called after OnSubmitClick THEN emit a new data list`() {
        val data = listOf("DATA_1", "DATA_2", "DATA_3")

        val input = "DATA_4"
        var inputAdded = false
        val newData = listOf("DATA_1", "DATA_2", "DATA_3", "DATA_4")

        coEvery { exampleRepository.getData() } coAnswers {
            if (inputAdded) {
                newData.right()
            } else {
                data.right()
            }
        }
        coEvery { exampleRepository.submitData(any()) } coAnswers {
            inputAdded = true
            Unit.right()
        }

        runTest(dispatcher) {
            viewModel = ExampleViewModel(dispatcher, exampleRepository)

            // Simulate the screen scope that will collect the events
            backgroundScope.launch {
                viewModel.events.collect {
                    if (it is ExampleEvent.OnDataSubmitted) {
                        viewModel.onAction(ExampleAction.OnRefresh)
                    }
                }
            }

            viewModel.onAction(ExampleAction.OnInputChanged(input))
            viewModel.onAction(ExampleAction.OnSubmitClick)

            viewModel.uiState.test {
                // Skip the init block emissions
                skipItems(3)

                // Skip the OnSubmitClick emissions
                skipItems(2)

                // First emission while getData is loading
                awaitItem().let { stateSnapshot ->
                    stateSnapshot.loading.shouldBeTrue()
                    stateSnapshot.data.shouldContainSame(data)
                }

                // Second emission with the expected data after OnRefresh
                awaitItem().let {
                    it.loading.shouldBeFalse()
                    it.data.shouldContainSame(newData)
                }
            }
        }
    }
}
