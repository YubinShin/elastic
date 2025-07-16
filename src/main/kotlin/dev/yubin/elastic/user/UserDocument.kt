package dev.yubin.elastic.user

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

@Document(indexName = "users") // users 인덱스의 Document임을 명시
data class UserDocument(

    @Id
    var id: String? = null, // Elasticsearch에서는 Document ID는 String으로

    @Field(type = FieldType.Keyword)
    val name: String,

    @Field(type = FieldType.Long)
    val age: Long,

    @Field(type = FieldType.Boolean)
    val isActive: Boolean
)
