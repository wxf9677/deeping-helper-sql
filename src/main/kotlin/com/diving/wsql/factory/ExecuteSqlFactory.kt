package com.diving.wsql.factory

import com.diving.wsql.helpr.ExecuteSqlHelper
import com.diving.wsql.builder.*

import javax.persistence.EntityManager

//更改工厂包括删除和修改
class ExecuteSqlFactory(private val entityManager: EntityManager) {

    private var sql: String = ""
    private var action: Int? = null
    internal fun appendSql(sql: String) {
        this.sql = sql
    }

    fun insertWithEntity(): com.diving.wsql.builder.InsertBuilder {
        action = 0
        return InsertBuilder(this)
    }

    fun insertWithKeyAndValue(): com.diving.wsql.builder.InsertBuilder2 {
        action = 0
        return InsertBuilder2(this)
    }


    fun delete(): com.diving.wsql.builder.DeleteBuilder {
        action = 1
        return DeleteBuilder(this)
    }

    fun update(): com.diving.wsql.builder.UpdateBuilder {
        action = 2
        return UpdateBuilder(this)
    }

    fun execute(): Int {
        if (sql.isNullOrEmpty()) {
            throw IllegalArgumentException("sql is not define")
        }
        return ExecuteSqlHelper(sql, entityManager).execute()
    }


}

