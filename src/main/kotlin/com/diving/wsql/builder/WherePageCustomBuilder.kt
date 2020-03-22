package com.diving.wsql.builder

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.en.Direction
import com.diving.wsql.factory.QuerySqlFactory

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: where后面分页
 * @version:
 **/
class WherePageCustomBuilder(
    private val sqlFactory: QuerySqlFactory,
    private val doBefore: (pagedSql: String, uk: String, key: String) -> Unit
) :
    HelpBuilder {
    private var page: Int? = null
    private var size: Int? = null
    private var indexUk: String? = ""
    private var indexKey: String? = ""
    private var indexSort: Direction = Direction.DESC
    private val sorts = linkedSetOf<Triple<String?, String, Direction?>>()
    private var paged = false
    fun setPage(page: Int, size: Int): WherePageCustomBuilder {
        paged = true
        this.page = page
        this.size = size
        return this
    }


    fun setSort(uk: String?, property: String, direction: Direction?): WherePageCustomBuilder {
        sqlFactory.isUkExist(uk, true)
        sorts.add(Triple(uk, property, direction))
        return this
    }

    //这个方法是分页的扩展的方法设置后可以根据自己的要求进行分页
    fun setPageWrapper(indexUk: String, indexKey: String, sort: Direction = Direction.DESC): WherePageCustomBuilder {
        sqlFactory.isUkExist(indexUk, true)
        paged = true
        this.indexUk = indexUk
        this.indexKey = indexKey
        this.indexSort = sort
        return this
    }


    fun end(): QuerySqlFactory {
        sorts.forEach {
            sqlFactory.isUkExist(it.first, true)
        }
        val pagedSql = if (paged) {
            requireNotNull(page) { "page is needed,please setPage first" }
            requireNotNull(size) { "size is needed,please setPage first" }
            requireNotNull(indexKey) { "indexKey is needed,please linkToUkAndTableName first" }
            indexKey = Utils.formatSqlField(indexKey!!)
            val offset = page!! * size!!
            val pagedSql = "${SqlSplitUtils.makePageWithIndex(indexKey!!, indexUk!!, indexSort, offset, size!!)}"
            SqlSplitUtils.makeOrderSql(pagedSql, sorts)
        } else {
            SqlSplitUtils.makeOrderSql("", sorts)
        }
        doBefore.invoke(pagedSql, indexUk!!, indexKey!!)
        return sqlFactory
    }
}