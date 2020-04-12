package com.diving.wsql.temp

import com.diving.wsql.GsonUtil
import com.diving.wsql.Utils
import com.diving.wsql.Utils.checkValueFix
import com.diving.wsql.bean.QueryDto
import com.diving.wsql.core.checkException
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.temp.en.QP
import com.google.gson.Gson
import java.lang.reflect.Field
import javax.persistence.EntityManager


class QueryUtil {
    //临时存储每条数据库数据装好的类，用完清空
    private val readTempList = LinkedHashMap<String, QueryDto>()
    //存储结果集中的子对象
    private val subObjectList = LinkedHashMap<String, QueryDto>()
    //存储结果集
    private val objectList = LinkedHashMap<String, QueryDto>()
    //qp装载器
    private lateinit var qpMaker: QPMaker

    fun query(clazz: Class<*>, entityManager: EntityManager): List<QueryDto> {
        qpMaker = QPMaker(clazz)
        val query = entityManager.createNativeQuery(qpMaker.sql.toString())
        query.resultList.forEachIndexed { index, any -> classFilling(qpMaker.superQp, any) }
        return objectList.map { it.value }
    }

    //把查询到的结果根据QP装进对象
    private fun classFilling(qp: QP, data: Any?) {
        //判断返回的结果集数量和查询字段数量是否符合
        checkValueFix(data, qpMaker.query)
        //收集数据库返回的每条数据并装进临时对象
        makeTempObject(data)
        //把临时对象拼装起来
        assemble(qp)
        readTempList.clear()
    }

    private fun makeTempObject(data: Any?) {
        qpMaker.query.forEachIndexed { index, q ->
            val field = requireNotNull(q.field) { "only mainQp will lost it's field ,please check code" }
            var result = if (data is Array<*>) {
                checkException({ data[index] }) ?: return@forEachIndexed
            } else {
                data
            }
            val finalData = Utils.formatValue(field, result)
            setFieldValue(field, finalData, getTempObject(q, true))
        }
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


    private fun getSuperObject(queryDto: QueryDto?, qp: QP): QueryDto? {
        queryDto ?: return null
        val key = MakeUtil.makeMappingKey(qp) + Gson().toJson(queryDto)
        if (objectList[key] == null) {
            objectList[key] = queryDto
        }
        return objectList[key]
    }

    private fun getTempObject(qp: QP, autoCreate: Boolean): QueryDto? {
        val key = MakeUtil.makeMappingKey(qp)
        if (readTempList[key] == null && autoCreate) {
            readTempList[key] = qp.getMountFieldClass().newInstance() as QueryDto
        }
        return readTempList[key]
    }

    private fun getSubObject(queryDto: QueryDto?, qp: QP): QueryDto? {
        queryDto ?: return null
        val key = MakeUtil.makeMappingKey(qp) + Gson().toJson(queryDto)
        if (subObjectList[key] == null) {
            subObjectList[key] = queryDto
        }
        return subObjectList[key]
    }


    private fun assemble(qp: QP): QueryDto? {
        //获取qp对应的存储对象
        val obj = if (qp.isSuper())
            getSuperObject(getTempObject(qp, false), qp)
        else
            getSubObject(getTempObject(qp, false), qp)
        obj ?: return null
        //找出把当前qp的下挂载的内容
        val subObjectQp = getSubObjectQp(qp)
        subObjectQp.forEach { putToObj(it, obj) }
        return obj
    }


    private fun getSubObjectQp(qp: QP): MutableMap<String, QP> {
        val attach = qpMaker.query.filter { it.mountUk == qp.fixUk && it.mountUk != it.fixUk }
        val snakeMap = mutableMapOf<String, QP>()
        attach.forEach {
            if (snakeMap["${it.mountUk}-${it.getMountFieldName()}"] == null) {
                snakeMap["${it.mountUk}-${it.getMountFieldName()}"] = it
            }
        }
        return snakeMap
    }

    private fun putToObj(m: Map.Entry<String, QP>, obj: QueryDto): QueryDto? {
        //qd挂载的class对象
        val qp = m.value
        //获取当前qp所挂载的class
        val clazz = qp.getMountFieldClass()
        //获取当前class下面所有的field
        val field = qp.mountField!!
        //如果当前qd下面挂载了子对象
        val isInnerDir = qpMaker.query.any { it.mountUk == qp.fixUk }
        //获取当前qd所在类的类是否是一个collection
        val check = Utils.checkClazzType(field, qp.getMountFieldClass())
        require(check.first) { "the field ${field.name}'s genericType  is not ${qp.getMountFieldName()}" }
        field.isAccessible = true

        val subClass = if (isInnerDir) {
            assemble(qp)
        } else {
            getTempObject(qp, false)
        }
        subClass ?: return null

        if (qp.isCollection) {
            require(check.second) { "the class ${clazz.simpleName} must be a collection" }

            val fieldClass = field.get(obj) as? List<QueryDto>
            val l = mutableListOf<QueryDto>()
            if (fieldClass.isNullOrEmpty()) {
                l.add(subClass)
                field.set(obj, l)
            } else {
                val isContains = fieldClass.find { GsonUtil.toJson(it) == GsonUtil.toJson(subClass) } != null
                if (isContains) {
                    l.addAll(fieldClass)
                    field.set(obj, l)
                } else {
                    l.addAll(fieldClass)
                    l.add(subClass)
                    field.set(obj, l)
                }

            }
        } else {


            val s = field.get(obj)
            if (GsonUtil.toJson(s) != GsonUtil.toJson(subClass)) {
                field.set(obj, subClass)
            }


        }
        return obj
    }


}