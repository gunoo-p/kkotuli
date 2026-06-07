package com.wordchain

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class WordChainApplication

fun main(args: Array<String>) {
    runApplication<WordChainApplication>(*args)
}
// Virtual Thread는 application.yml의 spring.threads.virtual.enabled=true 로 활성화
// (Spring Boot 3.2+ + Java 21)
