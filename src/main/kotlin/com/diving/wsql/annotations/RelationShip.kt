package com.diving.wsql.annotations

import com.diving.wsql.en.Oprerate
import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Documented
@Target( ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
annotation class RelationShip()
