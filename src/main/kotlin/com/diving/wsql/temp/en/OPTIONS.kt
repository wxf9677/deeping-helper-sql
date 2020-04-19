package com.diving.wsql.temp.en

import com.diving.wsql.Utils
import java.lang.reflect.Field

//数据构造对象
class OPTIONS(
        val sql: String,
        val query: MutableList<QP>,
        val superQp: QP
)