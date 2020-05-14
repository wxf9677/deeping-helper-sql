package com.diving.wsql.temp

import com.diving.wsql.AnnotationUtils
import com.diving.wsql.GsonUtil
import com.diving.wsql.Utils
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.MOUNTKEY_SPLIT
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.core.stuffToString
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Join
import com.diving.wsql.en.Link
import com.diving.wsql.en.Operate
import com.diving.wsql.temp.annotations.*
import com.diving.wsql.temp.en.*
import java.lang.reflect.Field
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet


class QPMaker<T> {
    private val selectFields = StringBuffer()
    private val sql = StringBuffer()
    private val superQp: QP
    private val query = mutableListOf<QP>()
    private val whereBuilders = mutableMapOf<String, Where>()
    private val sqlTemp = LinkedHashSet<SQLTEMP>()
    private val ukPool = ArrayList<String>()


    private fun appendJoin(uk: String, sql: SQL, where: Where?, isSuper: Boolean) {
        require(!uk.contains(MOUNTKEY_SPLIT)) { "the uk or fieldName can not contains a char with $MOUNTKEY_SPLIT" }
        fitUk(uk)
        sqlTemp.add(SQLTEMP(uk, sql, where, isSuper))
    }


    private fun fitUk(uk: String) {
        require(!ukPool.contains(uk)) { "the uk:$uk is exist in ukPool ,can not add duplicate" }
        require(!MakeUtil.tactfulWord.contains(uk)) { "uk:$uk is tactful in sql " }
        ukPool.add(uk)
    }


    fun makeCount(): String {
        reMakeSqlTemp()
        MakeUtil.makeCountUrl(sql, sqlTemp)
        return sql.toString()
    }


    fun make(): OPTIONS<T> {
        reMakeSqlTemp()
        MakeUtil.makeSqlSelectionFields(selectFields, query)
        MakeUtil.makeUrl(sql, selectFields, sqlTemp)
        return OPTIONS(sql.toString(), query, superQp)
    }


    fun setFinalWhereBuilder(builder: Where): QPMaker<T> {
        val target = sqlTemp.find { it.isSuper }
        target?.where = builder
        return this

    }

    fun setWhereBuilder(uk: String, builder: Where): QPMaker<T> {
        whereBuilders[uk] = builder
        return this

    }


    fun appendJoin(uk: String, join: Join, tableName:String, where: Where?, condition: Condition): QPMaker<T> {
        val innersqlBuilder = SQL(join.s, Operate.SELECT, "", "*", tableName, UK_CHARACTER_IN_SQL, null, "on", listOf(condition))
        appendJoin(uk, innersqlBuilder, where, false)
        return this

    }


    private fun reMakeSqlTemp() {
        whereBuilders.forEach { builder ->
            val target = sqlTemp.find { it.uk == builder.key }
            target?.where = builder.value

        }
    }

