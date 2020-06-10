package com.diving.wsql

import com.diving.wsql.core.stuffToString
import com.diving.wsql.Utils.formatSqlField
import com.diving.wsql.bean.ConditionTerm
import com.diving.wsql.bean.SqlTemp
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.builder.PAGED_REPLACE_CHARACTER_IN_SQL
import com.diving.wsql.builder.UK_CHARACTER_IN_SQL
import com.diving.wsql.en.Arithmetic
import com.diving.wsql.en.Direction
import com.diving.wsql.en.Link
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet

object SqlSplitUtils {


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
                    newSql.append("${formatSqlField(it.second)}")
                } else {
                    newSql.append("${it.first}.${formatSqlField(it.second)}")
                }
                newSql.append(" ")
                it.third?.name?.apply { newSql.append(this) }
                newSql.append(" ")
            }
        }

        return newSql.toString()
    }


    //indexMax 默认是按照时间来的，需要其他的比较对象，需要重新设置
    fun makePageWithIndex(indexKey: String, linkUk: String, indexKeyDirection: Direction?, offset: Int, size: Int): String {

        //这里是否需要将所有排序加进来需要优化
        var startOffSet: Int
        var endOffSet: Int
        var direct: String
        val start: String
        val end: String
        when (indexKeyDirection) {
           Direction.ASC -> {
                startOffSet = offset
                endOffSet = offset + size - 1
                direct = "order by $linkUk.$indexKey ASC"
                start = "( $PAGED_REPLACE_CHARACTER_IN_SQL  $direct limit $startOffSet , 1)"
                end = "(select ifnull(($PAGED_REPLACE_CHARACTER_IN_SQL $direct limit $endOffSet , 1),(select MAX($indexKey) from ($PAGED_REPLACE_CHARACTER_IN_SQL) a )))"
            }
           Direction.DESC -> {
                startOffSet = offset + size - 1
                endOffSet = offset
                direct = "order by $linkUk.$indexKey DESC"
                start = "(select ifnull(($PAGED_REPLACE_CHARACTER_IN_SQL  $direct limit $startOffSet , 1),(select MIN($indexKey) from ($PAGED_REPLACE_CHARACTER_IN_SQL) a )))"
                end = "($PAGED_REPLACE_CHARACTER_IN_SQL  $direct limit $endOffSet , 1)"
            }
            else -> {
                startOffSet = offset + size - 1
                endOffSet = offset
                direct = "order by $linkUk.$indexKey DESC"
                start = "(select ifnull(($PAGED_REPLACE_CHARACTER_IN_SQL  $direct limit $startOffSet , 1),(select MIN($indexKey) from ($PAGED_REPLACE_CHARACTER_IN_SQL) a)))"
                end = "($PAGED_REPLACE_CHARACTER_IN_SQL  $direct limit $endOffSet , 1)"
            }
        }

        return if (linkUk == null)
            " $indexKey between $start and $end"
        else
            " $linkUk.$indexKey between $start and $end"

    }


    private fun filterSql(temp: LinkedHashSet<SqlTemp>, sqlTemp: LinkedList<SqlTemp>, includeKeys: MutableSet<String>) {

        var needLoop=false
        sqlTemp.forEach { sqlPair ->
            //如果关键sql句柄中包含关键的uk则添加到temp中
            if (includeKeys.contains(sqlPair.uk)) {
                temp.add(sqlPair)
            }
        }
        //如果添加的语句里引用了其他的uk则把他当作关键uk存起来
        temp.forEach { sql->
            sqlTemp.map { it.uk }.forEach {
                //如果该sql句柄依赖该uk说明依赖该sql
                if (sql.sql.trim().contains("${it}.")&&!includeKeys.contains(it)) {
                    includeKeys.add(it)
                    needLoop=true
                }
            }
        }

        if (needLoop)
            filterSql(temp, sqlTemp, includeKeys)
    }






    fun makePagedSql(pagedSql: String?, sqlTemp: LinkedList<SqlTemp>, indexUk: String?, indexKey: String?, whereSql: String, select: Set<ConditionTerm>) {
        if (!pagedSql.isNullOrEmpty()) {
            requireNotNull(indexUk) { "indexUk is null but it is needed" }
            requireNotNull(indexKey) { "indexKey is null but it is needed" }
            //创造关键uk
            val includeKeys = mutableSetOf<String>()
            includeKeys.add(indexUk)
            includeKeys.addAll(select.mapNotNull { it.sUk })
            //创造分页语句
            val temp = LinkedHashSet<SqlTemp>()
            //先把主句柄加进来
            val mainSql = requireNotNull(sqlTemp.find { it.sql.contains(FIELDS_CHARACTER_IN_SQL)} ){"the finalWhere sql must contains FIELDS_CHARACTER_IN_SQL"}
            temp.add(mainSql)
            //迭代添加包含关键uk的句柄
            filterSql(temp, sqlTemp, includeKeys)

            val newSqlTemp = LinkedHashSet<SqlTemp>()
            newSqlTemp.addAll(sqlTemp)

            val interator=newSqlTemp.iterator()

            while (interator.hasNext()){
                val next=interator.next()

                if(!temp.contains(next)){
                    interator.remove()
                }
            }

            newSqlTemp.add(SqlTemp("", whereSql, "", false))

            val newPagedTemp = LinkedList<SqlTemp>()
            val pagedSqlParts = pagedSql.split(PAGED_REPLACE_CHARACTER_IN_SQL)
            pagedSqlParts.forEachIndexed { index, s ->
                newPagedTemp.add(SqlTemp("", s, "", false))
                if (index != pagedSqlParts.size - 1) {
                    val s = newSqlTemp.map {
                        SqlTemp("", it.sql.replace(UK_CHARACTER_IN_SQL, it.uk).replace(FIELDS_CHARACTER_IN_SQL, " $indexUk.$indexKey"), "", false)
                    }
                    newPagedTemp.addAll(s)
                }
            }
            sqlTemp.add(SqlTemp("", whereSql, "", false))
            sqlTemp.addAll(newPagedTemp)
        } else {
            sqlTemp.add(SqlTemp("", whereSql, "", false))
        }
    }



    fun getTotalCountSql(sqlTemp: LinkedList<SqlTemp>, whereSql: String?, select: Set<ConditionTerm>?): String {
        //创造关键uk
        val includeKeys = mutableSetOf<String>()

        select?.mapNotNull { it.sUk }?.apply {includeKeys.addAll(this) }
        //创造分页语句
        val temp = LinkedHashSet<SqlTemp>()
        //先把主句柄加进来
        val mainSql = requireNotNull( sqlTemp.find { it.sql.contains(FIELDS_CHARACTER_IN_SQL) } ){"the sql select count lost finalWhere word"}
        temp.add(mainSql)
        //迭代添加包含关键uk的句柄
        filterSql(temp, sqlTemp, includeKeys)
        val newSqlTemp = LinkedHashSet<SqlTemp>()
        newSqlTemp.addAll(sqlTemp)
        val iterator=newSqlTemp.iterator()
        while (iterator.hasNext()){
            val next=iterator.next()
            if(!temp.contains(next)){
                iterator.remove()
            }
        }
        whereSql?.apply {  newSqlTemp.add(SqlTemp("", this, "", false))}
        val sql = StringBuffer()
        newSqlTemp.forEach { sql.append(it.sql.replace(UK_CHARACTER_IN_SQL, it.uk)) }
        return sql.toString()
    }



    fun makeConditionValue(arithmetic: Arithmetic, vararg values: String): String {
        val v = values.map { "'$it'" }
        return make(arithmetic, v)
    }

    fun makeConditionValue(arithmetic: Arithmetic, vararg values: Int): String {
        val v = values.map { "$it" }
        return make(arithmetic, v)

    }

    fun makeConditionValue(arithmetic: Arithmetic, vararg values: Boolean): String {
        val v = values.map { "$it" }
        return make(arithmetic, v)
    }

    private fun make(arithmetic: Arithmetic, v: List<String>): String {
        return when (arithmetic) {
            Arithmetic.LESS , Arithmetic.EQUAL, Arithmetic.LIKE -> {
                if (v.size > 1 || v.isEmpty())
                    throw IllegalArgumentException("Arithmetic.EQUAL must compare with just one value")
                "  ${arithmetic.string} ${v[0]} "
            }
            Arithmetic.IN -> {
                "  ${arithmetic.string} (${v.stuffToString()}) "
            }
            Arithmetic.BETWEEN -> {
                if (v.size !=2)
                    throw IllegalArgumentException("Arithmetic.BETWEEN must compare with just two value")
                "  ${arithmetic.string} ${v[0]} ${Link.AND} ${v[1]} "
            }


            else -> throw IllegalArgumentException("暂不支持")
        }
    }

}