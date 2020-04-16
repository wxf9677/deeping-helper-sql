package com.diving.wsql.builder

import com.diving.wsql.core.stuffToString
import com.diving.wsql.factory.QuerySqlFactory
import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.Utils.makeMountFieldKey
import com.diving.wsql.bean.*
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.en.Join
import com.diving.wsql.en.Operate
import java.util.*

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 联表查询
 * @version:
 **/
class JoinBuilder(private val sqlFactory: QuerySqlFactory) : HelpBuilder {
    private var join: Join? = null
    private var innerJoin: Join? = null
    private var tableName: String? = null
    private var innerTableName: String? = null
    private var clazz: Class<*>? = null
    private var alias: MutableSet<Pair<String, String>> = mutableSetOf()
    private var excludeFields: MutableSet<String> = mutableSetOf()
    private var invert: Pair<Boolean, Boolean>? = null
    private var uk: String? = null
    private var innerUk: String? = null
    private var tUk: String? = null
    private var tFieldName: String? = null
    private var innerConditionTerm: ConditionTerm? = null
    private var sqlTerm = ""

    fun setJoin(join: Join, uk: String, tableName: String, clazz: Class<*>): JoinBuilder {
        this.join = join
        this.uk = uk
        this.clazz = clazz
        this.tableName = tableName
        sqlFactory.setUkAndName(uk!!, tableName!!, clazz)
        return this
    }


    fun setInnerJoin(join: Join, tableName: String, uk: String, conditionTerm: ConditionTerm): JoinBuilder {
        this.innerUk = uk
        this.innerJoin = join
        this.innerConditionTerm = conditionTerm
        this.innerTableName = tableName
        sqlFactory.setUkAndName(innerUk!!, innerTableName!!, null)
        return this
    }

    fun setRelationShip(tUk: String, tFieldName: String): JoinBuilder {
        this.tUk = tUk
        this.tFieldName = tFieldName
        val targetClazz = sqlFactory.findClazz(tUk)
        requireNotNull(clazz) { "clazz is needed,please setJoin first" }
        Utils.isFieldExist(targetClazz, tFieldName!!, clazz!!)
        return this
    }

    fun aliasFields(alias: List<Pair<String, String>>): JoinBuilder {
        this.alias.addAll(alias)
        return this
    }

    fun aliasField(fieldName: String, queryName: String): JoinBuilder {
        this.alias.add(Pair(fieldName, queryName))
        return this
    }

    fun excludeField(fieldName: String): JoinBuilder {
        if (!alias.map { it.first }.contains(fieldName))
            excludeFields.add(fieldName)
        return this
    }

    fun excludeFields(fieldNames: List<String>): JoinBuilder {
        //重命名的field无法排除
        val fs = fieldNames.filter { !alias.map { it.first }.contains(it) }
        excludeFields.addAll(fs)
        return this
    }


    //排除取反 将已经排除的字段添加进来，将没有排除的字段排除调
    fun invertExclude(invert: Boolean, recursion: Boolean): JoinBuilder {
        this.invert = invert to recursion
        return this
    }

    // 取反操作
    private fun logicOfExclude() {
        if (excludeFields.isNotEmpty()) {
            if (invert?.first == true) {
                val fields = if (invert?.second == true) {
                    clazz!!.getFieldsRecursive()
                } else {
                    clazz!!.declaredFields.toList()
                }
                val fieldNames = fields.map { it.name }

                if (fieldNames.containsAll(excludeFields)) {
                    val invertFields = fieldNames.filter { !excludeFields.contains(it) }
                    val ivs = invertFields.filter { !alias.map { it.first }.contains(it) }.map { it to "" }
                    alias.addAll(ivs)
                } else {
                    val unFixFields = excludeFields.filter { !fieldNames.contains(it) }
                    throw IllegalArgumentException("the fields ${unFixFields.stuffToString()} " + "can not found in class ${clazz!!.simpleName}")
                }

            } else {
                val ivs = excludeFields.filter { !alias.map { it.first }.contains(it) }.map { it to "" }
                alias.addAll(ivs)
            }

        }
    }


    fun setTerm(): WhereTermBuilder<JoinBuilder> {
        return WhereTermBuilder(this, sqlFactory, "on") { term, select ->
            sqlTerm = term
        }
    }


    fun where(): WhereBuilder {
        requireNotNull(join) { "join is needed,please setJoin first" }
        requireNotNull(tableName) { "tableName is needed,please setTableName first" }
        requireNotNull(tUk) { "targetUk is needed,please setRelationShip first" }
        requireNotNull(tFieldName) { "tFieldName is needed,please setRelationShip first" }
        requireNotNull(uk) { "uk is needed,please setUk first" }

        return WhereBuilder(uk!!, sqlFactory, "where") { whereSql, pagedSql, indexUk, indexKey, s ->
            logicOfExclude()
            if (innerUk !== null && innerConditionTerm != null && innerJoin !== null && innerTableName != null) {
                val innerSqlTerm = innerConditionTerm!!.getExpression(sqlFactory)
                val innerSQl = " ${innerJoin!!.s} (${Operate.SELECT.string} * from $innerTableName) $UK_CHARACTER_IN_SQL on $innerSqlTerm "
                sqlFactory.appendSql(innerUk!!, innerSQl, innerTableName!!, false)
            }
            val fieldKey = makeMountFieldKey(tUk!!, tFieldName!!)
            sqlFactory.setFieldKey(fieldKey)
            var inSql = LinkedList<SqlTemp>()
            inSql.add(SqlTemp("", "${Operate.SELECT.string} * from $tableName", "", false))
            SqlSplitUtils.makePagedSql(pagedSql, inSql, indexUk, indexKey, whereSql, s)
            val sqlBody = "${join!!.s} (${inSql.mapNotNull { it.sql }.stuffToString(" ")} ) $UK_CHARACTER_IN_SQL "
            val sql = "$sqlBody $sqlTerm"
            sqlFactory.appendSql(uk!!, sql, tableName!!, false)
            val a = alias?.map { Alias(it.first, it.second, uk!!) }
            sqlFactory.appendAlias(alias = a)
            val t = QueryTerm(uk!!, tUk!!, fieldKey!!, clazz)
            sqlFactory.appendTerm(t)
        }
    }
}