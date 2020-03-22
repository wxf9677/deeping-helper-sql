package com.diving.wsql.core
import java.util.*
    private fun getUniqueKey(): String {
        return UUID.randomUUID().toString().let { it.replace("-","") }
    }
    fun uniKey(len:Int):String{
        return ("${System.currentTimeMillis()}${getUniqueKey()}").takeLast(len)
    }
