package com.diving.wsql

import com.diving.wsql.core.checkException
import com.google.gson.*
import java.lang.reflect.Type

object GsonUtil {

   private val gson: Gson by lazy { Gson() }
    private val disableHtmlEscapingGson: Gson by lazy {    GsonBuilder().disableHtmlEscaping().create() }


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

    fun <T> fromJsonAny(result: Any?, clazz: Class<T>): T? {
        if(result is String) {
        return fromJson(result,clazz)
        }
        if(result is JsonElement) {
            return fromJson(result,clazz)
        }
        return null

    }


    fun toJson(clazz: Any?): String{
        return gson.toJson(clazz)
    }
    fun toJsonDisableHtmlEscaping(clazz: Any?): String{
       return disableHtmlEscapingGson.toJson(clazz)
    }




     fun getToInt( json: JsonElement?,key: String,default: Int=0) :Int{
        return checkException( { json?.asJsonObject?.get(key)?.asInt?:0 } )?:default
    }


     fun getToLong( json: JsonElement?,key: String,default: Long=0) :Long{
        return checkException(  { json?.asJsonObject?.get(key)?.asLong?:0 })?:default
    }

     fun getToInt(json:JsonObject?,key: String ,default: Int=0) :Int{
        return checkException(  { json?.get(key)?.asInt?:0 })?:default
    }

     fun getToBoolean(json:JsonObject?,key: String,default: Boolean=false) :Boolean{
        return checkException(  { json?.get(key)?.asBoolean?:false })?:default
    }


     fun getToLong(json:JsonObject?,key: String,default: Long=0) :Long{
        return checkException(  { json?.get(key)?.asLong?:0L })?:default
    }

     fun getToString(json:JsonObject?,key: String,default: String="") :String{
        return checkException(  { json?.get(key)?.asString?:"" })?:default
    }

     fun getToString(json: JsonElement?, key: String,default: String="") :String{
        return checkException( { json?.asJsonObject?.get(key)?.asString?:"" })?:default

    }
     fun getToJsonArray(json:JsonObject?,key: String,default: JsonArray= JsonArray()) : JsonArray {
        return checkException(  { json?.get(key)?.asJsonArray?: JsonArray() })?:default
    }



}