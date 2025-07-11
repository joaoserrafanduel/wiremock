plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.application"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation ("org.jetbrains.kotlin:kotlin-stdlib")

    // Simple HTTP client
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON Serialization (add this)
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation ("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")

    // WireMock
    testImplementation ("com.github.tomakehurst:wiremock-jre8:2.35.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}