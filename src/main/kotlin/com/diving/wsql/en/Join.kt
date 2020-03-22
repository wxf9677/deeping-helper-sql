package com.diving.wsql.en

enum class Join(val s:String){
    RIGHT(" right join "),//右链接
    RIGHT_OUTER(" right outer join "),//右链接
    LEFT(" left join "),//左链接
    LFET_OUTER(" left outer join "),//左链接
    INNER(" inner join "),// 内链接
    FULL(" full join ")//全链接
}