package com.diving.wsql.factory

import com.diving.wsql.helpr.QuerySqlHelper
import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.bean.*
import com.diving.wsql.builder.*
import com.diving.wsql.helpr.NativeSqlHelper
import java.math.BigInteger
import java.util.*
import javax.persistence.EntityManager
import kotlin.collections.LinkedHashSet


open class QuerySqlFactory(private val entityManager: EntityManager) {

    private val termTemp = linkedSetOf<QueryTerm>()
    private var sqlTemp = LinkedList<SqlTemp>()
    private val aliasTemp = LinkedHashSet<Alias>()
    private val ukAndTableNameTemp = arrayListOf<Triple<String, String,Class<*>?>>()
    private val redirectTemp = arrayListOf<Redirect>()
    private val fieldKeyTemp = arrayListOf<String>()
    //数据库的敏感字符，需要不断添加
    private val tactfulWord = listOf("like", "in", "on", "or", "=", "join", "describe", "a", "any")

    internal fun setUkAndName(uk: String, tableName: String,clazz: Class<*>?) {
        if (tactfulWord.contains(uk)) {
            throw IllegalArgumentException("uk:$uk is tactful in sql ")
        }
        if (ukAndTableNameTemp.map { it.first }.contains(uk)) {
            throw IllegalArgumentException("uk:$uk can not repeated addition ")
        }
        ukAndTableNameTemp.add(Triple(uk,tableName,clazz))
    }

    internal fun isUkExist(uk: String?, throwOut: Boolean): Boolean {
        if (uk.isNullOrEmpty())
            return false
        if (throwOut) {
            if (!ukAndTableNameTemp.map { it.first }.contains(uk)) {
                throw IllegalArgumentException("uk:${uk} is not define ")
            }
        }
        return ukAndTableNameTemp.map { it.first }.contains(uk)
    }


    internal fun findClazz(uk: String): Class<*> {
       return ukAndTableNameTemp.find { it.first==uk }?.third?: throw IllegalArgumentException("the clazz bind to uk:$uk is not define ")
    }

    internal fun setFieldKey(fieldKey: String) {
        if (fieldKeyTemp.contains(fieldKey)) {
            throw IllegalArgumentException("fieldKey$fieldKey can not repeated addition ")
        }
        fieldKeyTemp.add(fieldKey)
    }


    internal fun appendRedirect(redirects: List<Redirect>) {
        redirectTemp.addAll(redirects)
    }


    internal fun appendAlias(alias: List<Alias>) {
        aliasTemp.addAll(alias)

    }


    internal fun appendTerm(term: QueryTerm) {
        termTemp.add(term)
    }

    internal fun appendSql(uk: String, sql: String, tableName: String, main: Boolean) {
        if (tactfulWord.contains(uk)) {
            throw IllegalArgumentException("uk:$uk is tactful in sql ")
        }
        sqlTemp.add(SqlTemp(uk, sql, tableName, main))
    }

    fun selectCount(): CountBuilder {
        return CountBuilder(this)
    }


    fun select(): SelectBuilder {
        return SelectBuilder(this)
    }

    fun createJoin(): JoinBuilder {
        return JoinBuilder(this)
    }

    fun createReDirect(): ReDirectBuilder {
        return ReDirectBuilder(this)
    }

    fun createCountReDirect(): ReDirectCountBuilder {
        return ReDirectCountBuilder(this)
    }


    private var whereSql: String? = null

    private var selects: Set<ConditionTerm>? = null

    fun finalWhere(): WhereBuilder {
        return WhereBuilder("any", this, "where") { whereSql, pagedSql, indexUk, indexKey, s ->
            SqlSplitUtils.makePagedSql(pagedSql, sqlTemp, indexUk, indexKey, whereSql, s)
            this.whereSql = whereSql
            this.selects = s
        }
    }

    fun <T : QueryDto> result(): List<T> {
        val sql = StringBuffer()
        sqlTemp.forEach {
            sql.append(it.sql.replace(UK_CHARACTER_IN_SQL, it.uk))
        }
        return QuerySqlHelper(sql.toString(), termTemp, redirectTemp, aliasTemp, entityManager).query()
    }

    fun <T : QueryDto> resultWithTotalCount(): QueryTotalResultDto<T> {
        val sql = StringBuffer()
        sqlTemp.forEach {
            sql.append(it.sql.replace(UK_CHARACTER_IN_SQL, it.uk))
        }
        val tCountSql = SqlSplitUtils.getTotalCountSql(sqlTemp, whereSql, selects)
        return QuerySqlHelper(sql.toString(), termTemp, redirectTemp, aliasTemp, entityManager).queryWithTotalCount(
            tCountSql
        )
    }

    fun resultCount(): Long {
        val tCountSql = SqlSplitUtils.getTotalCountSql(sqlTemp, whereSql, selects)
        if (!tCountSql.contains(FIELDS_CHARACTER_IN_SQL)) {
            throw IllegalArgumentException("sql$tCountSql must contains operate $FIELDS_CHARACTER_IN_SQL")
        }
        val newCountSql = tCountSql.replace(FIELDS_CHARACTER_IN_SQL, "count(*)")
        val result = NativeSqlHelper(newCountSql, entityManager).nativeQuery()
        return (result.first() as BigInteger).toLong()
    }

}

