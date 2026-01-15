package com.example.directorduck_v10.data.model

data class WrongReviewItem(
    val uuid: String,
    val userAnswer: String,
    val loading: Boolean = true,
    val error: String? = null,
    val detail: WrongReviewDetail? = null
)
