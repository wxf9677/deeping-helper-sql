package com.diving.wsql

import com.google.gson.Gson
import com.google.gson.JsonElement
import java.lang.reflect.Type

object GsonUtil {

   private val gson: Gson by lazy { Gson() }

   fun <T>fromJson(str:String?,clazz: Class<T>): T {

       return  if(str.isNullOrEmpty()){
           gson.fromJson("{}",clazz)
       }else {
           gson.fromJson(str, clazz)
       }
   }

    fun <T>fromJson(str:String?,type: Type): T {
        return  if(str.isNullOrEmpty()){
            gson.fromJson("{}",type)
        }else {
            gson.fromJson(str, type)
        }
    }

    fun <T>fromJson(json: JsonElement?, type: Type): T {
        return gson.fromJson(json,type)
    }


    fun toJson(clazz: Any?): String{
        return gson.toJson(clazz)
    }
}