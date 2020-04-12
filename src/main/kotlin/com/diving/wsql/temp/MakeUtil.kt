package com.diving.wsql.temp
import com.diving.wsql.Utils
import com.diving.wsql.builder.OBJ_SPLIT
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.temp.en.QP

object MakeUtil {

     val tactfulWord = listOf("like", "in", "on", "or", "=", "join", "describe", "a", "any")


    fun makeConditionValue(sUk: String, sFieldName: String, sArithmetic: Arithmetic, targetUk: String, targetFieldName: String): String {
        val s= "$sUk.${Utils.formatSqlField(sFieldName)}"

        val t=  "$targetUk.${Utils.formatSqlField(targetFieldName)}"
        return "$s ${sArithmetic.string} $t"
    }

    fun makeMappingKey(qp: QP): String {
        return "${qp.mountUk}$OBJ_SPLIT${qp.getMountFieldName()}"
    }


}



