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
 * @describe:
 * @version:
 **/
class InsertBuilder2(private val sqlFactory: ExecuteSqlFactory) : HelpBuilder {

    private var tableName: String? = null

    private var keyAndValues: MutableSet<Pair<String,Any>> = mutableSetOf()


    fun setTableName(tableName: String): InsertBuilder2 {
        this.tableName = tableName
        return this
    }

    fun setMap(kv:Pair<String,Any>): InsertBuilder2 {
        keyAndValues.add(kv)
        return this
    }

    fun setMaps(kv:List<Pair<String,Any>>): InsertBuilder2 {
        keyAndValues.addAll(kv)
        return this
    }


    private fun doBefore() {
        requireNotNull(tableName) { "tableName is needed,please setTableName first" }
        val f = keyAndValues.map { Utils.formatSqlField(it.first) } .stuffToString()
        val v = keyAndValues.map {
            when (val v =it.second) {
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