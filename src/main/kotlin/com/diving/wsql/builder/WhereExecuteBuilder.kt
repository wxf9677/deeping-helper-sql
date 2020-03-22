package com.diving.wsql.builder

import com.diving.wsql.bean.ExecuteConditionTerm
import com.diving.wsql.en.Link
import com.diving.wsql.factory.ExecuteSqlFactory
/**
 * @package: com.diving.wsql.builder
 * @createAuthor: wuxianfeng
 * @createDate: 2019-09-16
 * @createTime: 01:59
 * @describe: where 后面的条件
 * @version:
 **/
class WhereExecuteBuilder(private val sqlFactory: ExecuteSqlFactory, private val prefix: String, private val doBefore: (partSql: String) -> Unit) : HelpBuilder {
    private var conditionTerms = StringBuffer()

    init {
        conditionTerms.append(prefix)
    }

    private fun addTerm(string: String) {
        conditionTerms.append(string)
    }


    fun setConditionTerm(term: ExecuteConditionTerm): WhereExecuteBuilder {
        addTerm(term.getExpression(sqlFactory))
        return this
    }

    fun setAndConditionTerm(term: ExecuteConditionTerm): WhereExecuteBuilder {
        conditionTerms.append(Link.AND.string)
        setConditionTerm(term)
        return this
    }

    fun end(): ExecuteSqlFactory {
        //如果前面只有前缀没有任何条件内容则清空内容
        if (conditionTerms.toString() == prefix) {
            conditionTerms.setLength(0)
        }
        doBefore.invoke(conditionTerms.toString())
        return sqlFactory
    }
}