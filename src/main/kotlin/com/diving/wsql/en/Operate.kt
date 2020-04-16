package com.diving.wsql.en

enum class Operate(val string: String) {
    UPDATE("update"),
    DELETE("delete"),
    SELECT("select "),
    INSERT("insert"),
    NONE("")

}