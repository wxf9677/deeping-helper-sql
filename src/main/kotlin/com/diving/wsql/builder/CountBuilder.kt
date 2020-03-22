package com.diving.wsql.builder
import com.diving.wsql.en.Oprerate
import com.diving.wsql.factory.QuerySqlFactory
/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: 开始查询
 * @version:
 **/
class CountBuilder(private val sqlFactory: QuerySqlFactory) : HelpBuilder {

    private var tableName: String? = null
    private var uk: String? = null

    fun setTableName(uk: String, tableName: String): CountBuilder {
        this.uk = uk
        this.tableName = tableName
        sqlFactory.setUkAndName(uk, tableName,null)
        return this
    }

    private fun doBefore() {
        requireNotNull(tableName){"tableName is needed,please setTableName first"}
        requireNotNull(uk){"uk is needed,please setUk first"}
        val sql = "${Oprerate.SELECT.string}  $FIELDS_CHARACTER_IN_SQL from $tableName $UK_CHARACTER_IN_SQL "
        sqlFactory.appendSql(uk!!, sql,tableName!!,true)
    }

    fun end(): QuerySqlFactory {
        doBefore()
        return sqlFactory
    }

}