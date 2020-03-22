package com.diving.wsql.factory

import com.diving.wsql.helpr.NativeSqlHelper
import java.math.BigInteger
import javax.persistence.EntityManager

class NativeSqlFactory(private val sql: String = "", private val entityManager: EntityManager) {



    fun query(): MutableList<Any?> {
        return NativeSqlHelper(sql,entityManager).nativeQuery()
    }

    fun count(): Long {
        return (query().first() as BigInteger).toLong()
    }
}

