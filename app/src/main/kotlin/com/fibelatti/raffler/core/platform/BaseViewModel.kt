package com.fibelatti.raffler.core.platform

import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fibelatti.raffler.core.provider.ThreadProvider
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import kotlin.coroutines.experimental.CoroutineContext

abstract class BaseViewModel(
    private val threadProvider: ThreadProvider
) : ViewModel(), CoroutineScope {

    private val parentJob by lazy { Job() }

    val error by lazy { MutableLiveData<Throwable>() }

    override val coroutineContext: CoroutineContext
        get() = threadProvider.main() + parentJob

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        parentJob.cancel()
    }

    protected fun handleError(error: Throwable) {
        this.error.value = error
    }

    protected fun start(
        block: suspend CoroutineScope.() -> Unit
    ) = launch(threadProvider.main()) { block() }

    protected suspend fun <T> inBackground(
        block: suspend CoroutineScope.() -> T
    ): T = withContext(threadProvider.background()) { block() }

    protected suspend fun <T> CoroutineScope.inBackgroundForParallel(
        block: suspend CoroutineScope.() -> T
    ): Deferred<T> = async(threadProvider.background()) { block() }
}
