package com.diving.wsql.temp

import com.diving.wsql.Utils
import com.diving.wsql.bean.SqlTemp
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.MOUNTKEY_SPLIT
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Operate
import com.diving.wsql.temp.annotations.*
import com.diving.wsql.temp.en.QP
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList


class QPMaker {
    private val selectFields = StringBuffer()
    val sql = StringBuffer()
    val superQp: QP
    val query: MutableList<QP> = mutableListOf()
    val sqlTemp = LinkedList<SqlTemp>()
    val ukPool = ArrayList<String>()


    private fun appendSql(uk: String, sql: String, tableName: String, isSuper: Boolean) {
        require(!uk.contains(MOUNTKEY_SPLIT)) { "the uk or fieldName can not contains a char with $MOUNTKEY_SPLIT" }
        fitUk(uk)
        sqlTemp.add(SqlTemp(uk, sql, tableName, isSuper))
    }


    private fun fitUk(uk: String) {
        require(!ukPool.contains(uk)) { "the uk:$uk is exist in ukPool ,can not add duplicate" }
        require(!MakeUtil.tactfulWord.contains(uk)) { "uk:$uk is tactful in sql " }
        ukPool.add(uk)
    }


    constructor(clazz: Class<*>) {
        val csn = AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(csn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(csn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val tableName = csn.tableName
        val uk = csn.uk
        val distinct = if (csn.distinct) "distinct" else ""
        appendSql(uk, "${Operate.SELECT.string} $distinct $FIELDS_CHARACTER_IN_SQL from $tableName $UK_CHARACTER_IN_SQL ", tableName, true)
        //主要qp
        superQp = QP(uk, uk, uk, "", null, null, false, false, clazz)
        makeQuery(superQp)
        require(query.isNotEmpty()) { "query is empty,makeSelection fail" }
        MakeUtil.makeSqlSelectionFields(selectFields, query)
        MakeUtil.makeUrl(sql, selectFields, sqlTemp)
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
        var uk: String
        val mountUk = qp.fixUk
        val superClazz = qp.superClazz
        val field = qp.field

        require(!(field != null && Utils.isPrimitive(field))) { "can not make qp with primitive field" }

        //说明这是主类
        if (qp.isSuper()) {
            uk = requireNotNull(sqlTemp.find { it.isSuper }?.uk) { "the super uk is lost" }
            clazz = superClazz
            isCollection = false
            mountFiled = null
        } else {
            uk = makeClassFieldUk(field!!)
            clazz = Utils.getClazzType(field)
            isCollection = Utils.isFieldIterable(field)
            mountFiled = field

            //如果该字段有排除注解则不需要查询
            val excludeResult = makeExclude(field)
            if (!excludeResult) {
                return emptyList()
            }
            //join
            val middleJoinResult = makeMiddleJoin(field)
            val joinResult = makeJoin(field)
            if (middleJoinResult == null && joinResult == null) {
                return emptyList()
            }
            if (middleJoinResult != null) {
                require(uk == middleJoinResult) { "the uk in SqlJoinMiddle on ${field.name} must be like  $uk " }
            }
            if (joinResult != null) {
                require(uk == joinResult) { "the uk in SqlJoin on ${field.name} must be like  $uk " }
            }
        }


        return clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk: String? = null
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
                makerDirect(field)


            }

            QP(nowUk ?: uk!!, mountUk, uk!!, sqlFieldName, field, mountFiled, isCollection, isCustom, superClazz)
        }
    }


    private fun makeClassFieldUk(field: Field): String {
        val clazz = Utils.getClazzType(field!!)
        return AnnotationUtils.findAnnotation(field, SqlUkDefined::class.java)?.uk
                ?: requireNotNull(AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)) { "the class:${clazz.simpleName} must DI with SqlQuery" }.uk

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
        val sqlTerm: String = MakeUtil.makeConditionValue(uk, fieldName, arithmetic, targetUk, targetFieldName)
        val sqlBody = "$joinString ( ${Operate.SELECT.string} * from $tableName  ) $UK_CHARACTER_IN_SQL "
        val sql = "$sqlBody on $sqlTerm"
        appendSql(uk!!, sql, tableName!!, false)
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
        val innerSqlTerm = MakeUtil.makeConditionValue(innerUk, innerFieldName, innerArithmetic, innerTargetUk, innerTargetFieldName)
        val innerSQl = " $innerJ (${Operate.SELECT.string} * from $innerTableName) $UK_CHARACTER_IN_SQL on $innerSqlTerm "
        appendSql(innerUk!!, innerSQl, innerTableName!!, false)

        val tableName: String = join.tableName
        val uk: String = join.uk
        val fieldName: String = join.fieldName
        val arithmetic: Arithmetic = join.arithmetic
        val targetUk: String = join.targetUk
        val targetFieldName: String = join.targetFieldName
        val joinString: String = join.join.s
        val sqlTerm = MakeUtil.makeConditionValue(uk!!, fieldName!!, arithmetic!!, targetUk!!, targetFieldName!!)
        val sqlBody = "$joinString ( ${Operate.SELECT.string} * from $tableName  ) $UK_CHARACTER_IN_SQL "
        val sql = "$sqlBody on $sqlTerm"
        appendSql(uk!!, sql, tableName!!, false)
        return uk
    }

    private fun makerDirect(field: Field) {
        val frn = AnnotationUtils.findAnnotation(field, SqlRedirect::class.java)

    }
}