package com.fibelatti.raffler.features.lottery.presentation

import com.fibelatti.raffler.BaseTest
import com.fibelatti.raffler.MockDataProvider
import com.fibelatti.raffler.core.extension.empty
import com.fibelatti.raffler.core.extension.givenSuspend
import com.fibelatti.raffler.core.extension.mock
import com.fibelatti.raffler.core.extension.safeAny
import com.fibelatti.raffler.core.extension.shouldNeverReceiveValues
import com.fibelatti.raffler.core.extension.shouldReceive
import com.fibelatti.raffler.core.functional.Success
import com.fibelatti.raffler.core.provider.ResourceProvider
import com.fibelatti.raffler.features.preferences.Preferences
import com.fibelatti.raffler.features.preferences.PreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given

class LotteryViewModelTest : BaseTest() {

    private val mockPreferencesRepository = mock<PreferencesRepository>()
    private val mockLotteryNumberModelMapper = mock<LotteryNumberModelMapper>()
    private val mockResourceProvider = mock<ResourceProvider>()

    private val mockPreferences = mock<Preferences>()
    private val mockLotteryNumberList = mock<List<LotteryNumberModel>>()

    private lateinit var viewModel: LotteryViewModel

    @Before
    fun setup() {
        given(mockResourceProvider.getString(anyInt()))
            .willReturn(MockDataProvider.genericString)
        givenSuspend { mockPreferencesRepository.getPreferences() }
            .willReturn(Success(mockPreferences))
        given(mockPreferences.lotteryDefaultQuantityAvailable)
            .willReturn(MockDataProvider.genericString)
        given(mockPreferences.lotteryDefaultQuantityToRaffle)
            .willReturn(MockDataProvider.genericString)

        viewModel = LotteryViewModel(
            mockPreferencesRepository,
            mockLotteryNumberModelMapper,
            mockResourceProvider,
            testThreadProvider
        )

        viewModel.defaultQuantityAvailable shouldReceive MockDataProvider.genericString
        viewModel.defaultQuantityToRaffle shouldReceive MockDataProvider.genericString
    }

    @Test
    fun whenGetLotteryNumbersIsCalledAndQuantityAvailableIsEmptyThenQuantityAvailableErrorReceivesError() {
        runBlocking {
            // WHEN
            viewModel.getLotteryNumbers(quantityAvailable = " ", quantityToRaffle = "15")

            // THEN
            viewModel.quantityAvailableError shouldReceive MockDataProvider.genericString
            viewModel.quantityToRaffleError.shouldNeverReceiveValues()
            viewModel.lotteryNumbers.shouldNeverReceiveValues()
        }
    }

    @Test
    fun whenGetLotteryNumbersIsCalledAndQuantityAvailableIsInvalidThenQuantityAvailableErrorReceivesError() {
        runBlocking {
            // WHEN
            viewModel.getLotteryNumbers(quantityAvailable = "abc", quantityToRaffle = "15")

            // THEN
            viewModel.quantityAvailableError shouldReceive MockDataProvider.genericString
            viewModel.quantityToRaffleError.shouldNeverReceiveValues()
            viewModel.lotteryNumbers.shouldNeverReceiveValues()
        }
    }

    @Test
    fun whenGetLotteryNumbersIsCalledAndQuantityToRaffleIsEmptyThenQuantityToRaffleErrorReceivesError() {
        runBlocking {
            // WHEN
            viewModel.getLotteryNumbers(quantityAvailable = "10", quantityToRaffle = " ")

            // THEN
            viewModel.quantityAvailableError shouldReceive String.empty()
            viewModel.quantityToRaffleError shouldReceive MockDataProvider.genericString
            viewModel.lotteryNumbers.shouldNeverReceiveValues()
        }
    }

    @Test
    fun whenGetLotteryNumbersIsCalledAndQuantityToRaffleIsInvalidThenQuantityToRaffleErrorReceivesError() {
        runBlocking {
            // WHEN
            viewModel.getLotteryNumbers(quantityAvailable = "10", quantityToRaffle = "abc")

            // THEN
            viewModel.quantityAvailableError shouldReceive String.empty()
            viewModel.quantityToRaffleError shouldReceive MockDataProvider.genericString
            viewModel.lotteryNumbers.shouldNeverReceiveValues()
        }
    }

    @Test
    fun whenGetLotteryNumbersIsCalledThenLotteryNumbersReceivesValue() {
        runBlocking {
            // GIVEN
            given(mockLotteryNumberModelMapper.mapList(safeAny()))
                .willReturn(mockLotteryNumberList)

            // WHEN
            viewModel.getLotteryNumbers(quantityAvailable = "10", quantityToRaffle = "5")

            // THEN
            viewModel.quantityAvailableError shouldReceive String.empty()
            viewModel.quantityToRaffleError shouldReceive String.empty()
            viewModel.error.shouldNeverReceiveValues()
            viewModel.lotteryNumbers shouldReceive mockLotteryNumberList
        }
    }
}
