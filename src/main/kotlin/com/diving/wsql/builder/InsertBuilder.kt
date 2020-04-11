package com.diving.wsql.builder

import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
import com.diving.wsql.core.getFieldsRecursive
import com.diving.wsql.en.Operate
import com.diving.wsql.factory.ExecuteSqlFactory

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 开始查询
 * @version:
 **/
class InsertBuilder(private val sqlFactory: ExecuteSqlFactory) : HelpBuilder {

    private var tableName: String? = null
    private var obj: Any? = null
    private var clazz: Class<*>? = null
    private var invert: Pair<Boolean, Boolean>? = null


    private var excludeFields: MutableSet<String> = mutableSetOf()

    fun setTableName(tableName: String): InsertBuilder {
        this.tableName = tableName
        return this
    }


    fun excludeField(fieldName: String): InsertBuilder {
        excludeFields.add(fieldName)
        return this
    }

    fun excludeFields(fieldNames: List<String>): InsertBuilder {
        excludeFields.addAll(fieldNames)
        return this
    }

    fun invertExclude(invert: Boolean, recursion: Boolean): InsertBuilder {
        this.invert = invert to recursion
        return this
    }


    fun setObj(obj: Any): InsertBuilder {
        this.obj = obj
        this.clazz=obj.javaClass
        return this
    }

    private fun doBefore() {
        requireNotNull(tableName) { "tableName is needed,please setTableName first" }
        requireNotNull(clazz) { "clazz is needed,please setObj first" }
        requireNotNull(obj) { "obj is needed,please setObj first" }
        val fields = if (invert?.second == true) {
            clazz!!.getFieldsRecursive()
        } else {
            clazz!!.declaredFields.toMutableList()
        }

        val f2 = if (invert?.first == true)
            fields.filter { excludeFields.contains(it.name) }
        else
            fields.filter { !excludeFields.contains(it.name) }

        val ff = f2.filter {
            it.isAccessible = true
            //获取基本类型并且这个值不为null
            Utils.isPrimitive(it) && it.get(obj) != null
        }


        val f = ff.map { Utils.formatSqlField(it.name) }.stuffToString()
        val v = ff.map {
            it.isAccessible = true

            when (val v = it.get(obj)) {
                is String -> "'$v'"
                else -> v.toString()
            }
        }.stuffToString()

        val sql = "${Operate.INSERT} into $tableName ($f)  values($v)"
        sqlFactory.appendSql(sql)
    }

    fun end(): ExecuteSqlFactory {
        doBefore()
        return sqlFactory
    }
}