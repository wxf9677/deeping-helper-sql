package com.diving.wsql.core
import java.util.*
    private fun getUniqueKey(): String {
        return UUID.randomUUID().toString().let { it.replace("-","") }
    }
    fun uniKey(len:Int?=null):String{
       val k= ("${System.currentTimeMillis()}${getUniqueKey()}")
      return  if(len==null){
            k
        }else{
            k.takeLast(len)
        }

    }
