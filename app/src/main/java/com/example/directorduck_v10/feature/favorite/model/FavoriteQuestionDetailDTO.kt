package com.example.directorduck_v10.feature.favorite.model

data class FavoriteQuestionDetailDTO(
    val id: Long?,
    val uuid: String?,
    val questionText: String?,
    val questionImage: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,
    val analysis: String?,
    val difficultyLevel: Byte?,
    val status: Byte?,
    val createdTime: String?,
    val updatedTime: String?,
    val categoryName: String?,
    val favoritedAt: String?
)