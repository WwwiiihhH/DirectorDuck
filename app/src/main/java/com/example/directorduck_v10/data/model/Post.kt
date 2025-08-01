package com.example.directorduck_v10.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Post(
    val username: String,
    val time: String,
    val content: String,
    val imageResId: Int?,
    val comments: List<Comment> = emptyList()
) : Parcelable
