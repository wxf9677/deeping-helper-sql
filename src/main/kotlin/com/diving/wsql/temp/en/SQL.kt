package com.diving.wsql.temp.en

import com.diving.wsql.core.stuffToString
import com.diving.wsql.en.Operate


//  join left  select distinct * from tablename where  order by limit  on .....
//数据构造对象
class SQL(
        val join: String="",
        val select: Operate=Operate.SELECT,
        val distinct: String="",
        var params: String="",
        val tableName: String="",
        var uk: String="",
        var where: String?=null,
        val term :String="",
        val terms: List<Condition>

) {


    private fun getTName(): String {

        return if(tableName.isNotEmpty()){
            "from $tableName"
        }else{
            ""
        }
    }


    fun make(): String {
       return if (join.isNotEmpty()) {
           "$join (${select.string} $distinct $params  ${getTName()} $uk ${where?:""}) $uk $term ${terms.map { it.make() }.stuffToString(" and ")}"
       }else {
           "${select.string} $distinct $params ${getTName()} $uk ${where?:""} "
       }

    }
}