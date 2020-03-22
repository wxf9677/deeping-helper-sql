package com.diving.wsql.bean


//数据构造条件
class Redirect(
        //当前的uk
        val uk: String,
        //目标的uk
        val tUk: String,
        //查询的name
        val fieldName: String,
        //为查询的name赋值
        val wrapperValue: String?

)
