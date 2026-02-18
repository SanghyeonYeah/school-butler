import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"   // open class 자동 처리
    kotlin("plugin.jpa") version "1.9.24"      // no-arg constructor 자동 생성
}

group = "com.schedulepartner"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

// JPA Entity no-arg constructor 자동 생성 대상
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

repositories {
    mavenCentral()
}

dependencies {
    // ── Spring Boot ──────────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── WebFlux (WebClient for Gemini API) ───────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.netty:reactor-netty-http")

    // ── Kotlin ───────────────────────────────────────────────────────────────
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")  // Coroutine + Reactor 브릿지

    // ── JWT (JJWT) ───────────────────────────────────────────────────────────
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // ── Rate Limiting (Bucket4j) ─────────────────────────────────────────────
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // ── Configuration Properties ─────────────────────────────────────────────
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // ── Database ─────────────────────────────────────────────────────────────
    runtimeOnly("com.h2database:h2")                       // dev
    runtimeOnly("com.mysql:mysql-connector-j")             // prod

    // ── Test ─────────────────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"   // null-safety 엄격 적용
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}