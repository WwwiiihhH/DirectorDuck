package com.example.directorduck_v10.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//@Parcelize
//data class Comment(
//    val username: String,
//    val content: String
//) : Parcelable



@Parcelize
data class Comment(
    val id: Long,
    val content: String,
    val postId: Long,
    val userId: Long,
    val username: String,
    val createdAt: String
) : Parcelable {
    fun formatTime(): String {
        return try {
            // 格式化时间显示，例如：2024-01-15T10:30:00 -> 2024/01/15 10:30
            val dateTime = createdAt.replace("T", " ").substringBefore(".")
            dateTime.substring(0, 16) // 只显示到分钟
        } catch (e: Exception) {
            createdAt
        }
    }
}