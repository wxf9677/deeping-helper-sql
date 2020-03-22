package com.diving.wsql.bean

open class QueryTotalResultDto<T: QueryDto> (val totalCount:Long, val data:List<T>)