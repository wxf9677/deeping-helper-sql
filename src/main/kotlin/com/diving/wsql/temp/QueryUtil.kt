package com.diving.wsql.temp

import com.diving.wsql.temp.en.OPTIONS
import javax.persistence.EntityManager

class QueryUtil(val entityManager: EntityManager) {

    fun <T>query(o: OPTIONS<T>): List<T> {
        return QueryContext<T>().query(o,entityManager)
    }

    fun queryCount(o: String): Long {
        return QueryCountContext().queryCount(o,entityManager)
    }

}