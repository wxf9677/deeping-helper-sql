package com.diving.wsql.temp

import com.diving.wsql.Utils
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.temp.annotations.*
import com.diving.wsql.temp.en.QP
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Field


class QPMaker {
    private val selectFields = StringBuffer()
    private val annotationParser = AnnotationParser()
    val sql = StringBuffer()
    val superQp: QP
    val query: MutableList<QP> = mutableListOf()

    constructor(clazz: Class<*>) {
        val uk = annotationParser.makeSuperBody(clazz)
        //主要qp
        superQp = QP(uk, uk, uk, "", null, null, false, false, clazz)
        makeQuery(superQp)
        require(query.isNotEmpty()) { "query is empty,makeSelection fail" }
        MakeUtil.makeSqlSelectionFields(selectFields, query)
        MakeUtil.makeUrl(sql, selectFields, annotationParser.sqlTemp)
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
        val uk: String
        val mountUk = qp.fixUk
        val superClazz = qp.superClazz
        val field = qp.field

        require(!(field != null && Utils.isPrimitive(field))) { "can not make qp with primitive field" }

        //说明这是主类
        if (qp.isSuper()) {
            clazz = superClazz
            isCollection = false
            mountFiled = null
            uk = annotationParser.getSuperUk()
        } else {

            clazz = Utils.getClazzType(field!!)
            isCollection = Utils.isFieldIterable(field)
            mountFiled = field
            //如果是成员类先去检查有无定义uk的SqlUkDefined
            //如果没有则去取SqlQuery的uk
            //但是这样会出现uk重叠的现象
            uk = AnnotationUtils.findAnnotation(field, SqlUkDefined::class.java)?.uk
                    ?: requireNotNull(AnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)) { "the class:${clazz.simpleName} must DI with Query" }.uk
            val fjn = AnnotationUtils.findAnnotation(field, SqlJoinMiddle::class.java)
            val fjn1 = AnnotationUtils.findAnnotation(field, SqlJoin::class.java)

            if (fjn == null && fjn1 == null) {
                return listOf()
            }

            require(!(fjn != null && fjn1 != null)) { "SqlJoinMiddle or SqlJoin must be di but can not di both" }

            if (fjn != null) {
                require(uk == annotationParser.makeMiddleJoin(fjn)) { "the uk in SqlJoinMiddle on ${field.name} must be like  $uk " }
            }
            if (fjn1 != null) {
                require(uk == annotationParser.makeJoin(fjn1)) { "the uk in SqlJoin on ${field.name} must be like  $uk " }
            }
        }

        return clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk: String? = null
            var sqlFieldName: String
            var isCustom = false
            field.isAccessible = true
            //如果该字段有排除注解则不需要查询
            val fen = AnnotationUtils.findAnnotation(field, SqlExclude::class.java)
            if (fen != null) {
                return@mapNotNull null
            }
            //如果有重命名注解则更新查询字段名字
            val fan = AnnotationUtils.findAnnotation(field, SqlFieldName::class.java)
            sqlFieldName = fan?.sqlFieldName ?: field.name




            QP(nowUk ?: uk, mountUk, uk, sqlFieldName, field, mountFiled, isCollection, isCustom, superClazz)
        }
    }
}