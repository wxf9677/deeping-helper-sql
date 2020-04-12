package com.diving.wsql.temp.en

import com.diving.wsql.Utils
import java.lang.reflect.Field

//数据构造对象
class QP(
        //查询的uk这个有可能会变
        val uk: String,
        //挂载的uk
        val mountUk: String,
        //查询的uk是固定不变的
        val fixUk: String,
        //查询的fieldName重命名
        val sqlFieldName: String,
        //查询的field（只有在装载主类的时候为空）
        val field: Field?,
        //当前field父field（只有在装载主类或者是挂载到主类的时候为空）
        val mountField: Field?,
        //查询的field所在的类是否是Collection
        val isCollection: Boolean,
        //如果isCustom为true则不给他分配uk
        val isCustom: Boolean = false,
        //最顶层类，即查询类
        val superClazz: Class<*>
) {

    fun isSuper(): Boolean {
        return field==null
    }

    fun getMountFieldClass():Class<*>{
       return this.mountField?.let {  Utils.getClazzType(it)}?:superClazz
    }
    fun getMountFieldName():String{
        return this.mountField?.name?:""
    }

}