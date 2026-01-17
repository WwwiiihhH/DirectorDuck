package com.example.directorduck_v10.feature.practice.model

import com.example.directorduck_v10.feature.practice.model.WrongReviewDetail

data class WrongReviewItem(
    val uuid: String,
    val userAnswer: String,
    val loading: Boolean = true,
    val error: String? = null,
    val detail: WrongReviewDetail? = null
)
