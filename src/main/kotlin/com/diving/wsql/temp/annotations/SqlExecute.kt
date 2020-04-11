package com.diving.wsql.temp.annotations

import com.diving.wsql.en.Operate
import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Target( ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
annotation class SqlExecute(val action:Operate, val distinct:Boolean=false)
