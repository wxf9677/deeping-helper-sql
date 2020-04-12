package com.diving.wsql.temp

import com.diving.wsql.Utils
import com.diving.wsql.bean.SqlTemp
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.MOUNTKEY_SPLIT
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.core.*
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Operate
import com.diving.wsql.temp.annotations.*
import com.diving.wsql.temp.en.QP
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Field
import java.util.*


class QPMaker {
    private val sqlTemp = LinkedList<SqlTemp>()
    private val selectFields = StringBuffer()
    private val superClazz: Class<*>
    val sql = StringBuffer()
    val superQp: QP
    val query: MutableList<QP> = mutableListOf()

    private fun appendSql(uk: String, sql: String, tableName: String, main: Boolean) {
        require(!MakeUtil.tactfulWord.contains(uk)) { "uk:$uk is tactful in sql " }
        sqlTemp.add(SqlTemp(uk, sql, tableName, main))
    }

    constructor(clazz: Class<*>) {
        superClazz = clazz
        val csn = AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(csn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(csn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val tableName = csn.tableName
        val uk = csn.uk
        val distinct = if (csn.distinct) "distinct" else ""
        appendSql(uk, "${Operate.SELECT.string} $distinct $FIELDS_CHARACTER_IN_SQL from $tableName $UK_CHARACTER_IN_SQL ", tableName, true)
        //主要qp
        superQp = QP(uk, uk, uk, "", null, null, false, false, superClazz)
        makeQuery(superQp)
        require(query.isNotEmpty()) { "query is empty,makeSelection fail" }
        makeSelectFields()
        makeSql()
    }

    private fun makeSql() {
        sqlTemp.forEach {
            sql.append("")
            if (it.main) {
                sql.append(it.sql.replace(FIELDS_CHARACTER_IN_SQL, selectFields.toString()).replace(UK_CHARACTER_IN_SQL, it.uk))
            } else {
                sql.append(it.sql.replace(UK_CHARACTER_IN_SQL, it.uk))
            }
        }
        println("*******************")
        println("*******************")
        println("*******************")
        println(sql)
        println("*******************")
        println("*******************")
        println("*******************")
    }


    private fun makeSelectFields() {
        selectFields.append(" ")
        query.forEach {
            val sqlField = if (it.isCustom) {
                it.sqlFieldName
            } else {
                Utils.formatSqlField(it.sqlFieldName)
            }
            val uk = it.uk
            //如果存在重定向的查询
            if (uk.isNullOrEmpty() || it.isCustom) {
                selectFields.append("$sqlField, ")
            } else {
                selectFields.append("$uk.$sqlField as \"${uniKey(32)}\", ")
            }
        }
        selectFields.replace(selectFields.lastIndexOf(","), selectFields.lastIndexOf(",") + 1, "")
    }

    //装填查询的QP
    private fun makeQuery(qp: QP) {
        val sqlQueryFields = makeQp(qp)
        val primitiveFields = sqlQueryFields.filter { !it.isSuper() && Utils.isPrimitive(it.field!!) }
        val clazzFields = sqlQueryFields.filter { !it.isSuper() && !Utils.isPrimitive(it.field!!) }
        clazzFields.forEach { pair -> makeQuery(pair) }
        query.addAll(primitiveFields)
    }


    private fun makeJoin(join: SqlJoin) {
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
    }

    private fun makeMiddleJoin(join: SqlJoinMiddle) {

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
    }

    /**
     * @package: com.diving.wsql.temp
     * @createAuthor: wuxianfeng
     * @createDate: 2020-04-11
     * @createTime: 12:43
     * @describe: 描述
     * @version:
     **/
    private fun makeQp(qp: QP): List<QP> {

        val clazz: Class<*>
        val isCollection: Boolean
        val mountFiled: Field?

        //说明这是主类
        if (qp.isSuper()) {
            clazz = superClazz
            isCollection = false
            mountFiled = null
        } else {
            clazz = Utils.getClazzType(qp.field!!)
            isCollection = Utils.isFieldIterable(qp.field)
            mountFiled = qp.field
        }
        val mountUk = qp.fixUk
        val cn = AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(cn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(cn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val uk = cn.uk
        return clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk = uk
            var sqlFieldName = field.name
            var isCustom = false
            field.isAccessible = true
            //如果该字段有排除注解则不需要查询
            val fen = AnnotationUtils.findAnnotation(field, SqlExclude::class.java)
            if (fen != null) {
                return@mapNotNull null
            }
            //如果该字段不是常量并且没有连接注解则不需要查询
            if (!Utils.isPrimitive(field)) {
                val fjn = AnnotationUtils.findAnnotation(field, SqlJoinMiddle::class.java)
                val fjn1 = AnnotationUtils.findAnnotation(field, SqlJoin::class.java)
                if (fjn == null && fjn1 == null) {
                    return@mapNotNull null
                }
                if (fjn != null && fjn1 != null) {
                    throw  IllegalArgumentException("SqlJoinMiddle or SqlJoin must be di but can not di both")
                }
                if (fjn != null) {
                    makeMiddleJoin(fjn)
                }
                if (fjn1 != null) {
                    makeJoin(fjn1)
                }

            }
            //如果有重命名注解则更新查询字段名字
            val fan = AnnotationUtils.findAnnotation(field, SqlFieldName::class.java)
            sqlFieldName = fan?.sqlFieldName ?: field.name

            require(!(nowUk.contains(MOUNTKEY_SPLIT) ||
                    mountUk.contains(MOUNTKEY_SPLIT) ||
                    uk.contains(MOUNTKEY_SPLIT))) { "the uk or fieldName can not contains a char with $MOUNTKEY_SPLIT" }
            QP(nowUk, mountUk, uk, sqlFieldName, field, mountFiled, isCollection, isCustom, superClazz)
        }
    }
}