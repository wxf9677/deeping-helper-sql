package com.diving.wsql.temp.annotations;



import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlQuery {
    //这个uk只有在做主类查询的时候才会有用
    String uk();
    String tableName();
    boolean distinct() default false;
}

