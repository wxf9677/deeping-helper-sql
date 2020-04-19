package com.diving.wsql.temp

import java.math.BigInteger
import javax.persistence.EntityManager


class ExecuteContext {

    fun execute(o: String, entityManager: EntityManager): Int {
        val query = entityManager.createNativeQuery(o)
        return  query.executeUpdate()
    }
}