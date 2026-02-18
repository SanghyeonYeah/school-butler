package com.schedulepartner.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * application.yml 의 rate-limit 블록 바인딩
 *
 * rate-limit:
 *   requests-per-minute: 60
 */
@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    val requestsPerMinute: Int = 60,
)

@Configuration
class RateLimitConfig(
    private val props: RateLimitProperties,
) {
    /**
     * IP 별로 Bucket 을 캐싱하는 Map
     * - 실서비스에서는 Redis 기반 분산 Bucket 으로 교체 권장
     */
    @Bean
    fun ipBucketCache(): ConcurrentHashMap<String, Bucket> = ConcurrentHashMap()

    /** IP 당 분당 N 요청 허용 버킷 생성 */
    fun newBucket(): Bucket {
        val limit = Bandwidth.classic(
            props.requestsPerMinute.toLong(),
            Refill.intervally(props.requestsPerMinute.toLong(), Duration.ofMinutes(1)),
        )
        return Bucket.builder().addLimit(limit).build()
    }
}


@Configuration
class RateLimitFilter(
private val rateLimitConfig: RateLimitConfig,
private val ipBucketCache: ConcurrentHashMap<String, Bucket>,
) : OncePerRequestFilter() {

override fun doFilterInternal(
request: HttpServletRequest,
response: HttpServletResponse,
filterChain: FilterChain,
) {
val ip = resolveClientIp(request)
val bucket = ipBucketCache.computeIfAbsent(ip) { rateLimitConfig.newBucket() }

if (bucket.tryConsume(1)) {
filterChain.doFilter(request, response)
} else {
response.status = HttpStatus.TOO_MANY_REQUESTS.value()
response.contentType = "application/json"
response.writer.write(
"""{"code":"COMMON-429","message":"요청이 너무 많습니다. 잠시 후 다시 시도해주세요."}"""
)
}
}

override fun shouldNotFilter(request: HttpServletRequest): Boolean =
!request.requestURI.startsWith("/api/")

private fun resolveClientIp(request: HttpServletRequest): String =
request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
?: request.remoteAddr
}