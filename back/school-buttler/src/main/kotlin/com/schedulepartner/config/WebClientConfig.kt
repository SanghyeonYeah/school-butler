package com.schedulepartner.config

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class WebClientConfig(
    private val geminiProperties: GeminiProperties,
) {
    /**
     * Gemini API 전용 WebClient
     * - 타임아웃: GeminiProperties.timeoutSeconds 기준
     * - Content-Type: application/json 고정
     * - 요청/응답 로깅 필터 포함 (dev 환경 디버깅용)
     */
    @Bean
    fun geminiWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (geminiProperties.timeoutSeconds * 1000).toInt())
            .responseTimeout(Duration.ofSeconds(geminiProperties.timeoutSeconds))
            .doOnConnected { conn ->
                conn.addHandlerLast(
                    ReadTimeoutHandler(geminiProperties.timeoutSeconds, TimeUnit.SECONDS)
                )
                conn.addHandlerLast(
                    WriteTimeoutHandler(geminiProperties.timeoutSeconds, TimeUnit.SECONDS)
                )
            }

        return WebClient.builder()
            .baseUrl(geminiProperties.baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .filter(loggingRequestFilter())
            .filter(loggingResponseFilter())
            .build()
    }

    /**
     * 범용 WebClient (외부 API 추가 연동 시 사용)
     */
    @Bean
    fun defaultWebClient(): WebClient =
        WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

    // ── 로깅 필터 ────────────────────────────────────────────────────────────

    private fun loggingRequestFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { request ->
            // API Key 는 URL 파라미터로 전달되므로 로그에서 마스킹
            val safeUrl = request.url().toString()
                .replace(Regex("key=[^&]+"), "key=****")
            println("[GeminiWebClient] → ${request.method()} $safeUrl")
            Mono.just(request)
        }

    private fun loggingResponseFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofResponseProcessor { response ->
            println("[GeminiWebClient] ← Status: ${response.statusCode()}")
            Mono.just(response)
        }
}