package com.example.directorduck_v10.data.model

data class FavoritePageDTO<T>(
    val total: Long,
    val page: Int,
    val size: Int,
    val list: List<T>?
)