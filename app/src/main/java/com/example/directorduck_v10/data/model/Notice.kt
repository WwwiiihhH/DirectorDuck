package com.example.directorduck_v10.data.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Notice(
    val id: Int,
    val title: String,
    val category: String,
    val publishTime: String,
    val recruitCount: Int,
    val positionCount: Int,
    val applyTime: String,
    val paymentTime: String,
    val admitCardTime: String,
    val examTime: String,
    val content: String,
    val attachmentUrl: String?,
    val imageUrl: String?
): Parcelable