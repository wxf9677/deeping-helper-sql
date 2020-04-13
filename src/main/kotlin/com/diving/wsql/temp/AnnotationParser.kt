package com.diving.wsql.temp

import com.diving.wsql.bean.SqlTemp
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.MOUNTKEY_SPLIT
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Operate
import com.diving.wsql.temp.annotations.SqlJoin
import com.diving.wsql.temp.annotations.SqlJoinMiddle
import com.diving.wsql.temp.annotations.SqlQuery
import org.springframework.core.annotation.AnnotationUtils
import java.util.*
import kotlin.collections.ArrayList

/**
 * @Description: 解析注解的类
 * @Date: 20-4-13 下午5:11
 * @Author: wxf
 * @Version: 1.0
 * @Param
 * @Return
 */
class AnnotationParser {


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


    /**
     * @Description: 构建主语句
     * @Date: 20-4-13 下午5:18
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeSuperBody(clazz: Class<*>): String {
        val csn = AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(csn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(csn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val tableName = csn.tableName
        val uk = csn.uk
        val distinct = if (csn.distinct) "distinct" else ""
        appendSql(uk, "${Operate.SELECT.string} $distinct $FIELDS_CHARACTER_IN_SQL from $tableName $UK_CHARACTER_IN_SQL ", tableName, true)
        return uk
    }


    fun makeJoin(join: SqlJoin): String {
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

    fun makeMiddleJoin(join: SqlJoinMiddle): String {
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

    fun getSuperUk(): String {
       return  requireNotNull( sqlTemp.find { it.isSuper }?.uk){"the super uk is lost please makeSuperBody first "}
    }

}