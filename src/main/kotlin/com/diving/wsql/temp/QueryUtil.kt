package com.diving.wsql.temp

import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.builder.FIELDS_CHARACTER_IN_SQL
import com.diving.wsql.helpr.NativeSqlHelper
import com.diving.wsql.temp.en.OPTIONS
import java.math.BigInteger
import javax.persistence.EntityManager

class QueryUtil(val entityManager: EntityManager) {

    fun <T>query(o: OPTIONS): List<T> {
        return QueryContext().query(o,entityManager)
    }

    fun queryCount(o: String): Long {
        return QueryContext().queryCount(o,entityManager)
    }

}