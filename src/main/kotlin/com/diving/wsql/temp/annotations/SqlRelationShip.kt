package com.diving.wsql.temp.annotations

import java.lang.annotation.*
import java.lang.annotation.Retention
import java.lang.annotation.Target

@Target( ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
/**
 * @package: com.diving.wsql.temp.annotations
 * @createAuthor: wuxianfeng
 * @createDate: 2020-04-11
 * @createTime: 12:28
 * @describe: 描述
 * @version: 注解查询实体中独立类的关系
 **/
annotation class SqlRelationShip()
