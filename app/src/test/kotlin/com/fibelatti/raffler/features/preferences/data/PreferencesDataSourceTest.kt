package com.fibelatti.raffler.features.preferences.data

import com.fibelatti.raffler.BaseTest
import com.fibelatti.raffler.core.extension.mock
import com.fibelatti.raffler.core.extension.safeAny
import com.fibelatti.raffler.core.extension.shouldBe
import com.fibelatti.raffler.core.extension.shouldBeAnInstanceOf
import com.fibelatti.raffler.core.functional.Failure
import com.fibelatti.raffler.core.functional.Result
import com.fibelatti.raffler.core.functional.Success
import com.fibelatti.raffler.core.functional.getOrNull
import com.fibelatti.raffler.core.persistence.CurrentInstallSharedPreferences
import com.fibelatti.raffler.core.persistence.database.AppDatabase
import com.fibelatti.raffler.core.platform.AppConfig
import com.fibelatti.raffler.features.preferences.Preferences
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class PreferencesDataSourceTest : BaseTest() {

    private val mockCurrentInstallSharedPreferences = mock<CurrentInstallSharedPreferences>()
    private val mockPreferencesDao = mock<PreferencesDao>()
    private val mockAppDatabase = mock<AppDatabase>()
    private val mockPreferencesDtoMapper = mock<PreferencesDtoMapper>()

    private val preferencesDataSource = PreferencesDataSource(
        mockCurrentInstallSharedPreferences,
        mockPreferencesDao,
        mockAppDatabase,
        mockPreferencesDtoMapper
    )

    private val originalPreferences = PreferencesDto()

    @Before
    fun setup() {
        setupDatabaseTransaction()
    }

    // region getPreferences
    @Test
    fun `WHEN getPreferences is called AND error is thrown THEN Failure is returned`() {
        runBlocking {
            // GIVEN
            given(mockPreferencesDao.getPreferences())
                .willReturn(emptyList())

            // WHEN
            val result = preferencesDataSource.getPreferences()

            // THEN
            result.shouldBeAnInstanceOf<Failure>()
        }
    }

    @Test
    fun `WHEN getPreferences is called AND Success(Preferences) is returned`() {
        runBlocking {
            // GIVEN
            val mappedPreferences = Preferences(
                id = 0,
                appTheme = AppConfig.AppTheme.CLASSIC,
                appLanguage = AppConfig.AppLanguage.ENGLISH,
                lotteryDefaultQuantityAvailable = "0",
                lotteryDefaultQuantityToRaffle = "0",
                preferredRaffleMode = AppConfig.RaffleMode.NONE,
                rouletteMusicEnabled = false,
                rememberRaffledItems = false,
                hintsDisplayed = mutableMapOf()
            )
            val expectedPreferences = Preferences(
                id = 0,
                appTheme = AppConfig.AppTheme.DARK,
                appLanguage = AppConfig.AppLanguage.PORTUGUESE,
                lotteryDefaultQuantityAvailable = "0",
                lotteryDefaultQuantityToRaffle = "0",
                preferredRaffleMode = AppConfig.RaffleMode.NONE,
                rouletteMusicEnabled = false,
                rememberRaffledItems = false,
                hintsDisplayed = mutableMapOf()
            )

            given(mockPreferencesDao.getPreferences())
                .willReturn(listOf(originalPreferences))
            given(mockPreferencesDtoMapper.map(originalPreferences))
                .willReturn(mappedPreferences)
            given(mockCurrentInstallSharedPreferences.getTheme())
                .willReturn(AppConfig.AppTheme.DARK)
            given(mockCurrentInstallSharedPreferences.getAppLanguage())
                .willReturn(AppConfig.AppLanguage.PORTUGUESE)

            // WHEN
            val result = preferencesDataSource.getPreferences()

            // THEN
            result.shouldBeAnInstanceOf<Success<*>>()
            result.getOrNull() shouldBe expectedPreferences
        }
    }
    // endregion

    @Test
    fun `WHEN setAppTheme THEN CurrentInstallSharedPreferences setAppTheme is called`() {
        runBlocking {
            // WHEN
            preferencesDataSource.setAppTheme(AppConfig.AppTheme.DARK)

            // THEN
            verify(mockCurrentInstallSharedPreferences).setAppTheme(AppConfig.AppTheme.DARK)
        }
    }

    @Test
    fun `WHEN setLanguage THEN CurrentInstallSharedPreferences setAppLanguage is called`() {
        runBlocking {
            // WHEN
            preferencesDataSource.setLanguage(AppConfig.AppLanguage.SPANISH)

            // THEN
            verify(mockCurrentInstallSharedPreferences).setAppLanguage(AppConfig.AppLanguage.SPANISH)
        }
    }

    // region setRouletteMusicEnabled
    @Test
    fun `WHEN setRouletteMusicEnabled AND updateCurrentPreferences fails THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setRouletteMusicEnabled(false) }
    }

    @Test
    fun `WHEN setRouletteMusicEnabled AND updateCurrentPreferences succeeds THEN Success is returned`() {
        testUpdatePreferences(originalPreferences.copy(rouletteMusicEnabled = true)) {
            preferencesDataSource.setRouletteMusicEnabled(true)
        }
    }
    // endregion

    // region setPreferredRaffleMode
    @Test
    fun `WHEN setPreferredRaffleMode AND updateCurrentPreferences fails THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setPreferredRaffleMode(AppConfig.RaffleMode.ROULETTE) }
    }

    @Test
    fun `WHEN setPreferredRaffleMode AND updateCurrentPreferences succeeds THEN Success is returned`() {
        testUpdatePreferences(originalPreferences.copy(preferredRaffleMode = AppConfig.RaffleMode.ROULETTE.value)) {
            preferencesDataSource.setPreferredRaffleMode(AppConfig.RaffleMode.ROULETTE)
        }
    }
    // endregion

    // region setLotteryDefault
    @Test
    fun `WHEN setLotteryDefault AND updateCurrentPreferences fails THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setLotteryDefault(60, 6) }
    }

    @Test
    fun `WHEN setLotteryDefault AND updateCurrentPreferences succeeds THEN Success is returned`() {
        testUpdatePreferences(
            originalPreferences.copy(
                lotteryDefaultQuantityAvailable = 60,
                lotteryDefaultQuantityToRaffle = 6
            )
        ) {
            preferencesDataSource.setLotteryDefault(60, 6)
        }
    }
    // endregion

    // region rememberRaffledItems
    @Test
    fun `WHEN rememberRaffledItems AND updateCurrentPreferences fails THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.rememberRaffledItems(true) }
    }

    @Test
    fun `WHEN rememberRaffledItems AND updateCurrentPreferences succeeds THEN Success is returned`() {
        testUpdatePreferences(originalPreferences.copy(rememberRaffledItems = true)) {
            preferencesDataSource.rememberRaffledItems(true)
        }
    }
    // endregion

    // region resetHints
    @Test
    fun `WHEN resetHints AND updateCurrentPreferences fails THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.resetHints() }
    }

    @Test
    fun `WHEN resetHints AND updateCurrentPreferences succeeds THEN Success is returned`() {
        testUpdatePreferences(originalPreferences) {
            preferencesDataSource.resetHints()
        }
    }
    // endregion

    // region HINT_KEY_QUICK_DECISION
    @Test
    fun `WHEN getQuickDecisionHintDisplayed is called AND result is Failure THEN false is returned`() {
        testGetHint(repositoryResponse = emptyList(), expectedResult = false) {
            preferencesDataSource.getQuickDecisionHintDisplayed()
        }
    }

    @Test
    fun `WHEN getQuickDecisionHintDisplayed is called AND there is no map entry THEN false is returned`() {
        testGetHint(expectedResult = false) {
            preferencesDataSource.getQuickDecisionHintDisplayed()
        }
    }

    @Test
    fun `WHEN getQuickDecisionHintDisplayed is called THEN map value is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_QUICK_DECISION" to true)
        )

        testGetHint(repositoryResponse = listOf(preferencesWithExpectedKey), expectedResult = true) {
            preferencesDataSource.getQuickDecisionHintDisplayed()
        }
    }

    @Test
    fun `WHEN setQuickDecisionHintDismissed AND updateCurrentPreferences throws an error THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setQuickDecisionHintDismissed() }
    }

    @Test
    fun `WHEN setQuickDecisionHintDismissed THEN Success is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_QUICK_DECISION" to true)
        )

        testUpdatePreferences(preferencesWithExpectedKey) {
            preferencesDataSource.setQuickDecisionHintDismissed()
        }
    }
    // endregion

    // region HINT_KEY_ADD_NEW_QUICK_DECISION
    @Test
    fun `WHEN getAddNewQuickDecisionDisplayed is called AND result is Failure THEN false is returned`() {
        testGetHint(repositoryResponse = emptyList(), expectedResult = false) {
            preferencesDataSource.getAddNewQuickDecisionDisplayed()
        }
    }

    @Test
    fun `WHEN getAddNewQuickDecisionDisplayed is called AND there is no map entry THEN false is returned`() {
        testGetHint(expectedResult = false) {
            preferencesDataSource.getAddNewQuickDecisionDisplayed()
        }
    }

    @Test
    fun `WHEN getAddNewQuickDecisionDisplayed is called THEN map value is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_ADD_NEW_QUICK_DECISION" to true)
        )

        testGetHint(repositoryResponse = listOf(preferencesWithExpectedKey), expectedResult = true) {
            preferencesDataSource.getAddNewQuickDecisionDisplayed()
        }
    }

    @Test
    fun `WHEN setAddNewQuickDecisionDismissed AND updateCurrentPreferences throws an error THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setAddNewQuickDecisionDismissed() }
    }

    @Test
    fun `WHEN setAddNewQuickDecisionDismissed THEN Success is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_ADD_NEW_QUICK_DECISION" to true)
        )

        testUpdatePreferences(preferencesWithExpectedKey) {
            preferencesDataSource.setAddNewQuickDecisionDismissed()
        }
    }
    // endregion

    // region HINT_KEY_LOTTERY
    @Test
    fun `WHEN getLotteryHintDisplayed is called AND result is Failure THEN false is returned`() {
        testGetHint(repositoryResponse = emptyList(), expectedResult = false) {
            preferencesDataSource.getLotteryHintDisplayed()
        }
    }

    @Test
    fun `WHEN getLotteryHintDisplayed is called AND there is no map entry THEN false is returned`() {
        testGetHint(expectedResult = false) {
            preferencesDataSource.getLotteryHintDisplayed()
        }
    }

    @Test
    fun `WHEN getLotteryHintDisplayed is called THEN map value is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_LOTTERY" to true)
        )

        testGetHint(repositoryResponse = listOf(preferencesWithExpectedKey), expectedResult = true) {
            preferencesDataSource.getLotteryHintDisplayed()
        }
    }

    @Test
    fun `WHEN setLotteryHintDismissed AND updateCurrentPreferences throws an error THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setLotteryHintDismissed() }
    }

    @Test
    fun `WHEN setLotteryHintDismissed THEN Success is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_LOTTERY" to true)
        )

        testUpdatePreferences(preferencesWithExpectedKey) {
            preferencesDataSource.setLotteryHintDismissed()
        }
    }
    // endregion

    // region HINT_KEY_RAFFLE_DETAILS
    @Test
    fun `WHEN getRaffleDetailsHintDisplayed is called AND result is Failure THEN false is returned`() {
        testGetHint(repositoryResponse = emptyList(), expectedResult = false) {
            preferencesDataSource.getRaffleDetailsHintDisplayed()
        }
    }

    @Test
    fun `WHEN getRaffleDetailsHintDisplayed is called AND there is no map entry THEN false is returned`() {
        testGetHint(expectedResult = false) {
            preferencesDataSource.getRaffleDetailsHintDisplayed()
        }
    }

    @Test
    fun `WHEN getRaffleDetailsHintDisplayed is called THEN map value is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_RAFFLE_DETAILS" to true)
        )

        testGetHint(repositoryResponse = listOf(preferencesWithExpectedKey), expectedResult = true) {
            preferencesDataSource.getRaffleDetailsHintDisplayed()
        }
    }

    @Test
    fun `WHEN setRaffleDetailsHintDismissed AND updateCurrentPreferences throws an error THEN Failure is returned`() {
        testUpdatePreferencesError { preferencesDataSource.setRaffleDetailsHintDismissed() }
    }

    @Test
    fun `WHEN setRaffleDetailsHintDismissed THEN Success is returned`() {
        val preferencesWithExpectedKey = originalPreferences.copy(
            hintsDisplayed = mutableMapOf("HINT_KEY_RAFFLE_DETAILS" to true)
        )

        testUpdatePreferences(preferencesWithExpectedKey) {
            preferencesDataSource.setRaffleDetailsHintDismissed()
        }
    }
    // endregion

    private fun testGetHint(
        repositoryResponse: List<PreferencesDto> = listOf(originalPreferences),
        expectedResult: Boolean,
        methodCall: suspend () -> Boolean
    ) {
        runBlocking {
            // GIVEN
            given(mockPreferencesDao.getPreferences())
                .willReturn(repositoryResponse)

            // WHEN
            val result = methodCall()

            // THEN
            result shouldBe expectedResult
        }
    }

    private fun testUpdatePreferencesError(methodCall: suspend () -> Result<Unit>) {
        runBlocking {
            // GIVEN
            given(mockPreferencesDao.getPreferences())
                .willReturn(emptyList())

            // WHEN
            val result = methodCall()

            // THEN
            result.shouldBeAnInstanceOf<Failure>()
            verify(mockPreferencesDao, never()).setPreferences(safeAny())
        }
    }

    private fun testUpdatePreferences(
        updatedPreferences: PreferencesDto,
        methodCall: suspend () -> Result<Unit>
    ) {
        runBlocking {
            // GIVEN
            given(mockPreferencesDao.getPreferences())
                .willReturn(listOf(originalPreferences))

            // WHEN
            val result = methodCall()

            // THEN
            result.shouldBeAnInstanceOf<Success<*>>()
            verify(mockPreferencesDao).setPreferences(updatedPreferences)
        }
    }

    private fun setupDatabaseTransaction() {
        given(mockAppDatabase.runInTransaction(safeAny()))
            .willAnswer { (it.arguments.first() as? Runnable)?.run() }
    }
}
