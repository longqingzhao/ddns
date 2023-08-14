package com.ddns.cloudflare

import com.ddns.cloudflare.api.*
import com.ddns.cloudflare.data.Response
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.CoroutineContext


class Manager : CoroutineScope {

    private val atomicLong = AtomicLong(1000 * 60 * 60)
    private val atomicLong1 = AtomicLong(0)

    init {
        launch {
            while (isActive) {
                val job = async (coroutineContext) {
                    info("start query dns")
                    val data = ApiManager.api.queryDns(zoneId)
                    info("getData get data:${data.result.size}")
                    check(data)
                    delay(atomicLong.get())
                    atomicLong1.getAndUpdate { 0 }
                }
                job.await()
                delay(atomicLong1.get())
            }
        }
    }

    private suspend fun check(data: Response<List<Response.Result>>) {
        if (data.success) {
            val result = data.result.find { it.name == changeDns }
            val ipResult = ApiManager.api.queryIP()
            info("api data:${ipResult.data}")
            if (!ipResult.data.isNullOrEmpty() && ipResult.data != result?.content && result != null) {
                info("change ip:${ipResult.data}")
                val r = ApiManager.api.updateIP(zoneId, result.id!!, result.copy(content = ipResult.data))
                if (r.success) {
                    info("change success")
                    atomicLong.getAndSet(1000 * 60 * 60)
                } else {
                    info("change error")
                }
            } else {
                info("needn't change")
            }
        }

    }

    override val coroutineContext: CoroutineContext
        get() = Main.coroutineContext + CoroutineExceptionHandler { coroutines, throwable ->
            fail("error:$coroutines", throwable)
            atomicLong.getAndSet(1000 * 60 * 60 * 4)
            atomicLong1.getAndSet(500)
        }
}