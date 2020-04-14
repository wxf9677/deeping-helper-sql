package com.diving.wsql.temp

import com.diving.wsql.Utils
import com.diving.wsql.bean.SqlTemp
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.OBJ_SPLIT
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.core.uniKey
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.temp.en.QP
import java.util.*

object MakeUtil {


    /**
     * @Description: mysql关键字表
     * @Date: 20-4-13 下午5:02
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    val tactfulWord = listOf("like", "in", "on", "or", "=", "join", "describe", "a", "any")


    /**
     * @Description: 拼接查询条件
     * @Date: 20-4-13 下午5:02
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeConditionValue(sUk: String, sFieldName: String, sArithmetic: Arithmetic, targetUk: String, targetFieldName: String): String {
        val s = "$sUk.${Utils.formatSqlField(sFieldName)}"
        val t = "$targetUk.${Utils.formatSqlField(targetFieldName)}"
        return "$s ${sArithmetic.string} $t"
    }

    /**
     * @Description: 拼接映射key
     * @Date: 20-4-13 下午5:01
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeMappingKey(qp: QP): String {
        return "${qp.mountUk}$OBJ_SPLIT${qp.getMountFieldName()}"
    }


    /**
     * @Description:拼接查询字段
     * @Date: 20-4-13 下午5:00
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeSqlSelectionFields(selectFields: StringBuffer, query: MutableList<QP>) {
        selectFields.append(" ")
        query.forEach {
            val sqlField = if (it.isCustom) {
                it.sqlFieldName
            } else {
                Utils.formatSqlField(it.sqlFieldName)
            }
            val uk = it.uk
            //如果存在重定向的查询
            if (uk.isNullOrEmpty() || it.isCustom) {
                selectFields.append("$sqlField, ")
            } else {
                selectFields.append("$uk.$sqlField as \"${uniKey(32)}\", ")
            }
        }
        selectFields.replace(selectFields.lastIndexOf(","), selectFields.lastIndexOf(",") + 1, "")
    }

    fun makeUrl(sql: StringBuffer, selectFields: StringBuffer, sqlTemp: LinkedList<SqlTemp2>) {
        sqlTemp.forEach {
            sql.append("")
            if (it.isSuper) {
                sql.append(it.sql.make().replace(FIELDS_CHARACTER_IN_SQL, selectFields.toString()).replace(UK_CHARACTER_IN_SQL, it.uk))
            } else {
                sql.append(it.sql.make().replace(UK_CHARACTER_IN_SQL, it.uk))
            }
        }
    }
}



