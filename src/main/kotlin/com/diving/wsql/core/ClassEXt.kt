package com.diving.wsql.core

import java.io.File
import java.io.FileFilter
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.net.URL
import java.net.URLDecoder
import java.util.*

/**
 * @Description: 获取class中的成员变量
 * @Date: 20-4-10 下午1:35
 * @Author: wxf
 * @Version: 1.0
 * @Param
 * @Return
 */
fun Class<*>.getFieldsRecursive(): MutableList<Field> {
    val fieldList = mutableListOf<Field>()
    var tempClass: Class<*>? = this
    while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
        fieldList.addAll(tempClass.declaredFields)
        tempClass = tempClass.superclass //得到父类,然后赋给自己
    }
    return fieldList
}

fun Class<*>.getMethodsRecursive(): MutableList<Method> {
    val methodList = mutableListOf<Method>()
    var tempClass: Class<*>? = this
    while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
        methodList.addAll(tempClass.declaredMethods)
        tempClass = tempClass.superclass //得到父类,然后赋给自己
    }
    return methodList
}


fun Class<*>.getFieldClassRecursive(): MutableList<Class<*>> {
    val methodList = mutableListOf<Class<*>>()
    var tempClass: Class<*>? = this
    while (tempClass != null) {//当父类为null的时候说明到达了最上层的父类(Object类).
        methodList.add(tempClass)
        tempClass = tempClass.superclass //得到父类,然后赋给自己
    }
    return methodList
}


fun Class<*>.getInterfaceRecursive(): MutableList<Class<*>> {
    val interfaces = mutableListOf<Class<*>>()
    recursiveInterface(this, interfaces)
    return interfaces
}


private fun recursiveInterface(clazz: Class<*>, list: MutableList<Class<*>>) {
    var interfaces: Array<out Class<*>> = clazz.interfaces ?: return
    list.addAll(interfaces)
    interfaces?.forEach {
        recursiveInterface(it, list)
    }
}

/**
 * @Description: 根据报名获取包下面的类
 * @Date: 20-4-10 下午1:36
 * @Author: wxf
 * @Version: 1.0
 * @Param
 * @Return
 */
fun String?.loadClasses(): MutableSet<Class<*>> {
    val clazzs = mutableSetOf<Class<*>>()
    requireNotNull(this) { "please initialDtoPackage first" }
    // 是否循环搜索子包
    val recursive = true
    // 包名对应的路径名称
    val packageDirName = this!!.replace('.', '/')
    val dirs: Enumeration<URL>

    try {
        dirs = Thread.currentThread().contextClassLoader.getResources(packageDirName)
        while (dirs.hasMoreElements()) {
            val url = dirs.nextElement()
            val protocol = url.protocol

            if ("file" == protocol) {
                val filePath = URLDecoder.decode(url.file, "UTF-8")
                findClassInPackageByFile(this!!, filePath, recursive, clazzs)
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return clazzs
}


fun findClassInPackageByFile(packageName: String, filePath: String, recursive: Boolean, clazzs: MutableSet<Class<*>>) {
    val dir = File(filePath)
    if (!dir.exists() || !dir.isDirectory) {
        return
    }
    // 在给定的目录下找到所有的文件，并且进行条件过滤
    val dirFiles = dir.listFiles(object : FileFilter {
        override fun accept(file: File): Boolean {
            val acceptDir = recursive && file.isDirectory// 接受dir目录
            val acceptClass = file.name.endsWith("class")// 接受class文件
            return acceptDir || acceptClass
        }
    })

    for (file in dirFiles) {
        if (file.isDirectory) {
            findClassInPackageByFile(packageName + "." + file.name, file.absolutePath, recursive, clazzs)
        } else {
            val className = file.name.substring(0, file.name.length - 6)
            try {
                clazzs.add(Thread.currentThread().contextClassLoader.loadClass("$packageName.$className"))
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}