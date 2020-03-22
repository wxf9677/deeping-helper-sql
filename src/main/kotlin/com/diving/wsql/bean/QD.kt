package com.diving.wsql.bean

import java.lang.reflect.Field

//数据构造对象
class  QD(
        //查询的uk这个有可能会变
        val uk:String,
        //查询的uk是固定不变的
        val fixUk:String,
        //查询的fieldName重命名
        val alias: String,
        //查询的field
        val field: Field,
        //查询的field所在的类
        val clazz: Class<*>,
        //查询的field所在的类是否是Collection
        val isCollection: Boolean,
        //这个类挂载的位置
        val mountUk:String,
        //这个类在挂载位置的key，用这个key查找field位置
        val mountFieldKey:String,
        //如果isCustom为true则不给他分配uk
        val isCustom: Boolean =false
)