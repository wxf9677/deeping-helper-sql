package com.diving.wsql.temp.en

import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Link
import java.util.*

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

    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Date) {
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

        return when  {
            targetUk != null && targetFieldName !== null-> {
                val source = "$sourceUk.${Utils.formatSqlField(sourceFieldName)}"
                "$source ${arithmetic.string} $targetUk.${Utils.formatSqlField(targetFieldName!!)}"
            }
            targetValue.all { it is Date }-> {
                val source = "$sourceUk.${Utils.formatSqlField(sourceFieldName)}"
                "unix_timestamp($source)*1000  ${arithmetic.string} ${makeDateKeysAndValues()}"

            }
            else -> {
                val source = "$sourceUk.${Utils.formatSqlField(sourceFieldName)}"
                "$source ${arithmetic.string} ${makeDateKeysAndValues()}"

            }
        }




    }



    private fun makeDateKeysAndValues(): String {
        val values = targetValue.map { convert(it) }
        return when (arithmetic) {
            Arithmetic.THAN ,
            Arithmetic.LESS ,
            Arithmetic.EQUAL,
            Arithmetic.LIKE -> {
                require(values.size == 1) { "Arithmetic.EQUAL must compare with just one value" }
                " ${values[0]} "
            }
            Arithmetic.IN ->  "(${values.stuffToString()}) "

            Arithmetic.BETWEEN -> {
                if (values.size !=2)
                    throw IllegalArgumentException("Arithmetic.BETWEEN must compare with just two value")
                " ${values[0]} ${Link.AND} ${values[1]} "
            }
            else -> throw IllegalArgumentException("暂不支持")
        }
    }


    private fun convert(it: Any): String {
        return when (it) {
            is String -> "'$it'"
            is Int -> "$it"
            is Boolean -> "$it"
            is Date -> "${it.time}"
            else -> throw IllegalArgumentException("暂不支持")
        }
    }


}