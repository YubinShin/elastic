package dev.yubin.elastic.product.domain

import jakarta.persistence.*
import java.io.Serializable

@Entity
@Table(name = "products")
class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String,

    val price: Int,

    val rating: Double,

    val category: String
) : Serializable
