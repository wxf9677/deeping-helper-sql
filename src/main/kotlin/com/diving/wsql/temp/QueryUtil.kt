package com.diving.wsql.temp

import com.diving.wsql.GsonUtil
import com.diving.wsql.Utils
import com.diving.wsql.bean.QueryDto
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.core.checkException
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.en.Operate
import com.diving.wsql.temp.annotations.SqlQuery
import com.google.gson.Gson
import java.lang.reflect.Field
import javax.persistence.EntityManager


class QueryUtil {
    //临时存储非集合里的对象，用完清空
    private val singleClass = LinkedHashMap<String, QueryDto>()
    //临时存储集合里的对象，用户清空
    private val singleSubClass = LinkedHashMap<String, QueryDto>()
    //存储装填完的临时对象
    private val tempClass = LinkedHashMap<String, QueryDto>()

    fun query(clazz: Class<*>, operate: Operate,entityManager: EntityManager): String {
        val csn = MyAnnotationUtils.findAnnotation(clazz, SqlQuery::class.java)
        requireNotNull(csn) { "the class:${clazz.simpleName} must DI with Query" }
        val tableName = csn.tableName
        val uk = csn.uk


        val sql = when (operate) {
            Operate.SELECT -> {

                val distinct = if (csn.distinct) "distinct" else ""
                "${Operate.SELECT.string}  $distinct $FIELDS_CHARACTER_IN_SQL from $tableName $uk "
                /* sqlFactory.appendSql(uk!!, sql, tableName!!, true)

                if (innerUk !== null && innerConditionTerm != null && innerJoin !== null && innerTableName != null) {
                    val innerSqlTerm = innerConditionTerm!!.getExpression(sqlFactory)
                    val innerSQl = " ${innerJoin!!.s} (${Operate.SELECT.string} * from $innerTableName) $UK_CHARACTER_IN_SQL on $innerSqlTerm "
                    sqlFactory.appendSql(innerUk!!, innerSQl, innerTableName!!, false)
                }

                val a = alias?.map { Alias(it.first, it.second, uk!!) }
                sqlFactory.appendAlias(a)
                val t = QueryTerm(uk!!, uk!!, Utils.makeMountFieldKey(ROOTUK, ROOTNAME), clazz)
                sqlFactory.appendTerm(t)*/
            }
            else -> throw IllegalArgumentException("un support")
        }

        val selection = QPMaker.makeSelection(clazz)
        val queryParams = selection.first
        val params = selection.second
        if (!sql.contains(FIELDS_CHARACTER_IN_SQL)) {
            throw IllegalArgumentException("sql$sql must contains operate $FIELDS_CHARACTER_IN_SQL")
        }
        val newSql = sql.replace(FIELDS_CHARACTER_IN_SQL, queryParams)
        val query = entityManager.createNativeQuery(newSql)
        query.resultList.forEach { classFilling(uk, it, params) }

        return tempClass.filter { it.key.contains(qd.mountFieldKey) }.mapNotNull { it.value as? T }
    }

    //把查询到的结果根据QP装进对象
    private fun classFilling(uk:String, data: Any?, params: MutableList<QP>) {
        //判断返回的结果集数量和查询字段数量是否符合
        Utils.checkValueFix(data, params)



        params.forEachIndexed { index, q ->
            //获取需要加载数据的field
            val field = q.field

            if (data is Array<*>) {
                //获取qp对应的值
                var vResult = checkException({ data[index] }) ?: return@forEachIndexed
                //如果是列表
                if (q.isCollection) {
                    setFieldValue(field, Utils.formatValue(field, vResult), getSingleSubClazz(q, true))
                } else {
                    setFieldValue(field, Utils.formatValue(field, vResult), getSingleClazz(q, true))
                }
            } else {
                setFieldValue(field, Utils.formatValue(field, data), getSingleClazz(q, true))
            }

        }

        makeSingleInner(q, params)


        singleClass.clear()
        singleSubClass.clear()
    }


    private fun setFieldValue(field: Field, value: Any?, instance: Any?) {
        if (instance != null) {
            field.isAccessible = true
            val oldValue = field.get(instance)
            if (oldValue != value) {
                checkException({ field.set(instance, value) })
            }
        }
    }


