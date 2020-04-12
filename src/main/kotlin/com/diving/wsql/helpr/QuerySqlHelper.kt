package com.diving.wsql.helpr


import com.diving.wsql.GsonUtil
import com.diving.wsql.Utils
import com.diving.wsql.Utils.checkClazzType
import com.diving.wsql.Utils.checkValueFix
import com.diving.wsql.Utils.formatSqlField
import com.diving.wsql.Utils.getClazzType
import com.diving.wsql.Utils.isPrimitive
import com.diving.wsql.Utils.makeMountFieldKey
import com.diving.wsql.Utils.makeSingleClassKey
import com.diving.wsql.Utils.makeSingleSubClassKey
import com.diving.wsql.bean.*
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.ROOTNAME
import com.diving.wsql.builder.ROOTUK
import com.diving.wsql.core.checkException
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.core.uniKey
import com.google.gson.Gson
import java.lang.reflect.Field
import java.math.BigInteger
import javax.persistence.EntityManager


class QuerySqlHelper(val sql: String,
                     val terms: LinkedHashSet<QueryTerm>,
                     val redirects: ArrayList<Redirect>,
                     val alias: LinkedHashSet<Alias>,
                     val entityManager: EntityManager) {

    //临时存储非集合里的对象，用完清空
    private val singleClass = LinkedHashMap<String, QueryDto>()
    //临时存储集合里的对象，用户清空
    private val singleSubClass = LinkedHashMap<String, QueryDto>()
    //存储装填完的临时对象
    private val tempClass = LinkedHashMap<String, QueryDto>()


    fun <T : QueryDto> queryWithTotalCount(countSql: String): QueryTotalResultDto<T> {
        if (!countSql.contains(FIELDS_CHARACTER_IN_SQL)) {
            throw IllegalArgumentException("sql$countSql must contains operate $FIELDS_CHARACTER_IN_SQL")
        }
        val newCountSql = countSql.replace(FIELDS_CHARACTER_IN_SQL, "count(*)")
        val totalCount = (entityManager.createNativeQuery(newCountSql).resultList.first() as BigInteger).toLong()
        return QueryTotalResultDto(totalCount, query())
    }


    fun <T : QueryDto> query(): List<T> {
        val term = requireNotNull(terms.find { it.mountFieldKey == makeMountFieldKey(ROOTUK, ROOTNAME) }) { "you must start before query" }
        requireNotNull(term.clazz) { "query fail because query base is not exist" }
        val selection = makeSelection(term)
        val queryParams = selection.first
        val params = selection.second
        if (!sql.contains(FIELDS_CHARACTER_IN_SQL)) {
            throw IllegalArgumentException("sql$sql must contains operate $FIELDS_CHARACTER_IN_SQL")
        }
        val newSql = sql.replace(FIELDS_CHARACTER_IN_SQL, queryParams)
        val query = entityManager.createNativeQuery(newSql)
        val qd = requireNotNull(params.find { it.fixUk == term.uk }) { "query fail because qd base is not exist" }
        query.resultList.forEach { classFilling(qd, it, params) }
        return tempClass.filter { it.key.contains(qd.mountFieldKey) }.mapNotNull { it.value as? T }
    }


    //根据Qd生成查询语句
    private fun makeSelection(term: QueryTerm): Pair<String, MutableList<QD>> {
        val query: MutableList<QD> = mutableListOf()
        makeQuery(query, false, term)
        val params = StringBuffer()
        params.append(" ")
        query.forEach {
            val sqlField = if (it.isCustom) {
                it.alias
            } else {
                formatSqlField(it.alias)
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


    //装填查询的QD
    private fun makeQuery(query: MutableList<QD>, isCollection: Boolean, term: QueryTerm) {
        val clazz = term.clazz ?: return
        val uk = term.uk
        val mountUk = term.mountUk
        val mountFieldKey = term.mountFieldKey
        //如果有需要重新名的
        val sqlQueryFields = clazz.getFieldsRecursive().mapNotNull { field ->
            var nowUk = uk
            var nowFieldName = field.name
            var cutOut = false
            //重定义查询的fieldName
            alias?.find { it.uk == uk && it.fieldName == field.name }?.apply {
                //如果重命名的fieldName为空说明不需要查询
                if (aliasName.isNullOrEmpty()) {
                    cutOut = true
                } else {
                    nowFieldName = aliasName
                }
            }
            if (cutOut) {
                null
            } else {
                var isCustom = false
                redirects.find { it.tUk == uk && it.fieldName == field.name }?.apply {
                    nowUk = this.uk
                    this.wrapperValue?.apply {
                        isCustom = true
                        nowFieldName = this
                    }
                }
                QD(nowUk, uk, nowFieldName, field, clazz, isCollection, mountUk, mountFieldKey, isCustom)
            }

        }
        //先找出查询的基本类型
        val primitiveFields = sqlQueryFields.filter { isPrimitive(it.field) }
        //查出其他类型的
        val clazzFields = sqlQueryFields.filter { !isPrimitive(it.field) }
        clazzFields.forEach { pair ->
            //从条件列表中去找到当前类的field作为mountFieldKey挂载的条件
            val t = terms.find { makeMountFieldKey(uk, pair.field.name) == it.mountFieldKey } ?: return@forEach
            //判断当前条件的clazz和当前的field类型是否相同，如果不同则跳过
            val checkResult = checkClazzType(pair.field, t.clazz)
            if (checkResult.first) {
                if (isPrimitive(getClazzType(pair.field)))
                    throw IllegalArgumentException("because field type is ${pair.field.type} ,can not fill primitive value")
                makeQuery(query, checkResult.second, t)
            } else {
                throw IllegalArgumentException("soucre clazz is ${pair.field.type},but target clazz is ${t.clazz}")
            }

        }
        query.addAll(primitiveFields)
    }

    //把查询到的结果根据QD装进对象
    private fun classFilling(qd: QD, it: Any?, params: MutableList<QD>) {
        //判断返回的结果集数量和查询字段数量是否符合
        checkValueFix(it, params)
        params.forEachIndexed { index, q ->
            //获取需要加载数据的field
            val field = q.field
            if (it is Array<*>) {
                var vResult = checkException({ it[index] }) ?: return@forEachIndexed
                if (q.isCollection) {
                    setFieldValue(field, Utils.formatValue(field, vResult), getSingleSubClazz(q, true))
                } else {
                    setFieldValue(field, Utils.formatValue(field, vResult), getSingleClazz(q, true))
                }
            } else {
                setFieldValue(field, Utils.formatValue(field, it), getSingleClazz(q, true))
            }
        }
        makeSingleInner(qd, params)
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


    private fun getTempClazz(queryDto: QueryDto?, qd: QD): QueryDto? {
        queryDto ?: return null
        //因为mountkey是由唯一的uk和fieldName组成的，即可定位到某个类的某个变量关键字，在通过该类的字符集做为关键字可以的到唯一不重复的一个个类
        //todo 暂时用这个方法 Gson().toJson(queryDto)
        val key = qd.mountFieldKey + Gson().toJson(queryDto)
        if (tempClass[key] == null) {
            tempClass[key] = queryDto
        }
        return tempClass[key]
    }

    private fun getSingleClazz(gd: QD, autoCreate: Boolean): QueryDto? {
        val key = makeSingleClassKey(gd)
        if (singleClass[key] == null && autoCreate) {
            singleClass[key] = gd.clazz.newInstance() as QueryDto
        }
        return singleClass[key]
    }

    private fun getSingleSubClazz(gd: QD, autoCreate: Boolean): QueryDto? {
        val key = makeSingleSubClassKey(gd)
        if (singleSubClass[key] == null && autoCreate) {
            singleSubClass[key] = gd.clazz.newInstance() as QueryDto
        }
        return singleSubClass[key]
    }

    private fun makeSingleInner(qd: QD, params: List<QD>): QueryDto? {
        val obj = if (qd.isCollection)
            getTempClazz(getSingleSubClazz(qd, false), qd) ?: return null
        else
            getTempClazz(getSingleClazz(qd, false), qd) ?: return null
        val clazz = qd.clazz
        val fields = clazz.getFieldsRecursive()
        //这里符合条件的查询会重复，需要过滤
        val attach = params.filter { it.mountUk == qd.fixUk && fieldGetRight(fields, it) }
        val snakeMap = mutableMapOf<String, QD>()
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
                val check = checkClazzType(field, qd.clazz)
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
                            val isContains = fieldClass.find { GsonUtil.toJson(it)==GsonUtil.toJson(subClass) } != null
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
                    throw IllegalAccessException(" the field type  is ${field.type.simpleName},the clazz type  is ${qd.clazz.name} they are not fixed")

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
                        if (GsonUtil.toJson(s)!=GsonUtil.toJson(result) ) {
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


    private fun fieldGetRight(field: Field, qd: QD): Boolean {
        return makeMountFieldKey(qd.mountUk, field.name) == qd.mountFieldKey
    }

    private fun fieldGetRight(fields: List<Field>, qd: QD): Boolean {
        return fields.map { makeMountFieldKey(qd.mountUk, it.name) }.contains(qd.mountFieldKey)
    }


}