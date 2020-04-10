package com.diving.wsql.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target
@Documented
@Target( ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class Exclude()