    constructor(clazz: Class<T>) {
        val csn = AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(csn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(csn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val tableName = csn.tableName
        val uk = csn.uk
        val distinct = if (csn.distinct) "distinct" else ""
        val sqlBuilder = SQL("", Operate.SELECT, distinct, FIELDS_CHARACTER_IN_SQL, tableName, UK_CHARACTER_IN_SQL, null, "", listOf())
        appendJoin(uk, sqlBuilder, null, true)
        //主要qp
        superQp = QP(uk, uk, uk, "", null, null, false, false, clazz)
        makeQuery(superQp)
        require(query.isNotEmpty()) { "query is empty,makeSelection fail" }

    }


    private fun makeQuery(qp: QP) {
        val sqlQueryFields = makeQp(qp)
        val primitiveFields = sqlQueryFields.filter { !it.isSuper() && Utils.isPrimitive(it.field!!) }
        val clazzFields = sqlQueryFields.filter { !it.isSuper() && !Utils.isPrimitive(it.field!!) }
        clazzFields.forEach { pair -> makeQuery(pair) }
        query.addAll(primitiveFields)
    }


    private fun makeQp(qp: QP): List<QP> {
        val clazz: Class<*>
        val isCollection: Boolean
        val mountFiled: Field?
        var uk: String? = null
        val mountUk = qp.fixUk
        val superClazz = qp.superClazz
        val field = qp.field

        require(!(field != null && Utils.isPrimitive(field))) { "can not make qp with primitive field" }

        //说明这是主类
        if (qp.isSuper()) {
            uk = sqlTemp.find { it.isSuper }?.uk
            clazz = superClazz
            isCollection = false
            mountFiled = null
        } else {
            clazz = Utils.getClazzType(field!!)
            isCollection = Utils.isFieldIterable(field)
            mountFiled = field
            makeMiddleJoin(field)?.apply { uk = this }
            makeJoin(field)?.apply { uk = this }
        }

        if (uk == null) {
            return emptyList()
        }

        return clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk: String? = null
            var thisUk: String = uk!!
            var sqlFieldName: String = field.name
            var isCustom = false


            if (Utils.isPrimitive(field)) {
                //exclude
                val excludeResult = makeExclude(field)
                if (!excludeResult) {
                    return@mapNotNull null
                }
                //如果有重命名注解则更新查询字段名字
                val fieldNameResult = makeFieldName(field)
                if (fieldNameResult != null) {
                    sqlFieldName = fieldNameResult
                }
                //redirect
                makeRedirect(field) { uk, newInvalidFieldName, isInResult ->
                    nowUk = uk
                    sqlFieldName = newInvalidFieldName
                    isCustom = isInResult

                }
                makeRedirectCount(field) { uk, newInvalidFieldName ->
                    nowUk = uk
                    sqlFieldName = newInvalidFieldName

                }
                makeRedirectAllCount(field) { uk, newInvalidFieldName ->
                    nowUk = uk
                    sqlFieldName = newInvalidFieldName

                }
            }

            QP(nowUk ?: thisUk, mountUk, thisUk, sqlFieldName, field, mountFiled, isCollection, isCustom, superClazz)
        }
    }


    private fun makeExclude(field: Field): Boolean {
        val fen = AnnotationUtils.findAnnotation(field, SqlExclude::class.java)
        if (fen != null) {
            return false
        }
        return true
    }

    private fun makeFieldName(field: Field): String? {
        val fan = AnnotationUtils.findAnnotation(field, SqlFieldName::class.java)
        return fan?.sqlFieldName
    }


    private fun makeJoin(field: Field): String? {
        val join = AnnotationUtils.findAnnotation(field, SqlJoin::class.java) ?: return null
        val tableName: String = join.tableName
        val uk: String = join.uk
        val fieldName: String = join.fieldName
        val arithmetic: Arithmetic = join.arithmetic
        val targetUk: String = join.targetUk
        val targetFieldName: String = join.targetFieldName
        val joinString: String = join.join.s
        val sqlBuilder = SQL(joinString, Operate.SELECT, "", "*", tableName, UK_CHARACTER_IN_SQL, null, "on", listOf(Condition(uk, fieldName, arithmetic, targetUk, targetFieldName)))
        appendJoin(uk, sqlBuilder, null, false)
        return uk
    }

    private fun makeMiddleJoin(field: Field): String? {
        val join = AnnotationUtils.findAnnotation(field, SqlJoinMiddle::class.java) ?: return null
        val innerJoin = join.innerJoin
        val innerUk = innerJoin.uk
        val innerFieldName = innerJoin.fieldName
        val innerTableName = innerJoin.tableName
        val innerJ = innerJoin.join.s
        val innerArithmetic = innerJoin.arithmetic
        val innerTargetUk = innerJoin.targetUk
        val innerTargetFieldName = innerJoin.targetFieldName
        val innersqlBuilder = SQL(innerJ, Operate.SELECT, "", "*", innerTableName, UK_CHARACTER_IN_SQL, null, "on", listOf(Condition(innerUk, innerFieldName, innerArithmetic, innerTargetUk, innerTargetFieldName)))
        appendJoin(innerUk, innersqlBuilder, null, false)
        val tableName: String = join.tableName
        val uk: String = join.uk
        val fieldName: String = join.fieldName
        val arithmetic: Arithmetic = join.arithmetic
        val targetUk: String = join.targetUk
        val targetFieldName: String = join.targetFieldName
        val joinString: String = join.join.s
        val sqlBuilder = SQL(joinString, Operate.SELECT, "", "*", tableName, UK_CHARACTER_IN_SQL, null, "on", listOf(Condition(uk, fieldName, arithmetic, targetUk, targetFieldName)))
        appendJoin(uk, sqlBuilder, null, false)
        return uk
    }

    private fun makeRedirect(field: Field, callback: (String, String, Boolean) -> Unit) {
        val join = AnnotationUtils.findAnnotation(field, SqlRedirect::class.java) ?: return
        val tableName: String = join.tableName
        val uk: String = join.uk
        val fieldName: String = join.fieldName
        val arithmetic: Arithmetic = join.arithmetic
        val targetUk: String = join.targetUk
        val targetFieldName: String = join.targetFieldName
        val joinString: String = join.join.s
        val isInResult = join.isInResult
        //val invalidFieldName = join.invalidFieldName
        val newInvalidFieldName = if (isInResult) {
            "!isnull($uk.${Utils.formatSqlField(fieldName)})"
        } else {
            fieldName
        }

        val sqlBuilder = SQL(joinString, Operate.SELECT, "", "*", tableName, UK_CHARACTER_IN_SQL, null, "on", listOf(Condition(uk, fieldName, arithmetic, targetUk, targetFieldName)))
        val same = sqlTemp.find { it.uk == uk && GsonUtil.toJson(it.sql) == GsonUtil.toJson(sqlBuilder) }
        if (same == null) {
            appendJoin(uk, sqlBuilder, null, false)
        }

        callback.invoke(uk, newInvalidFieldName, isInResult)

    }


    //left join (select count(*) total_size, document_id from table_media_album_document  group by document_id) media on media.document_id = document.document_id

    private fun makeRedirectCount(field: Field, callback: (String, String) -> Unit) {
        val join = AnnotationUtils.findAnnotation(field, SqlRedirectCount::class.java) ?: return
        val tableName: String = join.tableName
        val uk: String = join.uk
        val fieldName: String = join.fieldName
        val arithmetic: Arithmetic = join.arithmetic
        val targetUk: String = join.targetUk
        val targetFieldName: String = join.targetFieldName
        val joinString: String = join.join.s
        val customCountFieldName = Utils.formatSqlField(field.name)
        val countUk = "countUk"
        //select distinct (select count(*) from taaaa  where user_id = countUk.user_id) count, user_id from taaaa countUk) roleqww on roleqww.user_id = user.user_id
        val w = Where().setConditionTerm(Condition(uk, fieldName, arithmetic, targetUk, targetFieldName),Link.None)
        val conditionsFields = w.conditionTerms.map { Utils.formatSqlField(it.first.sourceFieldName) }
        //select count(*) from taaaa  where user_id = countUk.user_id
        val countSql = SQL("", Operate.SELECT, "", "count(*)", tableName, "", w.make(sqlTemp), "", listOf())
        val selectParams = mutableListOf<String>()
        selectParams.add("(${countSql.make()}) $customCountFieldName")
        selectParams.addAll(conditionsFields)
        //select distinct (select count(*) from taaaa  where user_id = countUk.user_id) count, user_id from taaaa countUk)
        val countSql2 = SQL("", Operate.SELECT, "distinct", "${selectParams.stuffToString()}", tableName, countUk, null, "", listOf())
        val countSql3 = SQL(joinString, Operate.NONE, "", countSql2.make(), "", uk, null, "on", listOf(Condition(uk, fieldName, arithmetic, targetUk, targetFieldName)))
        appendJoin(uk, countSql3, null, false)
        callback.invoke(uk, customCountFieldName)

    }

    private fun makeRedirectAllCount(field: Field, callback: (String, String) -> Unit) {
        val join = AnnotationUtils.findAnnotation(field, SqlRedirectAllCount::class.java) ?: return
        val tableName: String = join.tableName
        val uk: String = join.uk
        val joinString: String = Join.INNER.s
        val customCountFieldName = Utils.formatSqlField(field.name)
        val countSql = SQL(joinString, Operate.SELECT, "", "count(*) $customCountFieldName", tableName, UK_CHARACTER_IN_SQL, null, "", listOf())
        appendJoin(uk, countSql, null, false)
        callback.invoke(uk, customCountFieldName)
    }

}