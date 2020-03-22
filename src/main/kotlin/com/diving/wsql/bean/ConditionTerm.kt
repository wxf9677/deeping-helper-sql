package com.diving.wsql.bean

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.factory.QuerySqlFactory
import javax.naming.NameNotFoundException

//查询条件
class ConditionTerm {

    var sUk: String? = null
    var sV: String? = null
    var tUk: String? = null
    var tV: Any? = null
    var q: Arithmetic?=null
    var expression: String? = null


    constructor(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, tV: String) {
        this.tUk = tUk
        this.sUk = sUk
        this.sV=sV
        this.tV=tV
        this.q=arithmetic
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        val tT = "$tUk.${Utils.formatSqlField(tV)}"
        expression = " $sT ${arithmetic.string} $tT "
    }




    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: String) {
        this.sUk = sUk
        this.sV=sV
        this.tV=value
        this.q=arithmetic

        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }

    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Int) {
        this.sUk = sUk
        this.sV=sV
        this.tV=value
        this.q=arithmetic

        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }

    constructor(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Boolean) {
        this.sUk = sUk
        this.sV=sV
        this.tV=value
        this.q=arithmetic


        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        expression = " $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} "
    }


    fun getExpression(sqlFactory: QuerySqlFactory): String {
        sUk?.apply { sqlFactory.isUkExist(sUk, true) }
        tUk?.apply { sqlFactory.isUkExist(tUk, true) }
        return expression ?: throw NameNotFoundException("expression is needed but now is null")
    }




}