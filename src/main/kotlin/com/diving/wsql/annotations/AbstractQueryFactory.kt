package com.diving.wsql.annotations

import com.diving.wsql.core.getMethodsRecursive
import com.diving.wsql.core.loadClasses
import org.springframework.core.annotation.AnnotationUtils

class AbstractQueryFactory : QueryFactory {


    private var dtoPackageName: String? = null
    private var daoPackageName: String? = null

    private var clazzs: MutableSet<Class<*>>? = null

    private var sqlAssertContext: MutableMap<String, String> = mutableMapOf()


    /**
     * @Description: 初始化dto包
     * @Date: 20-4-10 上午11:08
     * @Author: wxf
     * @Version: 1.0
     * @Param
     * @Return
     */
    fun initialDtoPackage(packageName: String) {
        this.dtoPackageName = packageName
    }

    fun initialDaoPackage(packageName: String) {
        this.daoPackageName = packageName
    }

    fun loadClasses() {
        clazzs = dtoPackageName.loadClasses()
    }


    fun filterQueryDtos() {
        clazzs?.filter { it.isAnnotationPresent(Query::class.java) }?.forEach { sss2(it) }
    }


    fun sss2(clazz: Class<*>) {
        val methods = clazz.getMethodsRecursive().filter { it.isAnnotationPresent(Execute::class.java) }



        methods.forEach {
            sqlAssertContext["${clazz.name}$${it.name}"]=SqlTemplateIml().makeSql(clazz,it)
        }


        val key= clazz.simpleName


    }


}