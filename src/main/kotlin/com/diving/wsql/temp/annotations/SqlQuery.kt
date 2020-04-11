package com.diving.wsql.temp.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Documented
@Target( ElementType.FIELD,ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
annotation class SqlQuery(val uk:String, val tableName: String,val distinct:Boolean=false)
