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
class WhereTermBuilder<T: HelpBuilder>(private val builder: T, private val sqlFactory: QuerySqlFactory, private val prefix:String, private val  doBefore:(String, Set<ConditionTerm>)->Unit):
    HelpBuilder {
    protected var conditionTerms = StringBuffer()

    private val selects= mutableSetOf<ConditionTerm>()
    init {
        conditionTerms.append(prefix)
    }

    private fun addTerm(string: String) {
        conditionTerms.append(string)
    }



    fun setConditionTerm(term: ConditionTerm): WhereTermBuilder<T> {
        selects.add(term)
        addTerm(term.getExpression(sqlFactory))
        return this
    }


    fun setAndConditionTerm(term: ConditionTerm): WhereTermBuilder<T> {
        conditionTerms.append(Link.AND.string)
        setConditionTerm(term)
        return this
    }

    private fun doBefore() {
        if (conditionTerms.toString() == prefix)
            doBefore.invoke("",selects)
        else {
            doBefore.invoke(conditionTerms.toString(),selects)
        }
    }

     fun end(): T {
        doBefore()
        return builder
    }


}