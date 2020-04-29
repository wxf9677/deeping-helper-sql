package com.diving.wsql.temp

import com.diving.wsql.GsonUtil
import com.diving.wsql.Utils
import com.diving.wsql.Utils.checkValueFix
import com.diving.wsql.core.checkException
import com.diving.wsql.temp.en.OPTIONS
import com.diving.wsql.temp.en.QP
import com.google.gson.Gson
import java.lang.reflect.Field
import java.math.BigInteger
import javax.persistence.EntityManager


class QueryContext<T> {
    //临时存储每条数据库数据装好的类，用完清空
    private val readTempList = LinkedHashMap<String, Any>()
    //存储结果集中的子对象
    private val subObjectList = LinkedHashMap<String, Any>()
    //存储结果集
    private val objectList = LinkedHashMap<String, Any>()
    //qp装载器
    private lateinit var options: OPTIONS<T>

    fun query(options: OPTIONS<T>, entityManager: EntityManager): List<T> {
        this.options = options
        val query = entityManager.createNativeQuery(options.sql)
        query.resultList.forEachIndexed { index, any -> classFilling(options.superQp, any) }
        return objectList.map { it.value as T}
    }


    fun queryCount(o: String, entityManager: EntityManager): Long {
        val query = entityManager.createNativeQuery(o)
        return  (query.resultList.first() as BigInteger).toLong()
    }

    //把查询到的结果根据QP装进对象
    private fun classFilling(qp: QP, data: Any?) {
        //判断返回的结果集数量和查询字段数量是否符合
        checkValueFix(data, options.query)
        //收集数据库返回的每条数据并装进临时对象
        makeTempObject(data)
        //把临时对象拼装起来
        assemble(qp)
        readTempList.clear()
    }

    private fun makeTempObject(data: Any?) {
        options.query.forEachIndexed { index, q ->
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


    private fun getSuperObject(queryDto: Any?, qp: QP): Any? {
        queryDto ?: return null
        val key = MakeUtil.makeMappingKey(qp) + Gson().toJson(queryDto)
        if (objectList[key] == null) {
            objectList[key] = queryDto
        }
        return objectList[key]
    }

    private fun getTempObject(qp: QP, autoCreate: Boolean): Any? {
        val key = MakeUtil.makeMappingKey(qp)
        if (readTempList[key] == null && autoCreate) {
            readTempList[key] = qp.getMountFieldClass().newInstance()
        }
        return readTempList[key]
    }

    private fun getSubObject(queryDto: Any?, qp: QP): Any? {
        queryDto ?: return null
        val key = MakeUtil.makeMappingKey(qp) + Gson().toJson(queryDto)
        if (subObjectList[key] == null) {
            subObjectList[key] = queryDto
        }
        return subObjectList[key]
    }


    private fun assemble(qp: QP): Any? {
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
        val snakeMap = mutableMapOf<String, QP>()
        options.query.forEach {
            if (it.mountUk == qp.fixUk && it.mountUk != it.fixUk &&it.getMountFieldName().isNotEmpty()&&snakeMap["${it.mountUk}-${it.getMountFieldName()}"] == null){
                snakeMap["${it.mountUk}-${it.getMountFieldName()}"] = it
            }
        }
        return snakeMap
    }

    private fun putToObj(m: Map.Entry<String, QP>, obj: Any): Any? {
        //qd挂载的class对象
        val qp = m.value
        //获取当前qp所挂载的class
        val clazz = qp.getMountFieldClass()
        //获取当前class下面所有的field
        val field = qp.mountField!!
        //如果当前qd下面挂载了子对象
        val isInnerDir = options.query.any { it.mountUk == qp.fixUk }
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
            val fieldClass = field.get(obj) as? Collection<Any>
            val list = mutableListOf<Any>()
            if (fieldClass.isNullOrEmpty()) {
                list.add(subClass)
            } else {
                val isContains = fieldClass.map { GsonUtil.toJson(it) }.contains(GsonUtil.toJson(subClass))
                list.addAll(fieldClass)
                if (!isContains) {
                    list.add(subClass)
                }
            }
            field.set(obj, list)
        } else {
            val s = field.get(obj)
            if (GsonUtil.toJson(s) != GsonUtil.toJson(subClass)) {
                field.set(obj, subClass)
            }
        }
        return obj
    }


}