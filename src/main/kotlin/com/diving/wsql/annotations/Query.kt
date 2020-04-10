package com.diving.wsql.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Documented
@Target( ElementType.FIELD,ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
annotation class Query(val uk:String , val tableName: String)
