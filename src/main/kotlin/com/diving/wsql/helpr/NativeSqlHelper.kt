package com.diving.wsql.helpr

import javax.persistence.EntityManager


class NativeSqlHelper(val sql: String, val entityManager: EntityManager) {
    fun  nativeQuery(): MutableList<Any?> {
        val query = entityManager.createNativeQuery(sql)
        return query.resultList
    }

}