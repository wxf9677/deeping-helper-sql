package com.diving.wsql.temp.en

import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
import com.diving.wsql.en.Arithmetic

class Condition {


     var sourceUk: String
     var sourceFieldName: String
     var targetUk: String? = null
     var targetFieldName: String? = null
     var arithmetic: Arithmetic
     var targetValue: List<Any> = listOf()


    constructor(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, tV: String) {
        this.targetUk = tUk
        this.sourceUk = sUk
        this.sourceFieldName = sV
        this.targetFieldName = tV
        this.arithmetic = arithmetic

    }


    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: String) {
        this.sourceUk = sUk
        this.sourceFieldName = sV
        this.targetValue = value.toList()
        this.arithmetic = arithmetic
    }

    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Int) {
        this.sourceUk = sUk
        this.sourceFieldName = sV
        this.targetValue = value.toList()
        this.arithmetic = arithmetic
    }

    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Boolean) {
        this.sourceUk = sUk
        this.sourceFieldName = sV
        this.targetValue = value.toList()
        this.arithmetic = arithmetic
    }


    fun make(): String {
        val source = "$sourceUk.${Utils.formatSqlField(sourceFieldName)}"

        var target = if (targetUk != null && targetFieldName !== null) {
            "$targetUk.${Utils.formatSqlField(targetFieldName!!)}"
        } else {
            val values = targetValue.map { convert(it) }
            when (arithmetic) {
                Arithmetic.EQUAL, Arithmetic.LIKE -> {
                    require(values.size == 1) { "Arithmetic.EQUAL must compare with just one value" }
                    " ${values[0]} "
                }
                Arithmetic.IN ->  "(${values.stuffToString()}) "
                else -> throw IllegalArgumentException("暂不支持")
            }


        }


        return "$source ${arithmetic.string} $target"

    }


    private fun convert(it: Any): String {
        return when (it) {
            is String -> "'$it'"
            is Int -> "$it"
            is Boolean -> "$it"
            else -> throw IllegalArgumentException("暂不支持")
        }
    }


}