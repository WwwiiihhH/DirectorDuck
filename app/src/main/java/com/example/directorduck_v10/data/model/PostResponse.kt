package com.example.directorduck_v10.data.model

import com.google.gson.annotations.SerializedName

// 保留原有的PostResponse类，用于兼容旧接口
data class PostResponse(
    val id: Long,
    val content: String,
    val imageUrl: String?,
    @SerializedName("publisherId")
    val publisherId: Long,
    @SerializedName("publisherUsername")
    val publisherUsername: String,
    val createdAt: String
) {
    // 转换为UI显示用的Post对象（不含点赞信息）
    fun toDisplayPost(): Post {
        return Post(
            id = this.id,
            username = this.publisherUsername,
            time = formatTime(this.createdAt),
            content = this.content,
            imageUrl = this.imageUrl,
            comments = emptyList(),
            likeCount = 0,  // 默认点赞数为0
            isLiked = false  // 默认未点赞
        )
    }

    private fun formatTime(createdAt: String): String {
        return try {
            // 格式化时间显示，例如：2024-01-15T10:30:00 -> 2024/01/15 10:30
            val dateTime = createdAt.replace("T", " ").substringBefore(".")
            dateTime.substring(0, 16) // 只显示到分钟
        } catch (e: Exception) {
            createdAt
        }
    }
}