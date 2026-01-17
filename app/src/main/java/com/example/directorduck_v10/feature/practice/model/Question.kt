package com.example.directorduck_v10.feature.practice.model

import java.io.Serializable

data class Question(
    val id: Long,
    val uuid: String,
    val questionText: String,
    val questionImage: String?, // 图片URL，可能为空
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String
) : Serializable