package com.diving.wsql.builder

import com.diving.wsql.SqlSplitUtils
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
class WherePageBuilder(private val sqlFactory: QuerySqlFactory, private val doBefore:(String)->Unit) : HelpBuilder {
    private var page: Int? = null
    private var size: Int? = null
    private val sorts = mutableSetOf<Triple<String?, String, Direction?>>()
    private var paged = false
    fun setPage(page: Int, size: Int): WherePageBuilder {
        paged = true
        this.page = page
        this.size = size
        return this
    }
    fun setSort(uk: String?, property: String, direction: Direction?): WherePageBuilder {
        sqlFactory.isUkExist(uk, true)
        sorts.add(Triple(uk, property, direction))
        return this
    }
     fun end(): QuerySqlFactory {
         sorts.forEach {
             sqlFactory.isUkExist(it.first, true)
         }
         val pagedSql = if (paged) {
             requireNotNull(page ){"page is needed,please setPage first"}
             requireNotNull(size){"size is needed,please setPage first"}
             val pagedSql = StringBuffer(SqlSplitUtils.makeOrderSql("", sorts))
             val offset = page!! * size!!
             pagedSql.append(" limit ")
             pagedSql.append(offset)
             pagedSql.append(",")
             pagedSql.append(size!!)
             pagedSql.toString()
         } else {
             SqlSplitUtils.makeOrderSql("", sorts)
         }
         doBefore.invoke(pagedSql)
         return sqlFactory
    }


}