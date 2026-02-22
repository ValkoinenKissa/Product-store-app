package com.example.product_store_app.model

import java.io.Serializable
// Data class de Kotlin
data class Product(
    val id: Int,
    val title: String,
    val price: Double,
    val thumbnail: String,
    val category: String
) : Serializable