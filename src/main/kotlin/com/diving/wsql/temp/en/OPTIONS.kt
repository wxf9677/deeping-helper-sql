package com.diving.wsql.temp.en

import com.diving.wsql.Utils
import com.diving.wsql.bean.QueryDto
import java.lang.reflect.Field

//数据构造对象
class OPTIONS<T>(
        val sql: String,
        val query: MutableList<QP>,
        val superQp: QP
)