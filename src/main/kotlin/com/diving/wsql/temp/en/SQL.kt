package com.diving.wsql.temp.en


// select distinct * from tablename where  order by limit
//数据构造对象
class SQL(
        val join: String="",
        val select: String="",
        val distinct: String="",
        val params: String="",
        val tableName: String="",
        val uk: String="",
        val where: String="",
        val term: String=""

) {
    fun make(): String {
       return if (join .isNotEmpty())
            "$join ($select $distinct $params from $tableName $where) $uk   $term"
        else
            "$select $distinct $params from $tableName $uk $where "

    }
}