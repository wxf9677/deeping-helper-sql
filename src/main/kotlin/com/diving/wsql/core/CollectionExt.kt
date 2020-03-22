package com.diving.wsql.core

import java.math.BigDecimal


/**
 * @ClassName:
 * @Description:    求总
 * @Author:         wuxianfeng
 * @CreateDate:     2019-08-26/20:21
 */
inline fun Collection<Int>.total(): Int {
    var totalSize = 0

    if (this.isEmpty()) {
        return totalSize
    }
    this.forEach {
        totalSize += it
    }
    return totalSize
}


inline fun Collection<BigDecimal>.totalBigDecimal(): BigDecimal {
    var totalSize = BigDecimal(0)

    if (this.isEmpty()) {
        return totalSize
    }
    this.forEach {
        totalSize += it
    }
    return totalSize
}

/**
 * @ClassName:
 * @Description:    把string组合 转成string format {"1,2,3,4,5"}
 * @Author:         wuxianfeng
 * @CreateDate:     2019-08-26/20:18
 */
inline fun Collection<Any>.stuffToString(split:String=","): String {
    if (this.isEmpty()) {
        return ""
    }

    val str = StringBuffer()
    this.forEachIndexed { index, it ->
        if (index == this.size - 1) {
            str.append("$it")
        } else {
            str.append("$it$split")
        }
    }
    return str.toString()
}

/**
 * @ClassName:
 * @Description:    把string按照 "," 分解成string组合
 * @Author:         wuxianfeng
 * @CreateDate:     2019-08-26/20:20
 */
inline fun String.getOut(split:String=","): Collection<String> {
    return this.split(split)
}