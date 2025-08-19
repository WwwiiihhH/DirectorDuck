package com.example.directorduck_v10.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// 显示用的Post模型
@Parcelize
data class Post(
    val id: Long,
    val username: String,
    val time: String,
    val content: String,
    val imageUrl: String?,
    val comments: List<Comment> = emptyList(),
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    val commentCount: Long = 0 // 添加评论数量字段
): Parcelable

// 后端返回的PostDTO模型
data class PostDTO(
    val id: Long,
    val content: String,
    val imageUrl: String?,
    val publisherId: Long,
    val publisherUsername: String,
    val createdAt: String,
    val likeCount: Long,
    val isLiked: Boolean,
    val comments: List<Comment> = emptyList(), // 添加评论列表字段
    val commentCount: Long? = 0 // 添加评论数量字段
) {
    // 转换为UI显示用的Post对象
    fun toDisplayPost(): Post {
        return Post(
            id = this.id,
            username = this.publisherUsername,
            time = formatTime(this.createdAt),
            content = this.content,
            imageUrl = this.imageUrl,
            comments = this.comments, // 传递评论列表
            likeCount = this.likeCount,
            isLiked = this.isLiked,
            commentCount = this.commentCount ?: 0 // 传递评论数量
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