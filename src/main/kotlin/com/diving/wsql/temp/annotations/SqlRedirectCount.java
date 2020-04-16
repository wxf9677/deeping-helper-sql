package com.diving.wsql.temp.annotations;

import com.diving.wsql.en.Arithmetic;
import com.diving.wsql.en.Join;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlRedirectCount {
    String tableName();
    Join join();
    String uk();
    String fieldName();
    Arithmetic arithmetic();
    String targetUk();
    String targetFieldName();
}
