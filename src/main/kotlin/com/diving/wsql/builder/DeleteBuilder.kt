package com.diving.wsql.builder

import com.diving.wsql.en.Oprerate
import com.diving.wsql.factory.ExecuteSqlFactory

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 开始查询
 * @version:
 **/
class DeleteBuilder(private val sqlFactory: ExecuteSqlFactory) : HelpBuilder {

    private var tableName: String? = null

    fun setTableName(tableName: String): DeleteBuilder {
        this.tableName = tableName
        return this
    }

    fun where(): WhereExecuteBuilder {
        return WhereExecuteBuilder(sqlFactory, "where") { whereSql ->
            requireNotNull(tableName) { "tableName is needed,please setTableName first" }
            val sql = "${Oprerate.DELETE} from $tableName  $whereSql"
            sqlFactory.appendSql(sql)
        }
    }
}