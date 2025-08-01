package com.example.directorduck_v10.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comment(
    val username: String,
    val content: String
) : Parcelable