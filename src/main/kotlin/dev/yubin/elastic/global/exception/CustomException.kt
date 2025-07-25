package dev.yubin.elastic.global.exception

open class CustomException(
    val code: String,
    override val message: String
) : RuntimeException(message)