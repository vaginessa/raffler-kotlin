package com.fibelatti.raffler.features.quickdecision.data

import com.fibelatti.raffler.BaseTest
import com.fibelatti.raffler.MockDataProvider
import com.fibelatti.raffler.core.extension.callSuspend
import com.fibelatti.raffler.core.extension.givenSuspend
import com.fibelatti.raffler.core.extension.mock
import com.fibelatti.raffler.core.extension.shouldBe
import com.fibelatti.raffler.core.extension.shouldBeAnInstanceOf
import com.fibelatti.raffler.core.extension.throwAssertionError
import com.fibelatti.raffler.core.functional.Failure
import com.fibelatti.raffler.core.functional.Success
import com.fibelatti.raffler.core.functional.exceptionOrNull
import com.fibelatti.raffler.core.functional.getOrNull
import com.fibelatti.raffler.core.provider.ResourceProvider
import com.fibelatti.raffler.features.quickdecision.QuickDecision
import com.google.gson.reflect.TypeToken
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.BDDMockito.given

class QuickDecisionDataSourceTest : BaseTest() {

    private val mockQuickDecisionDao = mock<QuickDecisionDao>()
    private val mockResourceProvider = mock<ResourceProvider>()
    private val mockQuickDecisionDtoMapper = mock<QuickDecisionDtoMapper>()

    private val mockQuickDecisionDtoList = mock<List<QuickDecisionDto>>()
    private val mockQuickDecisionList = mock<List<QuickDecision>>()
    private val mockError = mock<Throwable>()

    private val quickDecisionDataSource by lazy {
        QuickDecisionDataSource(
            mockQuickDecisionDao,
            mockResourceProvider,
            mockQuickDecisionDtoMapper
        )
    }

    @Before
    fun setup() {
        given(mockResourceProvider.getString(anyInt()))
            .willReturn(MockDataProvider.genericString)
        given(mockQuickDecisionDtoMapper.mapList(mockQuickDecisionDtoList))
            .willReturn(mockQuickDecisionList)
        given(mockQuickDecisionDtoMapper.mapListReverse(mockQuickDecisionList))
            .willReturn(mockQuickDecisionDtoList)
    }

    @Test
    fun `WHEN quickDecisionDao getAllQuickDecisions returns items THEN Success is returned`() {
        // GIVEN
        givenSuspend { mockQuickDecisionDao.getAllQuickDecisions() }
            .willReturn(mockQuickDecisionDtoList)

        // WHEN
        val result = callSuspend { quickDecisionDataSource.getAllQuickDecisions() }

        // THEN
        result shouldBeAnInstanceOf Success::class
        result.getOrNull()?.let {
            it shouldBe mockQuickDecisionList
        } ?: throwAssertionError()
    }

    @Test
    fun `WHEN quickDecisionDao getAllQuickDecisions returns empty AND getJsonFromAssets is returns null THEN Failure is returned`() {
        // GIVEN
        val typeToken = object : TypeToken<List<QuickDecision>>() {}

        givenSuspend { mockQuickDecisionDao.getAllQuickDecisions() }
            .willReturn(emptyList())
        given(mockResourceProvider.getJsonFromAssets("quick-decisions.json", typeToken))
            .willReturn(null)

        // WHEN
        val result = callSuspend { quickDecisionDataSource.getAllQuickDecisions() }

        // THEN
        result shouldBeAnInstanceOf Failure::class
        result.exceptionOrNull()?.let {
            it shouldBeAnInstanceOf RuntimeException::class
        }
    }

    @Test
    fun `WHEN quickDecisionDao getAllQuickDecisions returns empty AND getJsonFromAssets is returns a list AND addQuickDecisions fails THEN Failure is returned`() {
        // GIVEN
        val typeToken = object : TypeToken<List<QuickDecision>>() {}

        givenSuspend { mockQuickDecisionDao.getAllQuickDecisions() }
            .willReturn(emptyList())
        given(mockResourceProvider.getJsonFromAssets("quick-decisions.json", typeToken))
            .willReturn(mockQuickDecisionList)
        givenSuspend { mockQuickDecisionDao.addQuickDecisions(mockQuickDecisionDtoList) }
            .willAnswer { throw mockError }

        // WHEN
        val result = callSuspend { quickDecisionDataSource.getAllQuickDecisions() }

        // THEN
        result shouldBeAnInstanceOf Failure::class
        result.exceptionOrNull()?.let {
            it shouldBe mockError
        } ?: throwAssertionError()
    }

    @Test
    fun `WHEN quickDecisionDao getAllQuickDecisions returns empty AND getJsonFromAssets is returns a list AND addQuickDecisions succeeds THEN Success is returned`() {
        // GIVEN
        val typeToken = object : TypeToken<List<QuickDecision>>() {}

        givenSuspend { mockQuickDecisionDao.getAllQuickDecisions() }
            .willReturn(emptyList())
        given(mockResourceProvider.getJsonFromAssets("quick-decisions.json", typeToken))
            .willReturn(mockQuickDecisionList)

        // WHEN
        val result = callSuspend { quickDecisionDataSource.getAllQuickDecisions() }

        // THEN
        result shouldBeAnInstanceOf Success::class
        result.getOrNull()?.let {
            it shouldBe mockQuickDecisionList
        } ?: throwAssertionError()
    }

    @Test
    fun `WHEN addQuickDecisions is called AND quickDecisionDao throws an error THEN Failure is returned`() {
        // GIVEN
        givenSuspend { mockQuickDecisionDao.addQuickDecisions(mockQuickDecisionDtoList) }
            .willAnswer { throw mockError }

        // WHEN
        val result = callSuspend { quickDecisionDataSource.addQuickDecisions(mockQuickDecisionList) }

        // THEN
        result shouldBeAnInstanceOf Failure::class
        result.exceptionOrNull()?.let {
            it shouldBe mockError
        } ?: throwAssertionError()
    }

    @Test
    fun `WHEN addQuickDecisions is called THEN Success is returned`() {
        // WHEN
        val result = callSuspend { quickDecisionDataSource.addQuickDecisions(mockQuickDecisionList) }

        // THEN
        result shouldBeAnInstanceOf Success::class
    }
}
