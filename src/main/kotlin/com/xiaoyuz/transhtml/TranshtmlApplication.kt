package com.xiaoyuz.transhtml

import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

private val logger = KotlinLogging.logger {}

@SpringBootApplication(scanBasePackages = ["com.xiaoyuz.transhtml"])
class TranshtmlApplication

fun main(args: Array<String>) {
    SpringApplication.run(TranshtmlApplication::class.java, *args)
    val article = Article("http://www.sohu.com/a/227655720_538698")
}
