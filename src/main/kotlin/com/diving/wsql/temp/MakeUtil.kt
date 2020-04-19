package com.diving.wsql.temp

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.OBJ_SPLIT
import com.diving.wsql.builder.PAGED_REPLACE_CHARACTER_IN_SQL
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.core.uniKey
import com.diving.wsql.en.Direction
import com.diving.wsql.temp.en.Condition
import com.diving.wsql.temp.en.QP
import com.diving.wsql.temp.en.SQLTEMP
import com.diving.wsql.temp.en.Where
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

object MakeUtil {


    /**
     * @Description: mysql关键字表
     * @Date: 20-4-13 下午5:02
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    val tactfulWord = listOf("like", "in", "on", "or", "=", "join", "describe", "a", "any")


    /**
     * @Description: 拼接映射key
     * @Date: 20-4-13 下午5:01
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeMappingKey(qp: QP): String {
        return "${qp.mountUk}$OBJ_SPLIT${qp.getMountFieldName()}"
    }


    /**
     * @Description:拼接查询字段
     * @Date: 20-4-13 下午5:00
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun makeSqlSelectionFields(selectFields: StringBuffer, query: MutableList<QP>) {
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


    fun makeUrl(sql: StringBuffer, selectFields: StringBuffer, sqlTemp: LinkedHashSet<SQLTEMP>) {
        var finalWhere: Where? = null
        sqlTemp.forEach {
            sql.append("")
            if (it.isSuper) {
                finalWhere = it.where
                sql.append(it.sql.make().replace(FIELDS_CHARACTER_IN_SQL, selectFields.toString()).replace(UK_CHARACTER_IN_SQL, it.uk))
            } else {
                sql.append(it.sql.make().replace(UK_CHARACTER_IN_SQL, it.uk))
            }
        }
        finalWhere?.apply {sql.append(this.make(sqlTemp)) }
    }


    fun makeCountUrl(sql: StringBuffer, sqlTemp: LinkedHashSet<SQLTEMP>) {
        sql.append("")
        var finalWhere: Where? = sqlTemp.find { it.isSuper }?.where

        finalWhere?.apply {


            sql.append(this.makeCount(sqlTemp))
        }
    }

    fun makeOrderSql(sql: String, sorts: Set<Triple<String?, String, Direction?>>): String {

        val newSql = StringBuffer()
        val index = AtomicInteger(0)
        newSql.append(sql)
        if (sorts.isNotEmpty()) {
            newSql.append(" order by")
            sorts.forEach {
                newSql.append(" ")
                if (index.getAndIncrement() > 0) {
                    newSql.append(",")
                }
                if (it.first == null) {
                    newSql.append("${Utils.formatSqlField(it.second)}")
                } else {
                    newSql.append("${it.first}.${Utils.formatSqlField(it.second)}")
                }
                newSql.append(" ")
                it.third?.name?.apply { newSql.append(this) }
                newSql.append(" ")
            }
        }

        return newSql.toString()
    }


    fun makePagedSql(pagedSql: String?, sqlTemp: LinkedHashSet<SQLTEMP>, indexUk: String?, indexKey: String?, select: Set<Condition>): String {
        return if (!pagedSql.isNullOrEmpty()) {
            requireNotNull(indexUk) { "indexUk is null but it is needed" }
            requireNotNull(indexKey) { "indexKey is null but it is needed" }
            //创造关键uk
            val includeKeys = mutableSetOf<String>()
            includeKeys.add(indexUk)
            includeKeys.addAll(select.mapNotNull { it.sourceUk })
            //创造分页语句
            val temp = LinkedHashSet<SQLTEMP>()
            //先把主句柄加进来
            val mainSql = requireNotNull(sqlTemp.find { it.isSuper }) { "the finalWhere sql must contains FIELDS_CHARACTER_IN_SQL" }
            temp.add(mainSql)
            //迭代添加包含关键uk的句柄
            filterSql(temp, sqlTemp, includeKeys)
            val newSqlTemp = LinkedHashSet<SQLTEMP>()
            newSqlTemp.addAll(sqlTemp)
            val interator = newSqlTemp.iterator()
            while (interator.hasNext()) {
                val next = interator.next()
                if (!temp.contains(next)) {
                    interator.remove()
                }
            }
            val newPagedTemp = StringBuffer()
            val pagedSqlParts = pagedSql.split(PAGED_REPLACE_CHARACTER_IN_SQL)
            pagedSqlParts.forEachIndexed { index, s ->
                newPagedTemp.append(s)
                if (index != pagedSqlParts.size - 1) {
                    newSqlTemp.forEach {
                        val sql = it.sql
                        if (sql.params == FIELDS_CHARACTER_IN_SQL) {
                            sql.params = " $indexUk.$indexKey"
                        }

                        if (sql.uk == UK_CHARACTER_IN_SQL) {
                            sql.uk = it.uk
                        }
                        newPagedTemp.append(sql.make())

                    }

                }
            }
            newPagedTemp.toString()
        } else {
            ""
        }
    }


    private fun filterSql(temp: LinkedHashSet<SQLTEMP>, sqlTemp: LinkedHashSet<SQLTEMP>, includeKeys: MutableSet<String>) {

        var needLoop = false
        sqlTemp.forEach { sqlPair ->
            //如果关键sql句柄中包含关键的uk则添加到temp中
            if (includeKeys.contains(sqlPair.uk)) {
                temp.add(sqlPair)
            }
        }
        //如果添加的语句里引用了其他的uk则把他当作关键uk存起来
        temp.forEach { sql ->
            sqlTemp.map { it.uk }.forEach {
                //如果该sql句柄依赖该uk说明依赖该sql
                if (sql.sql.make().contains("$it.") && !includeKeys.contains(it)) {
                    includeKeys.add(it)
                    needLoop = true
                }
            }
        }

        if (needLoop)
            filterSql(temp, sqlTemp, includeKeys)
    }


    fun getTotalCountSql(sqlTemp: LinkedHashSet<SQLTEMP>, whereSql: String?, select: Set<Condition>): String {
        //创造关键uk
        val includeKeys = mutableSetOf<String>()
        includeKeys.addAll(select.mapNotNull { it.sourceUk })
        //创造分页语句
        val temp = LinkedHashSet<SQLTEMP>()
        //先把主句柄加进来
        val mainSql = requireNotNull(sqlTemp.find { it.isSuper }) { "the finalWhere sql must contains FIELDS_CHARACTER_IN_SQL" }
        temp.add(mainSql)
        //迭代添加包含关键uk的句柄
        filterSql(temp, sqlTemp, includeKeys)
        val newSqlTemp = LinkedHashSet<SQLTEMP>()
        newSqlTemp.addAll(sqlTemp)
        val iterator=newSqlTemp.iterator()
        while (iterator.hasNext()){
            val next=iterator.next()
            if(!temp.contains(next)){
                iterator.remove()
            }
        }
        val newSql = StringBuffer()
        newSqlTemp.forEach {
            val sql = it.sql
            if (sql.params == FIELDS_CHARACTER_IN_SQL) {
                sql.params = " count(*) "
            }

            if (sql.uk == UK_CHARACTER_IN_SQL) {
                sql.uk = it.uk
            }

            newSql.append(sql.make())
        }

        whereSql?.apply { newSql.append(this) }
        return newSql.toString()
    }

}



