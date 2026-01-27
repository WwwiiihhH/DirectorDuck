package com.example.directorduck_v10.core.network

/**
 * 统一判断接口是否成功
 * 兼容你项目里常见的 code=200，以及 apifox 示例里的 code=0
 */
fun <T> ApiResponse<T>.isOk(): Boolean {
    return this.code == 200 || this.code == 0
}
