package com.diving.wsql.temp.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Target(ElementType.METHOD, ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
//sqlFieldName 数据库字段名
annotation class SqlFiledName(val sqlFieldName:String)
