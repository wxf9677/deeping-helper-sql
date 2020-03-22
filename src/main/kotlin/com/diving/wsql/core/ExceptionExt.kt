package com.diving.wsql.core


fun <T> checkException(doAny: () -> T, doIfActive: ((Throwable) -> Unit)? = null, finally: (() -> Unit)? = null, needLogOut: Boolean = true): T? {
    return try {
        doAny.invoke()
    } catch (e: Throwable) {
        if (needLogOut) {
            println(e.message)
        }
        doIfActive?.invoke(e)
        null
    } finally {
        finally?.invoke()
    }
}