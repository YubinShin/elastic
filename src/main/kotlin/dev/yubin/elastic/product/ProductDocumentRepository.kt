package dev.yubin.elastic.product

import dev.yubin.elastic.product.domain.ProductDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository


interface ProductDocumentRepository : ElasticsearchRepository<ProductDocument, String>
