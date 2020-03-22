package com.diving.wsql.core

import java.lang.reflect.Field

inline fun Class<*>.getFieldsRecursive(): MutableList<Field> {
    val fieldList = mutableListOf<Field>()
    var tempClass:Class<*>? = this
    while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
        fieldList.addAll(tempClass.declaredFields)
        tempClass = tempClass.superclass //得到父类,然后赋给自己
    }
    return fieldList
}