package com.example.directorduck_v10.feature.favorite.model

data class FavoritePageDTO<T>(
    val total: Long,
    val page: Int,
    val size: Int,
    val list: List<T>?
)