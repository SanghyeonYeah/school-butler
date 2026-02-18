package com.schedulepartner

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing                  // createdAt / updatedAt 자동 관리
@ConfigurationPropertiesScan        // GeminiProperties 등 @ConfigurationProperties 자동 등록
class SchedulePartnerApplication

fun main(args: Array<String>) {
    runApplication<SchedulePartnerApplication>(*args)
}