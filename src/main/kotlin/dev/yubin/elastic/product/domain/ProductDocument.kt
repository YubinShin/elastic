package dev.yubin.elastic.product.domain

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*

@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
data class ProductDocument(
    @Id
    val id: String,
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "products_name_analyzer"),
        otherFields = [InnerField(suffix = "auto_complete", type = FieldType.Search_As_You_Type, analyzer = "nori")]
    )
    val name: String,
    @Field(type = FieldType.Text, analyzer = "products_description_analyzer")
    val description: String,
    @Field(type = FieldType.Integer)
    val price: Int,
    @Field(type = FieldType.Double)
    val rating: Double,
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "products_category_analyzer"),
        otherFields = [InnerField(suffix = "raw", type = FieldType.Keyword)]
    )
    val category: String
) {
//    companion object {
//        fun from(product: Product): ProductDocument {
//            return ProductDocument(
//                id = product.id,
//                name = product.name,
//                description = product.description,
//                price = product.price,
//                rating = product.rating,
//                category = product.category
//            )
//        }
//    }
}
