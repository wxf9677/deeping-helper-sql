package com.diving.wsql.bean



//数据构造条件
class QueryTerm(
        val  uk: String,
        val  mountUk: String,
        val  mountFieldKey: String,
        val  clazz: Class<*>?)
