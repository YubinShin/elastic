package dev.yubin.elastic.product

import dev.yubin.elastic.product.domain.Product
import dev.yubin.elastic.product.dto.CreateProductRequestDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("products")
class ProductController(
    private val productService: ProductService
) {

    @GetMapping
    fun getProducts(
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<MutableList<Product?>> {
        val products = productService.getProducts(page, size)
        return ResponseEntity.ok(products)
    }

    @PostMapping
    fun createProduct(@RequestBody dto: CreateProductRequestDto): ResponseEntity<Product> {
        val product = productService.createProduct(dto)
        return ResponseEntity.ok(product)
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: Long): ResponseEntity<Void> {
        productService.deleteProduct(id)
        return ResponseEntity.noContent().build()
    }
}
