package com.diving.wsql.builder

import com.diving.wsql.core.stuffToString
import com.diving.wsql.Utils.makeMountFieldKey
import com.diving.wsql.bean.*
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.en.Join
import com.diving.wsql.en.Operate
import com.diving.wsql.factory.QuerySqlFactory

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 开始查询
 * @version:
 **/
class SelectBuilder(private  val sqlFactory: QuerySqlFactory) : HelpBuilder {

    private var tableName: String? = null
    private var clazz: Class<*>? = null
    private var alias: MutableSet<Pair<String, String>> = mutableSetOf()
    private var excludeFields: MutableSet<String> = mutableSetOf()
    private var invert: Pair<Boolean, Boolean>? = null
    private var uk: String? = null
    private var innerUk: String? = null
    private var innerJoin: Join? = null
    private var innerConditionTerm: ConditionTerm? = null
    private var innerTableName: String? = null

    private var isDistinct = false

    fun setTableName(uk: String, tableName: String,clazz: Class<*>): SelectBuilder {
        this.uk = uk
        this.tableName = tableName
        this.clazz = clazz
        sqlFactory.setUkAndName(uk, tableName,clazz)
        return this
    }

    fun setDistinct(distinct: Boolean): SelectBuilder {
        this.isDistinct = distinct
        return this
    }

    fun aliasFields(alias: List<Pair<String, String>>): SelectBuilder {
        this.alias.addAll(alias)
        return this
    }

    fun aliasField(fieldName: String, queryName: String): SelectBuilder {
        this.alias.add(Pair(fieldName, queryName))
        return this
    }


    fun setInnerJoin(join: Join, tableName: String, uk: String, conditionTerm: ConditionTerm): SelectBuilder {
        this.innerUk = uk
        this.innerJoin = join
        this.innerConditionTerm = conditionTerm
        this.innerTableName = tableName
        sqlFactory.setUkAndName(uk, tableName,null)

        return this
    }

    fun excludeField(fieldName: String): SelectBuilder {
        if (!alias.map { it.first }.contains(fieldName))
            excludeFields.add(fieldName)
        return this
    }

    fun excludeFields(fieldNames: List<String>): SelectBuilder {
        //重命名的field无法排除
        val fs = fieldNames.filter { !alias.map { it.first }.contains(it) }
        excludeFields.addAll(fs)
        return this
    }


    //排除取反 将已经排除的字段添加进来，将没有排除的字段排除调
    fun invertExclude(invert: Boolean, recursion: Boolean): SelectBuilder {
        this.invert = invert to recursion
        return this
    }


    private fun logicOfExclude() {
        if (excludeFields.isNotEmpty()) {
            if (invert?.first == true) {
                if (excludeFields.isEmpty()) {
                    throw IllegalArgumentException("excludeFields is empty invert is fail")
                }
                val fields = if (invert?.second == true) {
                    clazz!!.getFieldsRecursive()
                } else {
                    clazz!!.declaredFields.toMutableList()
                }
                val fieldNames = fields.map { it.name }

                if (fieldNames.containsAll(excludeFields.toList())) {
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


    private fun doBefore() {
        requireNotNull(tableName) { "tableName is needed,please setTableName first" }
        requireNotNull(clazz) { "clazz is needed,please setClass first" }
        requireNotNull(uk) { "uk is needed,please setUk first" }
        sqlFactory.setFieldKey(ROOTNAME)
        logicOfExclude()

        val distinct = if (isDistinct) "distinct" else ""
        val sql = "${Operate.SELECT.string}  $distinct $FIELDS_CHARACTER_IN_SQL from $tableName $UK_CHARACTER_IN_SQL "
        sqlFactory.appendSql(uk!!, sql, tableName!!, true)

        if (innerUk !== null && innerConditionTerm != null && innerJoin !== null && innerTableName != null) {
            val innerSqlTerm = innerConditionTerm!!.getExpression(sqlFactory)
            val innerSQl = " ${innerJoin!!.s} (${Operate.SELECT.string} * from $innerTableName) $UK_CHARACTER_IN_SQL on $innerSqlTerm "
            sqlFactory.appendSql(innerUk!!, innerSQl, innerTableName!!, false)
        }

        val a = alias?.map { Alias(it.first, it.second, uk!!) }
        sqlFactory.appendAlias(a)
        val t = QueryTerm(uk!!, uk!!, makeMountFieldKey(ROOTUK, ROOTNAME), clazz)
        sqlFactory.appendTerm(t)
    }

    fun end(): QuerySqlFactory {
        doBefore()
        return sqlFactory
    }

}