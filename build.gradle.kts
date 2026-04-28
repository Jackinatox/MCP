plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.scyed"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-web:4.1.0-RC1")
    implementation("org.springframework.boot:spring-boot-starter-validation:4.1.0-RC1")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // Source: https://mvnrepository.com/artifact/com.github.docker-java/docker-java-core
    implementation("com.github.docker-java:docker-java-core:3.7.1")
    // Source: https://mvnrepository.com/artifact/com.github.docker-java/docker-java-transport-httpclient5
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.7.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("com.h2database:h2")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
