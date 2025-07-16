package dev.yubin.elastic.product

import dev.yubin.elastic.product.domain.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product?, Long?>
