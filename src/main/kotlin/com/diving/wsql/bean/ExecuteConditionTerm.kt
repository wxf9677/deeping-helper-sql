package com.diving.wsql.bean

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.factory.ExecuteSqlFactory
import javax.naming.NameNotFoundException

//查询条件
class ExecuteConditionTerm {

    var expression: String? = null

    constructor(sV: String, arithmetic: Arithmetic, vararg value: String) {
        val sT = "${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }

    constructor(sV: String, arithmetic: Arithmetic, vararg value: Int) {
        val sT = "${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }

    constructor(sV: String, arithmetic: Arithmetic, vararg value: Boolean) {
        val sT = "${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }



    fun getExpression(sqlFactory: ExecuteSqlFactory): String {
        return expression ?: throw NameNotFoundException("expression is needed but now is null")
    }



}