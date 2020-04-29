package com.diving.wsql.temp

import com.diving.wsql.GsonUtil
import com.diving.wsql.SqlSplitUtils
import com.diving.wsql.Utils
import com.diving.wsql.Utils.checkValueFix
import com.diving.wsql.bean.QueryDto
import com.diving.wsql.core.checkException
import com.diving.wsql.temp.en.OPTIONS
import com.diving.wsql.temp.en.QP
import com.google.gson.Gson
import java.lang.reflect.Field
import java.math.BigInteger
import javax.persistence.EntityManager


class QueryCountContext {
    fun queryCount(o: String, entityManager: EntityManager): Long {
        val query = entityManager.createNativeQuery(o)
        return  (query.resultList.first() as BigInteger).toLong()
    }
}