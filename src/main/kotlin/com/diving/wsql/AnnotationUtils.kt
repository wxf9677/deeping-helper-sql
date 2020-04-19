package com.diving.wsql

import java.lang.reflect.Field

object AnnotationUtils {

    fun<A:Annotation> findAnnotation(clazz: Class<*>, annotationClass:Class<A>): A?{
       return clazz.getAnnotation(annotationClass)
    }
    fun<A:Annotation> findAnnotation(clazz: Field, annotationClass:Class<A>): A? {
        return clazz.getAnnotation(annotationClass)
    }
}