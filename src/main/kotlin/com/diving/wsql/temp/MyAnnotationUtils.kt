package com.diving.wsql.temp

import com.diving.wsql.core.getMethodsRecursive
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Field
import java.lang.reflect.Method

object MyAnnotationUtils {

    fun<A : Annotation> findAnnotation(clazz: Class<*>, annotationType:Class<A> ): A? {
      return  AnnotationUtils.findAnnotation(clazz, annotationType)
    }

    fun<A : Annotation> findAnnotation(method: Method, annotationType:Class<A> ): A? {
       return AnnotationUtils.findAnnotation(method, annotationType)
    }
    fun<A : Annotation> findAnnotation(clazz: Class<*>,field:Field, annotationType:Class<A> ): A? {
        val method=clazz.getMethodsRecursive().find { it.name=="${field.name}\$annotations" }?:return null
        return AnnotationUtils.findAnnotation(method, annotationType)
    }
}