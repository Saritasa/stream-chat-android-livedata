package io.getstream.chat.android.livedata.utils

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import io.getstream.chat.android.client.utils.Result
import kotlinx.coroutines.*

interface Call2<T> {

    @WorkerThread
    fun execute(): Result<T>

    @UiThread
    fun enqueue(callback: (Result<T>) -> Unit = {})

    fun cancel()
}

class CallImpl2<T>(var runnable: suspend () -> Result<T>, var scope: CoroutineScope = GlobalScope) :
    Call2<T> {
    var canceled: Boolean = false

    override fun cancel() {
        canceled = true
    }

    override fun execute(): Result<T> {
        val result = runBlocking(scope.coroutineContext) { runnable() }
        return result
    }

    override fun enqueue(callback: (Result<T>) -> Unit) {
        scope.launch(scope.coroutineContext) {
            if (!canceled) {
                val result = runnable()
                callback(result)
            }
        }
    }
}
