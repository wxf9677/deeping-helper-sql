package com.diving.wsql.temp.en

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
import com.diving.wsql.en.Direction
import com.diving.wsql.temp.MakeUtil
import java.util.*
import kotlin.collections.LinkedHashSet

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: where后面分页
 * @version:
 **/
class Where {
   internal val conditionTerms = mutableSetOf<Condition>()
    internal val sorts = mutableSetOf<Triple<String?, String, Direction?>>()
    internal var page: Int = 0
    internal var size: Int = 0
    internal var paged = false
    internal var customPaged = false
    internal var sorted = false

    private var indexUk: String? = ""
    private var indexKey: String? = ""
    private var indexSort: Direction = Direction.DESC

    fun setConditionTerm(condition: Condition): Where {
        conditionTerms.add(condition)
        return this
    }

    fun setPage(page: Int, size: Int): Where {
        paged = true
        this.page = page
        this.size = size
        return this
    }

    fun setCustomPage(indexUk: String,indexKey: String,indexSort:Direction): Where {
        customPaged = true
        this.indexKey=indexKey
        this.indexUk=indexUk
        this.indexSort=indexSort

        return this
    }

    fun setSort(uk: String, property: String, direction: Direction?): Where {
        sorted = true
        sorts.add(Triple(uk, property, direction))
        return this
    }


    fun make(sqlTemp: LinkedHashSet<SQLTEMP>): String {
        val where = if (conditionTerms.isNotEmpty()) {
            " where ${conditionTerms.map { it.make() }.stuffToString("and")}"
        } else {
            ""
        }
        return if (paged && sorted) {
            makePagedSql(where,sqlTemp)
        } else if (paged && !sorted) {
            makePagedSql(where,sqlTemp)
        } else if (!paged && sorted) {
            MakeUtil.makeOrderSql(where, sorts)
        } else {
            where
        }
    }

    fun makeCount(sqlTemp: LinkedHashSet<SQLTEMP>): String {
        val where = if (conditionTerms.isNotEmpty()) {
            " where ${conditionTerms.map { it.make() }.stuffToString("and")}"
        } else {
            ""
        }
       return MakeUtil.getTotalCountSql(sqlTemp,where,conditionTerms)
    }



    private fun makePagedSql(where: String, sqlTemp: LinkedHashSet<SQLTEMP>): String {
        val newSql = StringBuffer()
        val paged= if (customPaged) {
            indexKey = Utils.formatSqlField(indexKey!!)
            val offset = page!! * size!!
            val pagedSql = "${MakeUtil.makePageWithIndex(where,indexKey!!, indexUk!!, indexSort, offset, size!!)}"
            MakeUtil.makePagedSql( MakeUtil.makeOrderSql(pagedSql, sorts), sqlTemp, indexUk, indexKey, conditionTerms)

        } else {
            val pagedSql = StringBuffer(MakeUtil.makeOrderSql("", sorts))
            val offset = page!! * size!!
            pagedSql.append(" limit ")
            pagedSql.append(offset)
            pagedSql.append(",")
            pagedSql.append(size!!)
            pagedSql.toString()
        }

        if(where.isNotEmpty()){
            newSql.append(where)
            newSql.append(" and ")
            newSql.append(paged)
        }else{
            newSql.append(" where ")
            newSql.append(paged)
        }
        return newSql.toString()

    }
}
