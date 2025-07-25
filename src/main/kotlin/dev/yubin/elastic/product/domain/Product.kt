package dev.yubin.elastic.product.domain

import jakarta.persistence.*
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "products")
class Product(
    @Id
    @Column(nullable = false, updatable = false, length = 36)
    val id: String = UUID.randomUUID().toString(),

    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String,

    val price: Int,

    val rating: Double,

    val category: String
) : Serializable
