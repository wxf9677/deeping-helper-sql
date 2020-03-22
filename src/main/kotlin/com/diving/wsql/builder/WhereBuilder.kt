package com.diving.wsql.builder

import com.diving.wsql.factory.QuerySqlFactory
import com.diving.wsql.bean.ConditionTerm
import com.diving.wsql.en.Link

/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: where 后面的条件
 * @version:
 **/
class WhereBuilder(private val uk: String, private val sqlFactory: QuerySqlFactory, private val prefix: String, private val doBefore: (partSql: String, pagedSql: String?, indexUk: String?, indexKey: String?, Set<ConditionTerm>) -> Unit) :
    HelpBuilder {
    private var conditionTerms = StringBuffer()
    private val selects = mutableSetOf<ConditionTerm>()

    init {
        conditionTerms.append(prefix)
    }


    fun isConditionTermNull(): Boolean {
        return selects.isNotEmpty()
    }

    fun setConditionTerm(term: ConditionTerm): WhereBuilder {
        if (term.sUk != uk && uk != "any") {
            throw IllegalArgumentException("the sUk must the same with defined uk")
        }
        conditionTerms.append(term.getExpression(sqlFactory))
        selects.add(term)
        return this
    }

    fun setAndConditionTerm(term: ConditionTerm): WhereBuilder {
        conditionTerms.append(Link.AND.string)
        setConditionTerm(term)
        return this
    }

    fun end(): QuerySqlFactory {
        //如果前面只有前缀没有任何条件内容则清空内容
        if (conditionTerms.toString() == prefix) {
            conditionTerms.setLength(0)
        }
        doBefore.invoke(conditionTerms.toString(), null, null, null, selects)
        return sqlFactory
    }

    fun endAndCreateCustomPage(): WherePageCustomBuilder {
        return WherePageCustomBuilder(sqlFactory) { pagedSql, uk, key ->
            if (conditionTerms.toString() == prefix) {
                conditionTerms.setLength(0)
            }
            if (conditionTerms.isEmpty())
                doBefore.invoke(conditionTerms.toString(), " where $pagedSql", uk, key, selects)
            else
                doBefore.invoke(conditionTerms.toString(), " and $pagedSql", uk, key, selects)
        }
    }

    fun endAndCreatePage(): WherePageBuilder {
        return WherePageBuilder(sqlFactory) {
            //如果前面只有前缀没有任何条件内容则清空内容
            if (conditionTerms.toString() == prefix) {
                conditionTerms.setLength(0)
            }
            conditionTerms.append(it)
            doBefore.invoke(conditionTerms.toString(), null, null, null, selects)
        }
    }
}