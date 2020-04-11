package com.diving.wsql.temp.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target
@Target( ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class SqlRedirect(val uk:String, val customValue:String)
