package com.diving.wsql.builder

import com.diving.wsql.Utils
import com.diving.wsql.core.stuffToString
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
class UpdateBuilder(private  val sqlFactory: ExecuteSqlFactory) : HelpBuilder {

    private var tableName: String? = null
    private var keyAndValue = HashMap<String, Any>()

    fun setTableName(tableName: String): UpdateBuilder {
        this.tableName = tableName
        return this
    }

    fun setFieldAndValue(fieldName: String, value: Any): UpdateBuilder {
        keyAndValue[fieldName] = value
        return this
    }

    fun where(): WhereExecuteBuilder {
        return WhereExecuteBuilder(sqlFactory, "where") { whereSql ->
            if (keyAndValue.isEmpty()) {
                throw IllegalArgumentException("tableName is needed,please setTableName first")
            }
            val f = keyAndValue.map {
                if (it.value is String)
                    "${Utils.formatSqlField(it.key)}= '${it.value}'"
                else {
                    "${Utils.formatSqlField(it.key)}= ${it.value}"
                }
            }.stuffToString()
            val sql = "${Operate.UPDATE}  $tableName SET $f $whereSql"
            sqlFactory.appendSql(sql)
        }
    }
}