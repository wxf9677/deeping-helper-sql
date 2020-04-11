package com.diving.wsql.builder

import com.diving.wsql.bean.InResultConditionTerm
import com.diving.wsql.core.stuffToString
import com.diving.wsql.factory.QuerySqlFactory
import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.bean.*
import com.diving.wsql.en.Join
import com.diving.wsql.en.Operate
import java.util.*



/**
 当前查询语句示例，需要优化
left join (select (select count(*)
from table_media_album_document
where document_id = media.document_id) total_size,
document_id
from table_media_album_document media) media on media.document_id = document.document_id
**/


/**
不考虑其他功能最好是这样
left join (select count(*) total_size, document_id from table_media_album_document  group by document_id) media on media.document_id = document.document_id
 **/

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 查询关联实体的总数
 * @version:
 **/
class ReDirectCountBuilder(private  val sqlFactory: QuerySqlFactory) : HelpBuilder {
    private var join: Join? = null
    private var tableName: String? = null
    private var uk: String? = null
    private var selector = mutableSetOf<InResultConditionTerm>()
    private var termSelector = mutableSetOf<ConditionTerm>()
    private var sqlTerm = ""
    private var inCountUk: String? = null


    fun setJoin(join: Join, uk: String, tableName: String): ReDirectCountBuilder {
        this.join = join
        this.tableName = tableName
        this.uk = uk
        sqlFactory.setUkAndName(uk, tableName,null)
        return this
    }

    fun setSelect(field: String): ReDirectCountBuilder {
        selector.add(InResultConditionTerm(null, null, field, null))
        return this
    }


    //重定义查询目标字段的数量
    fun setCountRedirect(tUk: String, field: String, inCountUk: String? = null): ReDirectCountBuilder {
        sqlFactory.findClazz(tUk)
        this.inCountUk = inCountUk
        selector.add(InResultConditionTerm(tUk, field, field, COUNT_SQL_CHARACTER_IN_SQL))
        return this
    }

    fun setTerm(): WhereTermBuilder<ReDirectCountBuilder> {
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
            val redirects = mutableListOf<com.diving.wsql.bean.Redirect>()
            //如果select的uk不为空而且不是当前查询的uk则说明是需要重定向字段
            selector.filter { !it.tUk.isNullOrEmpty() && it.tUk != uk }.forEach {
                val a = Alias(it.tV!!, it.fieldName, it.tUk!!)
                sqlFactory.appendAlias(listOf(a))
                redirects.add(com.diving.wsql.bean.Redirect(uk!!, it.tUk!!, it.tV!!, null))

            }

            var inSql = LinkedList<SqlTemp>()
            val inCountUk = inCountUk ?: uk!!
            inSql.add(SqlTemp("", "${Operate.SELECT.string} distinct ${termFields(inCountUk)} from $tableName $inCountUk ", "", false))
            SqlSplitUtils.makePagedSql(pagedSql, inSql, indexUk, indexKey, whereSql, s)
            val partSql = "${join!!.s} (${inSql.mapNotNull { it.sql }.stuffToString(" ")}) $UK_CHARACTER_IN_SQL $sqlTerm"
            sqlFactory.appendSql(uk!!, partSql, tableName!!, false)
            sqlFactory.appendRedirect(redirects)
        }
    }


    private fun termFields(inCountUk: String): String {
        val countTerms = termSelector.map {
            val sv = Utils.formatSqlField(it.sV!!)
            " $sv ${it.q!!.string} $inCountUk.$sv"
        }
        return selector.mapNotNull {
            if (it.value == COUNT_SQL_CHARACTER_IN_SQL) {
                "(select count(*) from $tableName where ${countTerms.stuffToString(" and ")}) ${Utils.formatSqlField(it.tV!!)}"
            } else {
                Utils.formatSqlField(it.fieldName)
            }
        }.distinct().stuffToString()
    }
}