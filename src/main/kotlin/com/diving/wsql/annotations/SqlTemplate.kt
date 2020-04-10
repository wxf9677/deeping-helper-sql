package com.diving.wsql.annotations

import java.lang.reflect.Method


interface SqlTemplate{


    fun makeSql(method: Method): String
    fun makeSql(clazz: Class<*>, method: Method): String
}