package com.diving.wsql.helpr
import com.diving.wsql.core.checkException
import javax.persistence.EntityManager
class ExecuteSqlHelper(private val sql: String, private val entityManager: EntityManager) {
    fun execute(): Int {
        return checkException( { entityManager.createNativeQuery(sql).executeUpdate() }) ?:0
    }
}