    private fun getTempClazz(queryDto: QueryDto?, qd: QP): QueryDto? {
        queryDto ?: return null
        //因为mountkey是由唯一的uk和fieldName组成的，即可定位到某个类的某个变量关键字，在通过该类的字符集做为关键字可以的到唯一不重复的一个个类
        //todo 暂时用这个方法 Gson().toJson(queryDto)
        val key = qd.mountFieldKey + Gson().toJson(queryDto)
        if (tempClass[key] == null) {
            tempClass[key] = queryDto
        }
        return tempClass[key]
    }

    private fun getSingleClazz(gd: QP, autoCreate: Boolean): QueryDto? {
        val key = Utils.makeSingleClassKey(gd)
        if (singleClass[key] == null && autoCreate) {
            singleClass[key] = gd.clazz.newInstance() as QueryDto
        }
        return singleClass[key]
    }

    private fun getSingleSubClazz(gd: QP, autoCreate: Boolean): QueryDto? {
        val key = Utils.makeSingleSubClassKey(gd)
        if (singleSubClass[key] == null && autoCreate) {
            singleSubClass[key] = gd.clazz.newInstance() as QueryDto
        }
        return singleSubClass[key]
    }



    private fun makeSingleInner(qd: QP, params: List<QP>): QueryDto? {
        val obj = if (qd.isCollection)
            getTempClazz(getSingleSubClazz(qd, false), qd) ?: return null
        else
            getTempClazz(getSingleClazz(qd, false), qd) ?: return null
        val clazz = qd.clazz
        val fields = clazz.getFieldsRecursive()
        //这里符合条件的查询会重复，需要过滤
        val attach = params.filter { it.mountUk == qd.fixUk && fieldGetRight(fields, it) }
        val snakeMap = mutableMapOf<String, QP>()
        attach.forEach {
            if (snakeMap["${it.mountUk}-${it.mountFieldKey}"] == null) {
                snakeMap["${it.mountUk}-${it.mountFieldKey}"] = it
            }
        }
        snakeMap.forEach { m ->
            val qd = m.value
            if (qd.isCollection) {
                val field = fields.find { fieldGetRight(it, qd) } ?: return@forEach
                field.isAccessible = true
                val check = Utils.checkClazzType(field, qd.clazz)
                if (check.first && check.second) {
                    val innerDir = params.filter { it.mountUk == qd.fixUk }
                    if (innerDir.isNotEmpty()) {
                        makeSingleInner(qd, params)
                    }
                    val subClass = getSingleSubClazz(qd, false)
                    if (subClass != null) {
                        val fieldClass = field.get(obj) as? List<QueryDto>
                        val l = mutableListOf<QueryDto>()
                        if (fieldClass.isNullOrEmpty()) {
                            l.add(subClass)
                            field.set(obj, l)
                        } else {
                            val isContains = fieldClass.find { GsonUtil.toJson(it)== GsonUtil.toJson(subClass) } != null
                            if (isContains) {
                                l.addAll(fieldClass)
                                field.set(obj, l)
                            } else {
                                l.addAll(fieldClass)
                                l.add(subClass)
                                field.set(obj, l)
                            }
                        }
                    }

                } else {
                    throw IllegalAccessException(" the field type  is ${field.type.name},the clazz type  is ${qd.clazz.name} they are not fixed")

                }
            } else {
                val field = fields.find { fieldGetRight(it, qd) } ?: return@forEach
                field.isAccessible = true
                if (field?.type == qd.clazz) {
                    val innerDir = params.filter { it.mountUk == qd.fixUk }
                    val result = if (innerDir.isNotEmpty()) {
                        makeSingleInner(qd, params)
                    } else {
                        getTempClazz(getSingleClazz(qd, false), qd)
                    }
                    val s = field.get(obj)
                    if (result != null) {
                        if (GsonUtil.toJson(s)!= GsonUtil.toJson(result) ) {
                            field.set(obj, result)
                        }
                    }
                } else {
                    throw IllegalAccessException(" the field type  is ${field.type?.name},the clazz type  is ${qd.clazz.name} they are not fixed")
                }
            }
        }
        return obj
    }


    private fun fieldGetRight(field: Field, qd: QP): Boolean {
        return Utils.makeMountFieldKey(qd.mountUk, field.name) == qd.mountFieldKey
    }

    private fun fieldGetRight(fields: List<Field>, qd: QP): Boolean {
        return fields.map { Utils.makeMountFieldKey(qd.mountUk, it.name) }.contains(qd.mountFieldKey)
    }

}