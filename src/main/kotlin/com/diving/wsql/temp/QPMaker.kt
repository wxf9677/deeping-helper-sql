package com.diving.wsql.temp

import com.diving.wsql.Utils
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.core.getInterfaceRecursive
import com.diving.wsql.core.uniKey
import com.diving.wsql.temp.annotations.*


object QPMaker {
    //根据Qd生成查询语句
     fun makeSelection(clazz: Class<*>): Pair<String, MutableList<QP>> {
        val query: MutableList<QP> = mutableListOf()
        makeQuery(query, clazz)
        val params = StringBuffer()
        params.append(" ")
        query.forEach {
            val sqlField = if (it.isCustom) {
                it.sqlFieldName
            } else {
                Utils.formatSqlField(it.sqlFieldName)
            }
            val uk = it.uk
            //如果存在重定向的查询
            if (uk.isNullOrEmpty() || it.isCustom) {
                val f = "$sqlField, "
                params.append(f)
            } else {
                val f = "$uk.$sqlField"
                params.append("$f as \"${uniKey(32)}\", ")
            }
        }
        if (query.isEmpty()) {
            throw IllegalArgumentException("query is empty,makeSelection fail")
        }
        params.replace(params.lastIndexOf(","), params.lastIndexOf(",") + 1, "")
        //这里生成了查询的字段和需要装载的对象
        return params.toString() to query
    }


    //装填查询的QP
    private fun makeQuery(query: MutableList<QP>, clazz: Class<*>) {
        val sqlQueryFields = makeQp(clazz)
        val primitiveFields = sqlQueryFields.filter { Utils.isPrimitive(it.field) }
        val clazzFields = sqlQueryFields.filter { !Utils.isPrimitive(it.field) }
        clazzFields.forEach { pair ->
            //获取该字段对应的数据类型
            val fieldType = Utils.getClazzType(pair.field)
            //如果该类型数据基本类型则不支持，报错
            if (Utils.isPrimitive(fieldType))
                throw IllegalArgumentException("because field type is ${pair.field.type} ,can not fill primitive value")
            //获取SqlRelationShip注解
            val frn = MyAnnotationUtils.findAnnotation(clazz, pair.field, SqlRelationShip::class.java)
            //获取SqlRelationShipRedirect注解
            val frrn = MyAnnotationUtils.findAnnotation(clazz, pair.field, SqlRelationShipRedirect::class.java)
            //如果同时存在2种注解就不知道如何操作，报错
            if (frn != null && frrn != null) {
                throw IllegalArgumentException("the ${pair.field.name} DI with SqlRelationShip and SqlRelationShipRedirect is not support")
            }
            if (frn != null || frrn != null)
                makeQuery(query, fieldType)
        }
        query.addAll(primitiveFields)
    }

    /**
     * @package: com.diving.wsql.temp
     * @createAuthor: wuxianfeng
     * @createDate: 2020-04-11
     * @createTime: 12:43
     * @describe: 描述
     * @version:
     **/
    private fun makeQp(clazz: Class<*>): List<QP> {
        val cn = MyAnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(cn) { "the class:${clazz.simpleName} must DI with Query" }
        requireNotNull(cn.uk) { "the class:${clazz.simpleName} lost uk in Query" }
        val uk = cn.uk
        return clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk = uk
            var nowFieldName = field.name
            var isCustom = false
            field.isAccessible = true
            val fan = MyAnnotationUtils.findAnnotation(clazz, field, SqlFiledName::class.java)
            //定义重命名但是重命名的newName为空则表示跳过该字段查询
            if (fan != null && fan.sqlFieldName.isNullOrEmpty()) {
                null
            } else {
                //未定义重命名则保留
                nowFieldName = fan?.sqlFieldName ?: field.name
                val frn = MyAnnotationUtils.findAnnotation(clazz, field, SqlRedirect::class.java)
                if (frn != null) {
                    requireNotNull(frn.uk) { "the field:${field.name} lost uk in Redirect" }
                    nowUk = frn.uk
                    if (frn.customValue.isNotEmpty()) {
                        isCustom = true
                        nowFieldName = frn.customValue
                    }
                }

                QP(nowUk, uk, nowFieldName, field, clazz, field.type.getInterfaceRecursive().contains(Iterable::class.java), isCustom)
            }
        }
    }
}