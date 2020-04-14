package com.diving.wsql.temp

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.factory.QuerySqlFactory
import com.diving.wsql.bean.ConditionTerm
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Link

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: where 后面的条件
 * @version:
 **/
class TermBuilder(prefix:String="on") {
    private var conditionTerms = StringBuffer(prefix)

    private val selects = mutableSetOf<ConditionTerm>()


    fun setConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, tV: String): TermBuilder {

        val sT = "$sUk.${Utils.formatSqlField(sV)}"
        val tT = "$tUk.${Utils.formatSqlField(tV)}"
        conditionTerms.append(" $sT ${arithmetic.string} $tT ")
        return this
    }


    fun setConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: String): TermBuilder {
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }

    fun setConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Int): TermBuilder {
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }

    fun setConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, vararg value: Boolean): TermBuilder {
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }

    fun setAndConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, tV: String): TermBuilder {
        conditionTerms.append(Link.AND.string)
        val sT = "$sUk.${Utils.formatSqlField(sV)}"
        val tT = "$tUk.${Utils.formatSqlField(tV)}"
        conditionTerms.append(" $sT ${arithmetic.string} $tT ")
        return this
    }

    fun setAndConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, vararg value: String): TermBuilder {
        conditionTerms.append(Link.AND.string)
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }

    fun setAndConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, vararg value: Int): TermBuilder {
        conditionTerms.append(Link.AND.string)
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }

    fun setAndConditionTerm(sUk: String, sV: String, arithmetic: Arithmetic, tUk: String, vararg value: Boolean): TermBuilder {
        conditionTerms.append(Link.AND.string)
        val sT = "${sUk}.${Utils.formatSqlField(sV)}"
        conditionTerms.append(" $sT ${SqlSplitUtils.makeConditionValue(arithmetic, *value)} ")
        return this
    }



}