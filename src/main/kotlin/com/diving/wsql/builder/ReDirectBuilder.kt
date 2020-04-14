package com.diving.wsql.builder

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.bean.InResultConditionTerm
import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
import com.diving.wsql.factory.QuerySqlFactory
import com.diving.wsql.bean.*
import com.diving.wsql.en.Join
import com.diving.wsql.en.Operate
import java.util.*

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 查询关联数据和外在条件的关系
 * @version:
 **/
class ReDirectBuilder(private val sqlFactory: QuerySqlFactory) : com.diving.wsql.builder.HelpBuilder {
    private var join: Join? = null
    private var tableName: String? = null
    private var uk: String? = null
    private var selector = mutableSetOf<InResultConditionTerm>()
    private var termSelector = mutableSetOf<com.diving.wsql.bean.ConditionTerm>()
    private var sqlTerm = ""


    fun setJoin(join: Join, uk: String, tableName: String): ReDirectBuilder {
        this.join = join
        this.tableName = tableName
        this.uk = uk
        sqlFactory.setUkAndName(uk, tableName, null)
        return this
    }

    fun setSelectRedirect(tUk: String, field: String, invalidFieldName: String): ReDirectBuilder {
        sqlFactory.findClazz(tUk)
        selector.add(InResultConditionTerm(tUk, field, invalidFieldName, null))
        return this
    }


    fun setSelect(field: String): ReDirectBuilder {
        selector.add(InResultConditionTerm(null, null, field, null))
        return this
    }


    //重定义查询目标字段和value的关系（等于或者包含）
    fun setInResultRedirect(tUk: String, field: String, invalidFieldName: String): ReDirectBuilder {
        selector.add(
                InResultConditionTerm(
                        tUk,
                        field,
                        invalidFieldName,
                        "!isnull($UK_CHARACTER_IN_SQL.${Utils.formatSqlField(invalidFieldName)})"
                )
        )
        return this
    }


    fun setTerm(): WhereTermBuilder<ReDirectBuilder> {
        return WhereTermBuilder(this, sqlFactory, "on") { term, s ->
            if (sqlTerm.isNullOrEmpty()) {
                sqlTerm = term
                termSelector.addAll(s)
                selector.addAll(s.map { InResultConditionTerm(null, null, it.sV!!, null) })
            }

        }
    }


    fun where(): WhereBuilder {
        requireNotNull(join) { "join is needed,please setJoin first" }
        requireNotNull(tableName) { "tableName is needed,please setTableName first" }
        requireNotNull(uk) { "uk is needed,please setUk first" }
        return WhereBuilder(uk!!, sqlFactory, "where") { whereSql, pagedSql, indexUk, indexKey, s ->
            val redirects = mutableListOf<Redirect>()
            //如果select的uk不为空而且不是当前查询的uk则说明是需要重定向字段
            selector.filter { !it.tUk.isNullOrEmpty() && it.tUk != uk }.forEach {
                val a = Alias(it.tV!!, it.fieldName, it.tUk!!)
                sqlFactory.appendAlias(listOf(a))
                if (it.value != null) {
                    redirects.add(
                            Redirect(
                                    uk!!,
                                    it.tUk!!,
                                    it.tV,
                                    it.value.replace(UK_CHARACTER_IN_SQL, uk!!)
                            )
                    )
                } else {
                    redirects.add(Redirect(uk!!, it.tUk!!, it.tV!!, null))
                }
            }
            var inSql = LinkedList<SqlTemp>()
            inSql.add(
                    SqlTemp(
                            "",
                            "${Operate.SELECT.string} ${termFields()} from $tableName  ",
                            "",
                            false
                    )
            )
            SqlSplitUtils.makePagedSql(pagedSql, inSql, indexUk, indexKey, whereSql, s)
            val partSql = "${join!!.s} (${inSql.mapNotNull { it.sql }.stuffToString(" ")}) $UK_CHARACTER_IN_SQL $sqlTerm"
            sqlFactory.appendSql(uk!!, partSql, tableName!!, false)
            sqlFactory.appendRedirect(redirects)
        }
    }


    private fun termFields(): String {
        return selector.mapNotNull { Utils.formatSqlField(it.fieldName) }.distinct().stuffToString()
    }
}