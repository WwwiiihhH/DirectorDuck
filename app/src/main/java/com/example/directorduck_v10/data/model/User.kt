package com.example.directorduck_v10.data.model


import java.io.Serializable

data class User(
    val id: Long,
    val username: String,
    val phone: String,
    val email: String
) : Serializable
