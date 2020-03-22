package com.diving.wsql


import com.diving.wsql.builder.MOUNTKEY_SPLIT
import com.diving.wsql.builder.OBJ_SPLIT
import com.diving.wsql.core.checkException
import com.diving.wsql.core.getFieldsRecursive
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.sql.Timestamp

object Utils {

    fun formatSqlField(field: String): String {
        val f = StringBuffer()
        val chars = field.toCharArray()
        chars.forEachIndexed { index: Int, c: Char ->
            if (index != 0 && index != chars.size - 1 && !c.isLowerCase() && c.isLetter()) {
                f.append("_${c.toLowerCase()}")
            } else {
                f.append(c)
            }
        }
        return String(f)
    }

    fun formatHumpField(field: String): String {
        val f = StringBuffer()
        val chars = field.toCharArray()
        chars.forEachIndexed { index: Int, c: Char ->
            if (c != '_') {
                if (index != 0 && index != chars.size - 1 && chars[index - 1] == '_') {
                    f.append("${c.toUpperCase()}")
                } else {
                    f.append(c)
                }
            }
        }
        return String(f)
    }


    fun makeMountFieldKey(uk: String, fieldName: String): String {
        require(!(uk.contains(MOUNTKEY_SPLIT) || fieldName.contains(MOUNTKEY_SPLIT))) { "the uk or fieldName can not contains a char with $MOUNTKEY_SPLIT" }
        return "$uk$MOUNTKEY_SPLIT$fieldName"
    }

    fun makeSingleSubClassKey(gd: com.diving.wsql.bean.QD): String {
        require(!(gd.mountUk.contains(OBJ_SPLIT) || gd.mountFieldKey.contains(OBJ_SPLIT))) { "the mountUk or mountFieldKey can not contains a char with $OBJ_SPLIT" }
        return "${gd.mountUk}$OBJ_SPLIT${gd.mountFieldKey}"
    }


    fun makeSingleClassKey(gd: com.diving.wsql.bean.QD): String {
        return "${gd.mountUk}$OBJ_SPLIT${gd.clazz.name}"
    }


    fun isFieldExist(supperClazz: Class<*>, fieldName: String, clazz: Class<*>): Boolean {

        val fields = supperClazz.getFieldsRecursive()
        val field = requireNotNull(fields.find { it.name == fieldName }) { "the fieldName :$fieldName is not in class ${supperClazz.simpleName}" }
        require(checkClazzType(field, clazz).first) { "the clazz from fieldNa" +
            "me:$fieldName type is not fixed " +
            "the Field named $fieldName's type is ${field.type.name}, " +
            "but class's type is ${clazz.simpleName}" }
        return true
    }

    fun checkClazzType(f: Field?, clazz: Class<*>?): Pair<Boolean, Boolean> {
        f ?: return false to false
        clazz ?: return false to false
        return if (f.type == List::class.java) {
            // 如果是List类型，得到其Generic的类型
            val genericType = f.genericType;
            // 如果是泛型参数的类型
            if (genericType is ParameterizedType) {
                //得到泛型里的class类型对象
                val classType = genericType.actualTypeArguments[0]
                if (classType == Integer::class.java && clazz == Int::class.java) {
                    true to true
                } else {
                    classType.typeName.contains(clazz.name) to true
                }
            } else {
                (genericType == clazz) to true
            }
        } else {
            (f.type == clazz) to false
        }
    }


    fun getClazzType(f: Field): Class<*> {
        return if (f.type == List::class.java) {
            // 如果是List类型，得到其Generic的类型
            val genericType = f.genericType;
            // 如果是泛型参数的类型
            if (genericType is ParameterizedType) {
                //得到泛型里的class类型对象
                val classType = genericType.actualTypeArguments[0]
                if (classType == Integer::class.java)
                    Int::class.java
                else
                    Class.forName(classType.typeName.replace("? extends", "").trim())
            } else {
                Class.forName(genericType.typeName.replace("? extends", "").trim())
            }
        } else {
            f.type
        }

    }


    fun isPrimitive(f: Field): Boolean {
        return f.type.isPrimitive || f.type.simpleName == "String" || f.type.simpleName == "Integer" || f.type.simpleName == "Long"
    }

    fun isPrimitive(c: Class<*>): Boolean {
        return c.isPrimitive || c.simpleName == "String" || c.simpleName == "Integer" || c.simpleName == "Long"
    }


    fun formatValue(field: Field, value: Any?): Any? {
        return checkException({
            if ((field.type.simpleName == "boolean" || field.type.simpleName == "Boolean") && value is Number) {
                val iv = value.toInt()
                if (iv != 0 && iv != 1) {
                    throw IllegalArgumentException("field.type is Boolean, but the value not fixed")
                } else {
                    iv != 0
                }
            } else if ((field.type.simpleName == "long" || field.type.simpleName == "Long") && value is Timestamp)
                value.time
            else if ((field.type.simpleName == "integer" || field.type.simpleName == "Integer") && value is Byte)
                value.toInt()
            else if ((field.type.simpleName == "long" || field.type.simpleName == "Long") && value is Number)
                value.toLong()
            else if ((field.type.simpleName == "string" || field.type.simpleName == "String") && value is Number)
                value.toString()
            else
                value
        })
    }


    fun checkValueFix(values: Any?, keys: Collection<*>) {
        when (values) {
            null -> {
                throw IllegalArgumentException("sql result not fixed")
            }
            is Array<*> -> {
                require(values.size == keys.size) { "sql result not fixed" }
            }
            is Collection<*> -> {
                require(values.size == keys.size) { "sql result not fixed" }
            }
            else->{
                require(1 == keys.size) { "sql result not fixed" }
            }
        }


    }
}